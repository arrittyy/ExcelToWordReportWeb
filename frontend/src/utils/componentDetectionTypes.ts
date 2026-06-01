import type { ReportList } from '@/types';
import { detectionTypeOrderIndex } from '@/utils/aggregateDetectionLogOrder';

function componentIdsFromReport(report: ReportList): number[] {
  if (report.projectComponentIds && report.projectComponentIds.length > 0) {
    return report.projectComponentIds;
  }
  if (report.projectComponentId != null && report.projectComponentId > 0) {
    return [report.projectComponentId];
  }
  return [];
}

/** 与 WordGeneratorServiceImpl.EXPERIMENT_TYPE_ORDER 一致（detectionTypeOrderIndex） */
function compareByFixedExperimentTypeOrder(a: string, b: string): number {
  const ta = detectionTypeOrderIndex(a);
  const tb = detectionTypeOrderIndex(b);
  if (ta !== tb) return ta - tb;
  return a.localeCompare(b, 'zh-CN');
}

/**
 * 按部件 ID 聚合已保存报告上的 experimentTypeName（去重、按 EXPERIMENT_TYPE_ORDER 固定顺序排列）。
 */
export function buildDetectionTypesByComponentId(reports: ReportList[]): Map<number, string[]> {
  const raw = new Map<number, Set<string>>();

  for (const report of reports) {
    if (report.id == null) continue;
    const typeName = report.experimentTypeName?.trim();
    if (!typeName) continue;

    for (const componentId of componentIdsFromReport(report)) {
      if (!raw.has(componentId)) {
        raw.set(componentId, new Set());
      }
      raw.get(componentId)!.add(typeName);
    }
  }

  const result = new Map<number, string[]>();
  for (const [componentId, typeSet] of raw) {
    const sorted = [...typeSet].sort(compareByFixedExperimentTypeOrder);
    result.set(componentId, sorted);
  }
  return result;
}
