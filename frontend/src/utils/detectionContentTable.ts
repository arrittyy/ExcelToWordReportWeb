import type { DetectionContentTableRow } from '../types';

/** 规范化 table 模式检测内容行，含每行 minRequiredThickness；顶层旧字段迁移到 rows[0] */
export function normalizeTableDetectionContentRows(
  rawRows: unknown[],
  legacyTopLevelMin?: string,
): DetectionContentTableRow[] {
  const legacy = String(legacyTopLevelMin ?? '').trim();
  const list = Array.isArray(rawRows) ? rawRows : [];
  return list.map((r, index) => {
    const row = r && typeof r === 'object' ? (r as Record<string, unknown>) : {};
    let min = row.minRequiredThickness;
    if (
      (min === undefined || min === null || String(min).trim() === '') &&
      legacy &&
      index === 0
    ) {
      min = legacy;
    }
    const minStr =
      min === undefined || min === null ? '' : String(min).trim();
    let projectComponentId: number | undefined;
    const rawPcId = row.projectComponentId;
    if (typeof rawPcId === 'number' && rawPcId > 0) {
      projectComponentId = rawPcId;
    } else if (rawPcId != null && String(rawPcId).trim() !== '') {
      const parsed = Number(String(rawPcId).trim());
      if (Number.isFinite(parsed) && parsed > 0) {
        projectComponentId = parsed;
      }
    }
    const base: DetectionContentTableRow = {
      type: String(row.type ?? ''),
      locationDesc: String(row.locationDesc ?? ''),
      method: String(row.method ?? ''),
      result: String(row.result ?? ''),
      locationNumber: String(row.locationNumber ?? ''),
      total: String(row.total ?? ''),
      minRequiredThickness: minStr,
    };
    if (projectComponentId != null) {
      base.projectComponentId = projectComponentId;
    }
    return base;
  });
}

export function emptyTableDetectionContentRow(): DetectionContentTableRow {
  return {
    type: '',
    locationDesc: '',
    method: '',
    result: '',
    locationNumber: '',
    total: '',
    minRequiredThickness: '',
  };
}

/** 合并保存后检测内容：保留本次提交各行 minRequiredThickness（后端自动填充可能未带回） */
export function mergeSentTableRowMinThickness(
  saved: DetectionContentTableRow[],
  sent: DetectionContentTableRow[],
): DetectionContentTableRow[] {
  const n = Math.max(saved.length, sent.length);
  const out: DetectionContentTableRow[] = [];
  for (let i = 0; i < n; i++) {
    const s = saved[i] ?? emptyTableDetectionContentRow();
    const t = sent[i];
    const sentMin = t?.minRequiredThickness != null ? String(t.minRequiredThickness).trim() : '';
    const gotMin = s.minRequiredThickness != null ? String(s.minRequiredThickness).trim() : '';
    const sentPcId = t?.projectComponentId;
    const pcId =
      sentPcId != null && sentPcId > 0
        ? sentPcId
        : s.projectComponentId != null && s.projectComponentId > 0
          ? s.projectComponentId
          : undefined;
    out.push({
      ...s,
      minRequiredThickness: gotMin || sentMin,
      ...(pcId != null ? { projectComponentId: pcId } : {}),
    });
  }
  return out;
}
