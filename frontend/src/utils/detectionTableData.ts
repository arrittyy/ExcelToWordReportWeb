/**
 * 检测数据 tableData：perContentRow 与检测内容行 1:1；顶层 rows 为各块合并（兼容旧读法）。
 */

import * as XLSX from 'xlsx';

const TABLE_ROW_META_KEYS = new Set(['_rid', 'key']);

/** 按 sheet 列范围将表头右侧补齐，避免 xlsx 稀疏行缺尾导致列错位 */
export function padExcelHeaderRowFromSheet(
  headerRow: string[],
  sheet: XLSX.WorkSheet,
): string[] {
  const ref = sheet['!ref'];
  if (!ref) return headerRow;
  const range = XLSX.utils.decode_range(ref);
  const colCount = range.e.c - range.s.c + 1;
  const out = [...headerRow];
  while (out.length < colCount) out.push('');
  return out;
}

/** 已知业务列键时：该行每个键均为「/」则视为末尾占位行 */
export function isSlashPlaceholderRowForKeys(row: Record<string, unknown>, businessKeys: string[]): boolean {
  if (!businessKeys.length) return false;
  return businessKeys.every((k) => {
    const v = row[k];
    const s = v === null || v === undefined ? '' : String(v).trim();
    return s === '/';
  });
}

export function stripTrailingSlashPlaceholderRows<T extends Record<string, unknown>>(
  rows: T[],
  businessKeys: string[],
): T[] {
  const out = [...rows];
  while (
    out.length > 0 &&
    isSlashPlaceholderRowForKeys(out[out.length - 1] as Record<string, unknown>, businessKeys)
  ) {
    out.pop();
  }
  return out;
}

export function buildSlashPlaceholderRow(businessKeys: string[]): Record<string, string> {
  const r: Record<string, string> = {};
  businessKeys.forEach((k) => {
    r[k] = '/';
  });
  return r;
}

/** 数据行：空与「/」均规范为 ''；整行占位行保持全「/」 */
export function normalizeDetectionDataCells(
  row: Record<string, unknown>,
  businessKeys: string[],
): Record<string, string> {
  if (isSlashPlaceholderRowForKeys(row, businessKeys)) {
    return buildSlashPlaceholderRow(businessKeys);
  }
  const out: Record<string, string> = {};
  businessKeys.forEach((k) => {
    const v = row[k];
    const s = v === null || v === undefined ? '' : String(v).trim();
    out[k] = s === '' || s === '/' ? '' : String(v);
  });
  return out;
}

/**
 * 与后端 TableDataMergeUtil.isTrailingSlashPlaceholderRow 对齐：
 * 除元数据字段外，每个字段均为字符串「/」。
 */
export function isDetectionTableTrailingSlashPlaceholderRow(
  row: Record<string, unknown> | null | undefined,
): boolean {
  if (!row || typeof row !== 'object') return false;
  const keys = Object.keys(row).filter((k) => !TABLE_ROW_META_KEYS.has(k));
  if (keys.length === 0) return false;
  return keys.every((k) => {
    const v = row[k];
    if (v === null || v === undefined) return false;
    return typeof v === 'string' && v.trim() === '/';
  });
}

export type DetectionDataBlock = { rows: Record<string, unknown>[] };

export interface ParsedTableDataShape {
  rows?: Record<string, unknown>[];
  perContentRow?: DetectionDataBlock[];
}

/** 从已解析的 tableData 对象取出分块列表；无 perContentRow 时视为整块在第 0 条检测内容下 */
export function extractPerContentRowBlocks(parsed: ParsedTableDataShape | null | undefined): DetectionDataBlock[] {
  if (!parsed || typeof parsed !== 'object') {
    return [{ rows: [] }];
  }
  const pr = parsed.perContentRow;
  if (Array.isArray(pr) && pr.length > 0) {
    return pr.map((b) => ({
      rows: Array.isArray(b?.rows) ? b.rows.map((r) => ({ ...r })) : [],
    }));
  }
  const legacy = Array.isArray(parsed.rows) ? parsed.rows.map((r) => ({ ...r })) : [];
  return [{ rows: legacy }];
}

/** 合并各分块 rows（顺序：按分块下标） */
export function flattenPerContentRowRows(blocks: DetectionDataBlock[]): Record<string, unknown>[] {
  const out: Record<string, unknown>[] = [];
  for (const b of blocks) {
    if (Array.isArray(b?.rows)) {
      for (const r of b.rows) {
        out.push({ ...r });
      }
    }
  }
  return out;
}

