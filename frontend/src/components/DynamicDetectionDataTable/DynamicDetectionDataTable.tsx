import React, { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { Button, Checkbox, Select, Space, message, Popconfirm } from 'antd';
import { PlusOutlined, DeleteOutlined, SaveOutlined, DownloadOutlined, UploadOutlined } from '@ant-design/icons';
import * as XLSX from 'xlsx';
import DataGrid, {
  SelectColumn,
  SELECT_COLUMN_KEY,
  textEditor,
} from 'react-data-grid';
import type { Column, RenderEditCellProps, RenderCellProps, RenderHeaderCellProps } from 'react-data-grid';
import { TableSchema, TableData, TableRowData } from '../../types';
import {
  buildSlashPlaceholderRow,
  buildDetectionSheetAoa,
  normalizeDetectionDataCells,
  isSlashPlaceholderRowForKeys,
  stripTrailingSlashPlaceholderRows,
  padExcelHeaderRowFromSheet,
} from '../../utils/detectionTableData';
import './DynamicDetectionDataGrid.css';

/** 依赖：react-data-grid（MIT），用于 Excel 式录入与粘贴扩行 */

/** 里氏分类由保存弹窗写入，仍保存在行 JSON；网格、粘贴列序、Excel 模板中不展示 */
const SCHEMA_COLUMN_HIDDEN_IN_GRID = '里氏分类';

interface DynamicDetectionDataTableProps {
  tableSchema: string;
  value?: TableData;
  /** 多部件 perContentRow 时仅最后一个检测内容块末尾追加全「/」占位行 */
  appendTrailingSlashPlaceholderRow?: boolean;
  onChange?: (value: TableData) => void;
  onSave?: (data: TableData) => void;
  disabled?: boolean;
  experimentTypeName?: string;
  nonComplianceRecords?: Array<{
    number?: string;
    itemName?: string;
    standardValue?: string;
    actualValue?: string;
    result?: string;
  }>;
}

const ALLOY_ELEMENTS = ['Mn', 'Cr', 'Mo', 'V', 'Ti', 'Ni', 'Al', 'Cu', 'Nb', 'W', 'Co', 'Mg', 'Zr'];

/** 从行对象键名推断合金元素列（含值为 / 或空的占位列），避免外链路过滤空行后误清空已选元素 */
function extractAlloyElementKeysFromRows(rows: TableRowData[] | undefined | null): string[] {
  if (!rows?.length) return [];
  const used = new Set<string>();
  for (const row of rows) {
    for (const key of Object.keys(row)) {
      if (key !== '编号' && key !== 'key' && ALLOY_ELEMENTS.includes(key)) {
        used.add(key);
      }
    }
  }
  return Array.from(used);
}

const LEEB_ITEM_NAME_ALIASES = new Set(['焊缝硬度', '管件硬度', '钢管硬度', '螺栓硬度', '螺帽硬度']);

const NON_COMPLIANCE_ITEM_NAME_TO_KEY: Record<string, string> = {
  实测厚度: '实测厚度',
  '实测厚度 (mm)': '实测厚度',
  '实测厚度（mm）': '实测厚度',
  实测厚度mm: '实测厚度',
  实测管径: '实测管径',
  实测管径mm: '实测管径',
};

const ACTIONS_COLUMN_KEY = '__actions__';
const RECORD_ONLY_DEFECT_KEY = '是否为记录缺陷';
const RECORD_ONLY_TYPE_NAMES = new Set(['超声检测', '渗透检测', '磁粉检测', '射线检测']);
const MAX_PASTE_ROWS = 1000;
const MIN_GRID_ROWS = 20;
const ACTIONS_COL_WIDTH = 88;
const RECORD_ONLY_COL_WIDTH = 128;
const SELECT_COL_WIDTH = 40;
const GRID_SCROLL_GUTTER = 14;
const MIN_DATA_COL_WIDTH = 80;
/** 建议 Excel 含主键列（缺失时警告，不阻断上传） */
const EXCEL_PRIMARY_KEY_KEYS = new Set(['测点编号', '编号', '序号', '弯头编号']);

function isRecordOnlyDefectChecked(raw: unknown): boolean {
  const s = String(raw ?? '').trim().toLowerCase();
  return s === '是' || s === 'true' || s === '1' || s === 'yes' || s === 'y';
}

function estimateMinWidthForLabel(label: string): number {
  return Math.max(MIN_DATA_COL_WIDTH, Math.min(220, label.length * 14 + 24));
}

type GridRow = TableRowData & { _rid: string };

function migrateRadiographicTableRow(row: TableRowData): TableRowData {
  const out: TableRowData = { ...row };
  const cellStr = (v: unknown) => (v === null || v === undefined ? '' : String(v).trim());
  const isBlank = (v: unknown) => {
    const s = cellStr(v);
    return !s || s === '/';
  };

  if (isBlank(out['底片编号'])) {
    const a = cellStr(out['底片']);
    const b = cellStr(out['编号']);
    const merged = [a, b].filter((x) => x && x !== '/').join(' ');
    if (merged) out['底片编号'] = merged;
  }

  if (isBlank(out['缺陷位置、性质及数量'])) {
    const a = cellStr(out['缺陷位置']);
    const b = cellStr(out['性质及数量']);
    const merged = [a, b].filter((x) => x && x !== '/').join(' ');
    if (merged) out['缺陷位置、性质及数量'] = merged;
  }

  if (isBlank(out['厚度 mm'])) {
    const t =
      cellStr(out['厚度']) ||
      cellStr(out['厚度（mm）']) ||
      cellStr(out['厚度(mm)']) ||
      cellStr(out['厚度mm']);
    if (t && t !== '/') out['厚度 mm'] = t;
  }

  return out;
}

function stripRid(rows: GridRow[]): TableRowData[] {
  return rows.map(({ _rid: _r, ...rest }) => rest as TableRowData);
}

function normalizeExcelHeaderLabel(text: string): string {
  return text.replace(/\s+/g, '').replace(/[()（）]/g, '').toLowerCase();
}

type ExcelColumnDef = { key: string; label: string; name?: string };

function buildExcelLabelToKey(columns: ExcelColumnDef[], isRadiographic: boolean): Record<string, string> {
  const labelToKey: Record<string, string> = {};
  columns.forEach((col) => {
    labelToKey[col.label] = col.key;
    labelToKey[col.key] = col.key;
    if (col.name) labelToKey[col.name] = col.key;
    labelToKey[normalizeExcelHeaderLabel(col.label)] = col.key;
    labelToKey[normalizeExcelHeaderLabel(col.key)] = col.key;
  });
  labelToKey['实测厚度（mm）'] = '实测厚度';
  labelToKey['实测厚度(mm)'] = '实测厚度';
  labelToKey['实测厚度 (mm)'] = '实测厚度';
  labelToKey['实测厚度mm'] = '实测厚度';
  labelToKey['实测厚度'] = '实测厚度';
  labelToKey[normalizeExcelHeaderLabel('实测厚度（mm）')] = '实测厚度';
  labelToKey['实测管径（mm）'] = '实测管径';
  labelToKey['实测管径(mm)'] = '实测管径';
  labelToKey['实测管径 (mm)'] = '实测管径';
  labelToKey['实测管径mm'] = '实测管径';
  labelToKey['实测管径'] = '实测管径';
  labelToKey[normalizeExcelHeaderLabel('实测管径（mm）')] = '实测管径';
  if (isRadiographic) {
    labelToKey['厚度（mm）'] = '厚度 mm';
    labelToKey['厚度(mm)'] = '厚度 mm';
    labelToKey['厚度mm'] = '厚度 mm';
    labelToKey['厚度'] = '厚度';
    labelToKey['底片'] = '底片';
    labelToKey['编号'] = '编号';
    labelToKey['缺陷位置'] = '缺陷位置';
    labelToKey['性质及数量'] = '性质及数量';
  }
  return labelToKey;
}

function resolveExcelHeaderKey(header: string, labelToKey: Record<string, string>): string | undefined {
  const h = header?.trim().replace(/^\uFEFF/, '');
  if (!h) return undefined;
  if (labelToKey[h]) return labelToKey[h];
  const norm = normalizeExcelHeaderLabel(h);
  return labelToKey[norm];
}

function parseExcelHeaderRow(rawHeader: (string | number)[]): string[] {
  return rawHeader.map((c, i) => {
    const s = String(c ?? '').trim();
    return i === 0 ? s.replace(/^\uFEFF/, '') : s;
  });
}

/**
 * 校验 Excel 表头：至少识别 1 列即可（允许仅为模板列的子集）。
 * missingKeys 为模板中有但 Excel 未出现的列，仅作提示，不导致失败。
 */
function validateExcelHeaders(
  headerRow: string[],
  labelToKey: Record<string, string>,
  schemaKeys: string[],
): {
  ok: boolean;
  missingKeys: string[];
  matchedKeys: string[];
  recognizedHeaders: string[];
} {
  const matchedKeys = new Set<string>();
  const recognizedHeaders: string[] = [];
  for (const h of headerRow) {
    const key = resolveExcelHeaderKey(h, labelToKey);
    if (key) {
      matchedKeys.add(key);
      if (h) recognizedHeaders.push(h);
    }
  }
  const missingKeys = schemaKeys.filter((k) => !matchedKeys.has(k));
  return {
    ok: matchedKeys.size > 0,
    missingKeys,
    matchedKeys: [...matchedKeys],
    recognizedHeaders,
  };
}

function stablePayloadFingerprint(
  rows: TableRowData[],
  businessKeys: string[],
  extraKeys: string[] = [],
): string {
  const stripped = stripTrailingSlashPlaceholderRows(rows, businessKeys);
  return JSON.stringify(
    stripped.map((r) => {
      const sorted: TableRowData = {};
      businessKeys.forEach((k) => {
        const v = r[k];
        sorted[k] = v === null || v === undefined ? '' : String(v);
      });
      extraKeys.forEach((k) => {
        const v = r[k];
        sorted[k] = v === null || v === undefined ? '' : String(v);
      });
      return sorted;
    }),
  );
}

function createSelectColumnEditor(options: string[]) {
  return function GridSelectEditor(props: RenderEditCellProps<GridRow>) {
    const k = props.column.key as string;
    const cur = props.row[k];
    return (
      <Select
        autoFocus
        allowClear
        style={{ width: '100%' }}
        placeholder="请选择"
        value={cur === '' || cur === '/' ? undefined : String(cur)}
        options={options.map((o) => ({ label: o, value: o }))}
        onChange={(val) => {
          props.onRowChange({ ...props.row, [k]: val ?? '' }, true);
        }}
      />
    );
  };
}

const DynamicDetectionDataTable: React.FC<DynamicDetectionDataTableProps> = ({
  tableSchema,
  value,
  appendTrailingSlashPlaceholderRow = true,
  onChange,
  onSave,
  disabled = false,
  experimentTypeName = '检测数据',
  nonComplianceRecords = [],
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const gridWrapRef = useRef<HTMLDivElement>(null);
  const nextRowIdRef = useRef(1);
  const dataColumnKeysRef = useRef<string[]>([]);
  const selectedAnchorRef = useRef<{ rowIdx: number; colKey: string } | null>(null);
  const emitRafRef = useRef<number | null>(null);

  const [schema, setSchema] = useState<TableSchema | null>(null);
  const [gridRows, setGridRows] = useState<GridRow[]>([]);
  const [selectedRows, setSelectedRows] = useState<ReadonlySet<string>>(new Set());
  const [gridWidth, setGridWidth] = useState(0);

  const isRadiographicInspection = experimentTypeName === '射线检测';
  const supportsRecordOnlyDefect = RECORD_ONLY_TYPE_NAMES.has(experimentTypeName);

  const visibleSchemaColumns = useMemo(() => {
    if (!schema) return [];
    return schema.columns.filter((c) => c.key !== SCHEMA_COLUMN_HIDDEN_IN_GRID);
  }, [schema]);

  const isAlloyAnalysis = useMemo(() => {
    if (!schema) return false;
    return schema.columns.length === 1 && schema.columns[0].key === '编号';
  }, [schema]);

  const [selectedElements, setSelectedElements] = useState<string[]>(() =>
    extractAlloyElementKeysFromRows(value?.rows),
  );

  const businessKeysForPayload = useMemo((): string[] => {
    if (isAlloyAnalysis) return ['编号', ...selectedElements];
    if (!schema) return [];
    return schema.columns.map((c) => c.key);
  }, [isAlloyAnalysis, schema, selectedElements]);

  const newRowId = useCallback(() => `gr_${nextRowIdRef.current++}`, []);

  const createEmptyRow = useCallback(
    (alloyElementsOverride?: string[]): GridRow => {
      const row: TableRowData = {};
      if (isAlloyAnalysis) {
        const els = alloyElementsOverride ?? selectedElements;
        row['编号'] = '';
        els.forEach((el) => {
          row[el] = '';
        });
      } else if (schema) {
        schema.columns.forEach((col) => {
          row[col.key] = '';
        });
      }
      return { ...row, _rid: newRowId() };
    },
    [isAlloyAnalysis, schema, selectedElements, newRowId],
  );

  const padToMinRows = useCallback(
    (rows: GridRow[], alloyElementsOverride?: string[]) => {
      const next = [...rows];
      while (next.length < MIN_GRID_ROWS) {
        next.push(createEmptyRow(alloyElementsOverride));
      }
      return next;
    },
    [createEmptyRow],
  );

  useLayoutEffect(() => {
    const el = gridWrapRef.current;
    if (!el) return;
    const measure = () => setGridWidth(Math.max(0, Math.floor(el.getBoundingClientRect().width)));
    measure();
    const ro = new ResizeObserver(() => measure());
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const availableFieldMeta = useMemo(() => {
    const map = new Map<string, string>();
    if (isAlloyAnalysis) {
      map.set('编号', '编号');
      selectedElements.forEach((el) => map.set(el, el));
      return map;
    }
    schema?.columns.forEach((col) => {
      map.set(col.key, col.key);
      map.set(col.label, col.key);
      if (col.name) map.set(col.name, col.key);
    });
    return map;
  }, [isAlloyAnalysis, selectedElements, schema]);

  const normalizeFieldName = (text: string) =>
    text.replace(/\s+/g, '').replace(/[()（）]/g, '').toLowerCase();

  const nonComplianceHighlightMap = useMemo(() => {
    const map = new Map<number, Map<string, string>>();
    if (!nonComplianceRecords?.length) return map;

    const fieldNameToKey = new Map<string, string>();
    availableFieldMeta.forEach((key, name) => {
      fieldNameToKey.set(name, key);
      fieldNameToKey.set(normalizeFieldName(name), key);
    });

    const getKeyByItemName = (itemName?: string) => {
      if (!itemName) return null;
      const direct =
        fieldNameToKey.get(itemName) || fieldNameToKey.get(normalizeFieldName(itemName)) || null;
      if (direct) return direct;
      const trimmed = itemName.trim();
      if (LEEB_ITEM_NAME_ALIASES.has(trimmed)) {
        return fieldNameToKey.get('平均') ?? null;
      }
      const aliasTarget =
        NON_COMPLIANCE_ITEM_NAME_TO_KEY[trimmed] ||
        NON_COMPLIANCE_ITEM_NAME_TO_KEY[normalizeFieldName(trimmed)] ||
        null;
      if (aliasTarget) {
        return (
          fieldNameToKey.get(aliasTarget) ||
          fieldNameToKey.get(normalizeFieldName(aliasTarget)) ||
          null
        );
      }
      return null;
    };

    nonComplianceRecords.forEach((record) => {
      const fieldKey = getKeyByItemName(record.itemName);
      if (!fieldKey) return;

      const number = record.number != null ? String(record.number).trim() : '';
      const targetRowIndexes: number[] = [];

      if (number && number !== '/') {
        gridRows.forEach((row, idx) => {
          const rowNo = row['编号'] != null ? String(row['编号']).trim() : '';
          const pointNo = row['测点编号'] != null ? String(row['测点编号']).trim() : '';
          if (rowNo === number || pointNo === number) targetRowIndexes.push(idx);
        });
      } else {
        for (let i = 0; i < gridRows.length; i++) targetRowIndexes.push(i);
      }

      const reasonParts = [
        record.result || '不符合标准要求',
        record.standardValue ? `标准值: ${record.standardValue}` : '',
        record.actualValue ? `实测值: ${record.actualValue}` : '',
      ].filter(Boolean);
      const reason = reasonParts.join('；');

      targetRowIndexes.forEach((rowIdx) => {
        if (!map.has(rowIdx)) map.set(rowIdx, new Map<string, string>());
        map.get(rowIdx)!.set(fieldKey, reason);
      });
    });

    return map;
  }, [nonComplianceRecords, availableFieldMeta, gridRows]);

  const getHighlightCellProps = (rowIndex: number, dataIndex: string) => {
    const rowMap = nonComplianceHighlightMap.get(rowIndex);
    const reason = rowMap?.get(dataIndex);
    if (!reason) return {};
    return {
      className: 'non-compliance-cell',
      title: reason,
      style: {
        backgroundColor: '#fff1f0',
        color: '#cf1322',
        fontWeight: 600,
      } as React.CSSProperties,
    };
  };

  useEffect(() => {
    try {
      const parsedSchema = JSON.parse(tableSchema);
      setSchema(parsedSchema);
    } catch (error) {
      console.error('Invalid TableSchema JSON:', error);
      message.error('检测数据表格配置错误');
    }
  }, [tableSchema]);

  useEffect(() => {
    if (!isAlloyAnalysis) return;
    const fromRows = extractAlloyElementKeysFromRows(value?.rows);
    // 外链路可能将「全空占位行」过滤成 rows: []，勿清空本地已选元素，否则会立刻丢掉列与多选状态
    if (fromRows.length > 0) {
      setSelectedElements(fromRows);
    }
  }, [isAlloyAnalysis, value]);

  const isRowEffectivelyEmpty = useCallback(
    (row: TableRowData): boolean => {
      if (
        businessKeysForPayload.length > 0 &&
        isSlashPlaceholderRowForKeys(row as Record<string, unknown>, businessKeysForPayload)
      ) {
        return false;
      }
      const cellEmpty = (v: unknown) => {
        if (v === null || v === undefined) return true;
        const s = String(v).trim();
        return s === '' || s === '/';
      };
      if (isAlloyAnalysis) {
        if (!cellEmpty(row['编号'])) return false;
        for (const el of selectedElements) {
          if (!cellEmpty(row[el])) return false;
        }
        return true;
      }
      if (!schema) return true;
      for (const col of schema.columns) {
        if (!cellEmpty(row[col.key])) return false;
      }
      return true;
    },
    [isAlloyAnalysis, schema, selectedElements, businessKeysForPayload],
  );

  const filterEmptyRows = useCallback(
    (rows: TableRowData[]) => rows.filter((r) => !isRowEffectivelyEmpty(r)),
    [isRowEffectivelyEmpty],
  );

  const finalizePayloadRows = useCallback(
    (data: TableRowData[]): TableRowData[] => {
      const keys = businessKeysForPayload;
      if (!keys.length) return [];
      let rows = filterEmptyRows(data);
      rows = stripTrailingSlashPlaceholderRows(rows, keys);
      let normalized = rows.map((r) => normalizeDetectionDataCells(r as Record<string, unknown>, keys) as TableRowData);
      if (supportsRecordOnlyDefect) {
        normalized = normalized.map((row, idx) => {
          const rawFlag = rows[idx]?.[RECORD_ONLY_DEFECT_KEY];
          const checked = isRecordOnlyDefectChecked(rawFlag);
          return checked ? { ...row, [RECORD_ONLY_DEFECT_KEY]: '是' } : row;
        });
      }
      if (appendTrailingSlashPlaceholderRow && normalized.length > 0) {
        normalized = [...normalized, buildSlashPlaceholderRow(keys) as TableRowData];
      }
      return normalized;
    },
    [businessKeysForPayload, filterEmptyRows, appendTrailingSlashPlaceholderRow, supportsRecordOnlyDefect],
  );

  const toPayload = useCallback(
    (rows: GridRow[]) => finalizePayloadRows(stripRid(rows)),
    [finalizePayloadRows],
  );

  /** 外部 value 变化时同步网格；若与当前编辑结果一致则保留行 _rid，避免每次 onChange 回写导致选区丢失 */
  useEffect(() => {
    const incoming: TableRowData[] = value?.rows?.length
      ? isRadiographicInspection
        ? value.rows.map((r) => migrateRadiographicTableRow({ ...r }))
        : value.rows.map((r) => ({ ...r }))
      : [];
    setGridRows((prev) => {
      const keys = businessKeysForPayload;
      const extraKeys = supportsRecordOnlyDefect ? [RECORD_ONLY_DEFECT_KEY] : [];
      const prevPayload = finalizePayloadRows(stripRid(prev));
      const incPayload = finalizePayloadRows(incoming);
      if (
        keys.length > 0 &&
        stablePayloadFingerprint(prevPayload, keys, extraKeys) ===
          stablePayloadFingerprint(incPayload, keys, extraKeys)
      ) {
        if (prev.length >= MIN_GRID_ROWS) return prev;
        return padToMinRows(prev);
      }
      setSelectedRows(new Set());
      const base = incoming.map((r) => ({ ...r, _rid: newRowId() }));
      return padToMinRows(base);
    });
  }, [
    value,
    isRadiographicInspection,
    businessKeysForPayload,
    supportsRecordOnlyDefect,
    finalizePayloadRows,
    newRowId,
    padToMinRows,
  ]);

  const emitChange = useCallback(
    (rows: GridRow[]) => {
      onChange?.({ rows: toPayload(rows) });
    },
    [toPayload, onChange],
  );

  useEffect(() => {
    return () => {
      if (emitRafRef.current != null) {
        cancelAnimationFrame(emitRafRef.current);
      }
    };
  }, []);

  const handleRowsChange = useCallback(
    (newRows: GridRow[]) => {
      setGridRows(newRows);
      emitChange(newRows);
    },
    [emitChange],
  );

  const setRecordOnlyForRow = useCallback(
    (rowIdx: number, checked: boolean) => {
      setGridRows((prev) => {
        if (rowIdx < 0 || rowIdx >= prev.length) return prev;
        const next = [...prev];
        const row = { ...next[rowIdx] };
        const hasBusinessValue = businessKeysForPayload.some((k) => {
          const v = row[k];
          if (v === null || v === undefined) return false;
          const s = String(v).trim();
          return s !== '' && s !== '/';
        });
        if (checked && !hasBusinessValue) {
          message.warning('请先录入该行缺陷数据，再勾选“是否为记录缺陷”');
          return prev;
        }
        row[RECORD_ONLY_DEFECT_KEY] = checked ? '是' : '';
        next[rowIdx] = row;
        // 使用 rAF 让本地勾选先完成绘制，再回传父级，避免点击时卡顿。
        if (emitRafRef.current != null) {
          cancelAnimationFrame(emitRafRef.current);
        }
        emitRafRef.current = requestAnimationFrame(() => {
          emitRafRef.current = null;
          emitChange(next);
        });
        return next;
      });
    },
    [emitChange, businessKeysForPayload],
  );

  const dataColumnKeys = useMemo(() => {
    if (isAlloyAnalysis) return ['编号', ...selectedElements];
    return visibleSchemaColumns.map((c) => c.key);
  }, [isAlloyAnalysis, visibleSchemaColumns, selectedElements]);

  useEffect(() => {
    dataColumnKeysRef.current = dataColumnKeys;
  }, [dataColumnKeys]);

  const handleElementSelectionChange = (elements: string[]) => {
    setSelectedElements(elements);
    setGridRows((prev) => {
      const newData =
        prev.length === 0
          ? []
          : prev.map((row) => {
              const newRow: GridRow = { ...row };
              Object.keys(newRow).forEach((key) => {
                if (key !== '_rid' && key !== '编号' && key !== 'key' && !elements.includes(key)) {
                  delete newRow[key];
                }
              });
              elements.forEach((el) => {
                if (el !== '编号' && (newRow[el] === undefined || newRow[el] === null)) {
                  newRow[el] = '';
                }
              });
              return newRow;
            });
      const padded = padToMinRows(newData, elements);
      queueMicrotask(() => emitChange(padded));
      return padded;
    });
  };

  const handlePaste = useCallback(
    (e: React.ClipboardEvent<HTMLDivElement>) => {
      if (disabled) return;
      const text = e.clipboardData.getData('text/plain');
      if (!text) return;
      const hasTab = text.includes('\t');
      const hasNl = /\r|\n/.test(text);
      if (!hasTab && !hasNl) return;

      e.preventDefault();
      e.stopPropagation();

      const lines = text.replace(/\r\n/g, '\n').replace(/\r/g, '\n').split('\n');
      while (lines.length && lines[lines.length - 1] === '') lines.pop();
      const matrix = lines.map((line) => line.split('\t'));
      if (matrix.length === 0) return;

      const keys = dataColumnKeysRef.current;
      if (keys.length === 0) return;

      const anchor = selectedAnchorRef.current;
      const startRow = anchor?.rowIdx ?? 0;
      let startCol = 0;
      if (anchor?.colKey && keys.includes(anchor.colKey)) {
        startCol = keys.indexOf(anchor.colKey);
      }

      setGridRows((prev) => {
        let next = [...prev];
        const needed = startRow + matrix.length;
        if (needed > MAX_PASTE_ROWS) {
          message.warning(`粘贴后总行数不能超过 ${MAX_PASTE_ROWS} 行，已截断`);
        }
        const cappedNeeded = Math.min(needed, MAX_PASTE_ROWS);
        while (next.length < cappedNeeded) {
          next.push(createEmptyRow());
        }
        const rowCount = Math.min(matrix.length, Math.max(0, cappedNeeded - startRow));
        for (let r = 0; r < rowCount; r++) {
          const cells = matrix[r];
          let row: GridRow = { ...next[startRow + r] };
          for (let c = 0; c < cells.length && startCol + c < keys.length; c++) {
            const key = keys[startCol + c];
            row = { ...row, [key]: String(cells[c] ?? '').trim() };
          }
          if (isRadiographicInspection) {
            row = { ...row, ...migrateRadiographicTableRow(row) } as GridRow;
          }
          next[startRow + r] = row;
        }
        const padded = padToMinRows(next);
        Promise.resolve().then(() => emitChange(padded));
        return padded;
      });
    },
    [disabled, createEmptyRow, emitChange, isRadiographicInspection, padToMinRows],
  );

  const handleFill = useCallback(({ columnKey, sourceRow, targetRow }: { columnKey: string; sourceRow: GridRow; targetRow: GridRow }) => {
    if (disabled || columnKey === SELECT_COLUMN_KEY || columnKey === ACTIONS_COLUMN_KEY) {
      return targetRow;
    }
    return { ...targetRow, [columnKey]: sourceRow[columnKey as keyof GridRow] };
  }, [disabled]);

  const handleSaveAll = () => {
    if (onSave) onSave({ rows: toPayload(gridRows) });
  };

  const writeWorkbookToFile = useCallback(
    (aoa: (string | number)[][], fileName: string) => {
      const wb = XLSX.utils.book_new();
      const ws = XLSX.utils.aoa_to_sheet(aoa);
      XLSX.utils.book_append_sheet(wb, ws, '检测数据');
      XLSX.writeFile(wb, fileName);
    },
    [],
  );

  const schemaColumnsForExcel = useMemo((): { key: string; label: string }[] => {
    if (isAlloyAnalysis) {
      return [{ key: '编号', label: '编号' }, ...selectedElements.map((el) => ({ key: el, label: el }))];
    }
    return visibleSchemaColumns.map((c) => ({ key: c.key, label: c.label }));
  }, [isAlloyAnalysis, selectedElements, visibleSchemaColumns]);

  const handleDownloadTemplate = () => {
    if (!schema && !isAlloyAnalysis) return;
    const cols = schemaColumnsForExcel;
    // 仅导出表头行，避免空数据行导致上传时被误判为「表头不一致」
    const aoa = buildDetectionSheetAoa(cols, []);
    const safeName = (experimentTypeName || '检测数据').replace(/[/\\?*\[\]:]/g, '_');
    writeWorkbookToFile(aoa, `检测数据模板_${safeName}.xlsx`);
    message.info('模板仅含表头，请在 Excel 表头下方填写数据后再上传');
  };

  const handleExportCurrentData = () => {
    if (!schema && !isAlloyAnalysis) return;
    const payloadRows = toPayload(gridRows);
    const aoa = buildDetectionSheetAoa(schemaColumnsForExcel, payloadRows as Record<string, unknown>[]);
    const safeName = (experimentTypeName || '检测数据').replace(/[/\\?*\[\]:]/g, '_');
    writeWorkbookToFile(aoa, `检测数据_${safeName}.xlsx`);
    message.success('已导出当前检测数据');
  };

  const handleUploadExcel = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !schema) return;
    e.target.value = '';
    const reader = new FileReader();
    reader.onload = (ev) => {
      try {
        const data = ev.target?.result;
        if (!data) return;
        const workbook = XLSX.read(data, { type: 'binary' });
        const firstSheetName = workbook.SheetNames[0];
        const sheet = workbook.Sheets[firstSheetName];
        const raw = XLSX.utils.sheet_to_json(sheet, { header: 1, defval: '' }) as (string | number)[][];
        if (!raw.length) {
          message.warning('Excel 为空');
          return;
        }
        let headerRow = parseExcelHeaderRow(raw[0]);
        headerRow = padExcelHeaderRowFromSheet(headerRow, sheet);
        const dataRows = raw.slice(1).filter((row) => {
          if (!row || !row.length) return false;
          return row.some((cell) => String(cell ?? '').trim() !== '');
        });

        if (isAlloyAnalysis) {
          const headerToKey: Record<string, string> = { 编号: '编号' };
          ALLOY_ELEMENTS.forEach((el) => {
            headerToKey[el] = el;
          });
          const uploadedElementKeys = new Set<string>();
          const parsedRows: TableRowData[] = [];
          for (const row of dataRows) {
            const obj: TableRowData = {};
            let hasAny = false;
            headerRow.forEach((h, i) => {
              const key = headerToKey[h];
              if (!key) return;
              const val = row[i];
              const v = val === null || val === undefined ? '' : String(val).trim();
              if (v !== '') hasAny = true;
              if (key === '编号') {
                obj[key] = v;
              } else if (ALLOY_ELEMENTS.includes(key)) {
                obj[key] = v;
                if (v) uploadedElementKeys.add(key);
              }
            });
            if (hasAny) parsedRows.push(obj);
          }
          const newElements = Array.from(new Set([...selectedElements, ...uploadedElementKeys]));
          setSelectedElements(newElements);
          const keysAtUpload = ['编号', ...newElements];
          const prior = stripTrailingSlashPlaceholderRows([...(value?.rows || [])], keysAtUpload);
          const merged = [...prior, ...parsedRows];
          const normalized = merged.map((row) =>
            normalizeDetectionDataCells(row as Record<string, unknown>, keysAtUpload),
          );
          const gr = padToMinRows(
            normalized.map((r) => ({ ...(r as TableRowData), _rid: newRowId() })),
            newElements,
          );
          setGridRows(gr);
          emitChange(gr);
          if (onSave) onSave({ rows: toPayload(gr) });
          message.success(`已追加 ${parsedRows.length} 行检测数据`);
        } else {
          const excelCols = schemaColumnsForExcel;
          const labelToKey = buildExcelLabelToKey(excelCols, isRadiographicInspection);
          const keysUpload = excelCols.map((c) => c.key);
          const headerCheck = validateExcelHeaders(headerRow, labelToKey, keysUpload);
          if (!headerCheck.ok) {
            const templateLabels = excelCols.map((c) => c.label).join('、');
            const actual = headerRow.filter((h) => h).join('、') || '（未识别到表头）';
            message.warning(
              `未能识别任何模板列。模板列：${templateLabels}；当前表头：${actual}`,
            );
            return;
          }
          if (dataRows.length === 0) {
            message.warning(
              `已识别表头，但未找到数据行。请在已识别的列下方填写检测数据后再上传`,
            );
            return;
          }
          const schemaPrimaryKeys = keysUpload.filter((k) => EXCEL_PRIMARY_KEY_KEYS.has(k));
          if (
            schemaPrimaryKeys.length > 0 &&
            !schemaPrimaryKeys.some((k) => headerCheck.matchedKeys.includes(k))
          ) {
            message.warning(
              `建议 Excel 包含「${schemaPrimaryKeys.join('、')}」列，便于与检测点对应；未包含的列将留空`,
            );
          }
          const parsedRows: TableRowData[] = [];
          for (const row of dataRows) {
            const obj: TableRowData = {};
            let hasAny = false;
            headerRow.forEach((h, i) => {
              const key = resolveExcelHeaderKey(h, labelToKey);
              if (!key) return;
              const val = row[i];
              const v = val === null || val === undefined ? '' : String(val).trim();
              if (v !== '') hasAny = true;
              obj[key] = v;
            });
            const finalized = isRadiographicInspection ? migrateRadiographicTableRow(obj) : obj;
            if (hasAny) parsedRows.push(finalized);
          }
          if (parsedRows.length === 0) {
            message.warning('未识别到有效数据，请确认各行至少填写测点编号或实测厚度等字段');
            return;
          }
          const prior = stripTrailingSlashPlaceholderRows(
            (value?.rows || []).map((r) =>
              isRadiographicInspection ? migrateRadiographicTableRow({ ...r }) : { ...r },
            ),
            keysUpload,
          );
          const merged = [...prior, ...parsedRows];
          const normalized = merged.map((row) =>
            normalizeDetectionDataCells(row as Record<string, unknown>, keysUpload),
          );
          const gr = padToMinRows(
            normalized.map((r) => ({ ...(r as TableRowData), _rid: newRowId() })),
          );
          setGridRows(gr);
          emitChange(gr);
          const payload = toPayload(gr);
          if (onSave) {
            queueMicrotask(() => onSave({ rows: payload }));
          }
          const colHint =
            headerCheck.missingKeys.length > 0
              ? `；已识别 ${headerCheck.matchedKeys.length} 列，其余列将留空`
              : '';
          message.success(`已追加 ${parsedRows.length} 行检测数据${colHint}`);
        }
      } catch (err) {
        console.error('Excel 解析失败', err);
        message.error('Excel 解析失败，请确认文件格式与表头与模板一致');
      }
    };
    reader.readAsBinaryString(file);
  };

  const handleAddRow = () => {
    const row = createEmptyRow();
    const next = [...gridRows, row];
    setGridRows(next);
    emitChange(next);
  };

  const deleteRowAt = useCallback(
    (rowIdx: number) => {
      setGridRows((prev) => {
        const removed = prev[rowIdx]?._rid;
        const next = padToMinRows(prev.filter((_, i) => i !== rowIdx));
        if (removed) {
          setTimeout(() => {
            setSelectedRows((s) => {
              const n = new Set(s);
              n.delete(removed);
              return n;
            });
          }, 0);
        }
        queueMicrotask(() => emitChange(next));
        return next;
      });
    },
    [emitChange, padToMinRows],
  );

  const deleteSelectedRows = () => {
    if (selectedRows.size === 0) {
      message.info('请先勾选要删除的行');
      return;
    }
    const next = padToMinRows(gridRows.filter((r) => !selectedRows.has(r._rid)));
    setSelectedRows(new Set());
    setGridRows(next);
    emitChange(next);
  };

  const dataColumnCount = useMemo(() => {
    if (isAlloyAnalysis) return Math.max(1, 1 + selectedElements.length);
    return Math.max(1, visibleSchemaColumns.length || 1);
  }, [isAlloyAnalysis, visibleSchemaColumns.length, selectedElements.length]);

  const dataColumnPixelWidth = useMemo(() => {
    const reserved =
      (!disabled ? SELECT_COL_WIDTH : 0) + ACTIONS_COL_WIDTH + GRID_SCROLL_GUTTER;
    if (gridWidth <= reserved) return MIN_DATA_COL_WIDTH;
    return Math.max(MIN_DATA_COL_WIDTH, Math.floor((gridWidth - reserved) / dataColumnCount));
  }, [gridWidth, disabled, dataColumnCount]);

  const renderHeaderWithTitle = useCallback(
    (label: string) =>
      function HeaderCell(props: RenderHeaderCellProps<GridRow>) {
        return (
          <div
            title={label}
            style={{
              width: '100%',
              height: '100%',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
            }}
          >
            {props.column.name}
          </div>
        );
      },
    [],
  );

  const columnWidthForLabel = useCallback(
    (label: string) => Math.max(dataColumnPixelWidth, estimateMinWidthForLabel(label)),
    [dataColumnPixelWidth],
  );

  const renderHighlightedCell = (
    props: RenderCellProps<GridRow>,
    content: React.ReactNode,
  ) => {
    const hl = getHighlightCellProps(props.rowIdx, props.column.key as string);
    return (
      <div
        className={hl.className}
        title={hl.title}
        style={{ ...hl.style, width: '100%', height: '100%', overflow: 'hidden', textOverflow: 'ellipsis' }}
      >
        {content}
      </div>
    );
  };

  const columns: readonly Column<GridRow>[] = useMemo(() => {
    const cols: Column<GridRow>[] = [];

    if (!disabled) {
      cols.push({
        ...SelectColumn,
        frozen: true,
        width: SELECT_COL_WIDTH,
      });
    }

    if (isAlloyAnalysis) {
      const numLabel = '编号';
      const numW = columnWidthForLabel(numLabel);
      cols.push({
        key: '编号',
        name: numLabel,
        width: numW,
        minWidth: estimateMinWidthForLabel(numLabel),
        editable: !disabled,
        renderHeaderCell: renderHeaderWithTitle(numLabel),
        renderEditCell: textEditor,
        renderCell: (p) =>
          renderHighlightedCell(
            p,
            p.row['编号'] === '/' || p.row['编号'] == null || p.row['编号'] === ''
              ? ''
              : String(p.row['编号']),
          ),
      });
      selectedElements.forEach((element) => {
        const elW = columnWidthForLabel(element);
        cols.push({
          key: element,
          name: element,
          width: elW,
          minWidth: estimateMinWidthForLabel(element),
          editable: !disabled,
          renderHeaderCell: renderHeaderWithTitle(element),
          renderEditCell: textEditor,
          renderCell: (p) =>
            renderHighlightedCell(
              p,
              p.row[element] === '/' || p.row[element] == null ? '' : String(p.row[element]),
            ),
        });
      });
    } else if (schema) {
      visibleSchemaColumns.forEach((col) => {
        const editCell =
          col.type === 'select' && col.options?.length
            ? createSelectColumnEditor(col.options)
            : textEditor;
        const colW = columnWidthForLabel(col.label);
        cols.push({
          key: col.key,
          name: col.label,
          width: colW,
          minWidth: estimateMinWidthForLabel(col.label),
          editable: !disabled,
          renderHeaderCell: renderHeaderWithTitle(col.label),
          renderEditCell: editCell,
          renderCell: (p) => {
            const v = p.row[col.key];
            const disp = v === '/' || v == null || v === '' ? '' : String(v);
            return renderHighlightedCell(p, disp);
          },
        });
      });
      if (supportsRecordOnlyDefect) {
        cols.push({
          key: RECORD_ONLY_DEFECT_KEY,
          name: RECORD_ONLY_DEFECT_KEY,
          width: RECORD_ONLY_COL_WIDTH,
          minWidth: RECORD_ONLY_COL_WIDTH,
          editable: false,
          renderHeaderCell: renderHeaderWithTitle(RECORD_ONLY_DEFECT_KEY),
          renderCell: (p) => (
            <Checkbox
              checked={isRecordOnlyDefectChecked(p.row[RECORD_ONLY_DEFECT_KEY])}
              disabled={disabled}
              onChange={(e) => setRecordOnlyForRow(p.rowIdx, e.target.checked)}
              onClick={(e) => e.stopPropagation()}
            />
          ),
        });
      }
    }

    cols.push({
      key: ACTIONS_COLUMN_KEY,
      name: '操作',
      width: ACTIONS_COL_WIDTH,
      frozen: true,
      editable: false,
      renderCell: (p) => (
        <Popconfirm title="确定删除该行？" onConfirm={() => deleteRowAt(p.rowIdx)} disabled={disabled}>
          <Button type="link" size="small" danger icon={<DeleteOutlined />} disabled={disabled}>
            删除
          </Button>
        </Popconfirm>
      ),
    });

    return cols;
  }, [
    disabled,
    isAlloyAnalysis,
    schema,
    selectedElements,
    nonComplianceHighlightMap,
    deleteRowAt,
    dataColumnPixelWidth,
    columnWidthForLabel,
    renderHeaderWithTitle,
    visibleSchemaColumns,
    supportsRecordOnlyDefect,
    setRecordOnlyForRow,
  ]);

  const gridHeight = Math.min(560, Math.max(200, 42 + Math.max(gridRows.length, 8) * 36));

  if (!schema) {
    return <div>加载中...</div>;
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
        <h4 style={{ margin: 0, fontSize: 14, color: '#1890ff' }}>检测数据详情</h4>
        <Space size="small" wrap>
          <Button size="small" icon={<DownloadOutlined />} onClick={handleDownloadTemplate} disabled={disabled}>
            下载 Excel 模板
          </Button>
          <Button size="small" icon={<DownloadOutlined />} onClick={handleExportCurrentData} disabled={disabled}>
            导出当前数据
          </Button>
          <Button size="small" icon={<UploadOutlined />} onClick={() => fileInputRef.current?.click()} disabled={disabled}>
            上传 Excel
          </Button>
          <input
            ref={fileInputRef}
            type="file"
            accept=".xlsx,.xls"
            style={{ display: 'none' }}
            onChange={handleUploadExcel}
          />
          {!disabled && selectedRows.size > 0 && (
            <Popconfirm title={`确定删除选中的 ${selectedRows.size} 行吗？`} onConfirm={deleteSelectedRows}>
              <Button size="small" danger>
                删除选中行
              </Button>
            </Popconfirm>
          )}
          <Button type="primary" size="small" icon={<SaveOutlined />} onClick={handleSaveAll} disabled={disabled}>
            保存
          </Button>
        </Space>
      </div>

      {isAlloyAnalysis && (
        <div style={{ marginBottom: 16, padding: 12, background: '#f5f5f5', borderRadius: 4 }}>
          <div style={{ marginBottom: 8, fontWeight: 500 }}>选择要检测的元素：</div>
          <Select
            mode="multiple"
            placeholder="请选择要检测的元素"
            value={selectedElements}
            onChange={handleElementSelectionChange}
            style={{ width: '100%' }}
            disabled={disabled}
            options={ALLOY_ELEMENTS.map((el) => ({ label: el, value: el }))}
          />
        </div>
      )}

      <div
        ref={gridWrapRef}
        className="detection-data-grid-wrap"
        onPaste={handlePaste}
        role="presentation"
        style={{ outline: 'none', width: '100%' }}
      >
        <DataGrid
          rowKeyGetter={(row) => row._rid}
          columns={columns}
          rows={gridRows}
          onRowsChange={(rows, data) => {
            if (disabled) return;
            if (data.column.key === ACTIONS_COLUMN_KEY || data.column.key === SELECT_COLUMN_KEY) return;
            handleRowsChange(rows as GridRow[]);
          }}
          selectedRows={disabled ? undefined : selectedRows}
          onSelectedRowsChange={disabled ? undefined : (set) => setSelectedRows(set)}
          rowHeight={36}
          headerRowHeight={36}
          style={{ height: gridHeight }}
          className="rdg-light"
          onFill={handleFill}
          onSelectedCellChange={(args) => {
            const k = args.column.key as string;
            if (k === SELECT_COLUMN_KEY || k === ACTIONS_COLUMN_KEY) return;
            selectedAnchorRef.current = { rowIdx: args.rowIdx, colKey: k };
          }}
        />
      </div>

      <Button
        type="dashed"
        icon={<PlusOutlined />}
        onClick={handleAddRow}
        disabled={disabled}
        block
        style={{ marginTop: 8 }}
      >
        添加检测数据行
      </Button>
    </div>
  );
};

export default DynamicDetectionDataTable;
