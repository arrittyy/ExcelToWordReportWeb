import type { DetectionContentTableRow } from '@/types';
import type { ProjectComponent } from '@/services/componentService';
import { buildComponentSummaryLine } from '@/utils/componentSummaryLine';

/**
 * 与后端 DetectionContentRowComponentResolver 一致：
 * 1) rows[i].projectComponentId（须在 projectComponentIds 内）
 * 2) 否则 projectComponentIds[i]
 */
export function resolveDetectionContentRowComponentId(
  row: DetectionContentTableRow | undefined,
  rowIndex: number,
  projectComponentIds: number[] | undefined,
): number | undefined {
  const selected = projectComponentIds ?? [];
  const rowId = row?.projectComponentId;
  if (rowId != null && rowId > 0 && selected.includes(rowId)) {
    return rowId;
  }
  if (rowIndex >= 0 && rowIndex < selected.length) {
    return selected[rowIndex];
  }
  return undefined;
}

export function buildRowComponentSummary(
  row: DetectionContentTableRow | undefined,
  rowIndex: number,
  projectComponentIds: number[] | undefined,
  components: ProjectComponent[],
): string {
  const cid = resolveDetectionContentRowComponentId(row, rowIndex, projectComponentIds);
  if (cid == null) return '';
  const comp = components.find((c) => c.id === cid);
  return buildComponentSummaryLine(comp);
}

/** 变更报告级多选部件后，清理行内不在新列表中的 projectComponentId */
export function sanitizeTableRowComponentIds(
  rows: DetectionContentTableRow[],
  projectComponentIds: number[] | undefined,
): DetectionContentTableRow[] {
  const allowed = new Set(projectComponentIds ?? []);
  return rows.map((row, i) => {
    const id = row.projectComponentId;
    if (id != null && id > 0 && !allowed.has(id)) {
      const { projectComponentId: _removed, ...rest } = row;
      return rest;
    }
    const resolved = resolveDetectionContentRowComponentId(row, i, projectComponentIds);
    if (resolved != null && row.projectComponentId == null && i < (projectComponentIds?.length ?? 0)) {
      return { ...row, projectComponentId: resolved };
    }
    return row;
  });
}