/** 与检测内容 table 行数对齐：多退少补空块 */
export function alignPerContentRowToContentRowCount(
  blocks: DetectionDataBlock[],
  contentRowCount: number,
): DetectionDataBlock[] {
  const n = Math.max(0, contentRowCount);
  const copy = blocks.map((b) => ({ rows: Array.isArray(b?.rows) ? [...b.rows] : [] }));
  while (copy.length < n) {
    copy.push({ rows: [] });
  }
  if (copy.length > n) {
    copy.length = n;
  }
  return copy;
}

/** 写入 DB 的 tableData 对象：含 perContentRow 与合并后的 rows */
export function buildTableDataPayload(blocks: DetectionDataBlock[]): ParsedTableDataShape {
  const aligned = blocks.map((b) => ({ rows: Array.isArray(b?.rows) ? [...b.rows] : [] }));
  return {
    perContentRow: aligned,
    rows: flattenPerContentRowRows(aligned),
  };
}

/** 与检测内容行数对齐并重建 payload，避免 perContentRow 与顶层 rows 不一致导致保存后重复行 */
export function canonicalizeTableDataPayload(
  detectionData: ParsedTableDataShape | null | undefined,
  contentRowCount: number,
): ParsedTableDataShape {
  const n = Math.max(1, contentRowCount);
  const blocks = alignPerContentRowToContentRowCount(extractPerContentRowBlocks(detectionData), n);
  return buildTableDataPayload(blocks);
}

/** 更新某一分块并保持合并 rows */
export function setBlockRows(
  blocks: DetectionDataBlock[],
  contentRowIndex: number,
  rows: Record<string, unknown>[],
): DetectionDataBlock[] {
  const next = alignPerContentRowToContentRowCount(blocks, Math.max(blocks.length, contentRowIndex + 1));
  if (contentRowIndex >= 0 && contentRowIndex < next.length) {
    next[contentRowIndex] = { rows: rows != null ? [...rows] : [] };
  }
  return next;
}

/**
 * 同步将某检测内容行对应分块写入 tableData（不依赖 setState）。
 * contentRowCount 应与 detectionContent.rows.length 一致，避免 legacy 单块扩行时丢块。
 */
/** 是否存在非空、非末尾「/」占位行的检测数据 */
export function hasNonEmptyDetectionBlocks(
  detectionData: ParsedTableDataShape | null | undefined,
): boolean {
  const blocks = extractPerContentRowBlocks(detectionData);
  return blocks.some((b) =>
    (b.rows ?? []).some((r) => {
      if (isDetectionTableTrailingSlashPlaceholderRow(r)) return false;
      return Object.keys(r).some((k) => {
        if (TABLE_ROW_META_KEYS.has(k)) return false;
        const v = r[k];
        const s = v === null || v === undefined ? '' : String(v).trim();
        return s !== '' && s !== '/';
      });
    }),
  );
}

export function mergeBlockIntoTableData(
  detectionData: ParsedTableDataShape | null | undefined,
  contentRowIndex: number,
  rows: Record<string, unknown>[],
  contentRowCount?: number,
): ParsedTableDataShape {
  const blocks = extractPerContentRowBlocks(detectionData);
  const n = Math.max(
    1,
    contentRowCount ?? Math.max(blocks.length, contentRowIndex + 1),
  );
  const aligned = alignPerContentRowToContentRowCount(blocks, n);
  const nextBlocks = setBlockRows(aligned, contentRowIndex, rows);
  return buildTableDataPayload(nextBlocks);
}

export type SchemaColumnLike = { key: string; label: string };

/** Excel 导出：表头与列顺序与 schema.columns / 网格一致（aoa 避免 json_to_sheet 乱序） */
export function buildDetectionSheetAoa(
  columns: SchemaColumnLike[],
  dataRows: Record<string, unknown>[],
): (string | number)[][] {
  const headers = columns.map((c) => c.label);
  const aoa: (string | number)[][] = [headers];
  for (const row of dataRows) {
    aoa.push(
      columns.map((c) => {
        const v = row[c.key];
        if (v === null || v === undefined) return '';
        return String(v);
      }),
    );
  }
  return aoa;
}
