import type { ProjectComponent } from '@/services/componentService';
import { getComponentDisplaySpec } from '@/utils/projectComponentDisplaySpec';

/** 统一部件 ID（接口/json 可能出现 string，避免 === 匹配失败） */
export function normalizeProjectComponentIdList(ids: unknown, singleId: unknown): number[] {
  if (Array.isArray(ids) && ids.length > 0) {
    return ids.map((x) => Number(x)).filter((n) => Number.isFinite(n));
  }
  if (singleId != null && singleId !== '') {
    const n = Number(singleId as number | string);
    return Number.isFinite(n) ? [n] : [];
  }
  return [];
}

/** 按报告关联的部件 ID 解析为部件实体（与表格展示同源） */
export function resolveProjectComponentsByIds(
  ids: unknown,
  singleId: unknown,
  allComponents: ProjectComponent[],
): ProjectComponent[] {
  const resolved = normalizeProjectComponentIdList(ids, singleId);
  if (resolved.length === 0 || !allComponents.length) return [];
  const out: ProjectComponent[] = [];
  for (const cid of resolved) {
    const c = allComponents.find((x) => Number(x.id) === cid);
    if (c) out.push(c);
  }
  return out;
}

/** 部件下拉：名称 · 规格 · 材质（备注附后） */
export function formatComponentLabel(comp: ProjectComponent): string {
  const specStr = getComponentDisplaySpec(comp);
  const mat = comp.material?.trim();
  const remarkPart = comp.remark?.trim() ? `（${comp.remark.trim()}）` : '';
  const main = [comp.componentName, specStr, mat].filter(Boolean).join(' · ');
  return remarkPart ? `${main} ${remarkPart}` : main;
}

/** 与 ReportComponentMergeHelper.mergeMaterials：顺序拼接、去掉连续重复 */
export function mergeMaterialsDisplay(comps: ProjectComponent[]): string {
  const parts: string[] = [];
  for (const c of comps) {
    const m = c.material?.trim() ?? '';
    if (!m) continue;
    if (parts.length > 0 && parts[parts.length - 1] === m) continue;
    parts.push(m);
  }
  return parts.join('/');
}

/** 多选部件只读：名称 · 规格1/规格2 · 合并材质 · 备注（与项目详情报告表「部件名称」列一致） */
export function formatMultiComponentDisplay(
  ids: number[] | undefined,
  singleId: number | undefined,
  allComponents: ProjectComponent[],
): string {
  const comps = resolveProjectComponentsByIds(ids, singleId, allComponents);
  if (comps.length === 0) return '-';
  const name = comps[0].componentName || '';
  const specParts = comps.map((c) => getComponentDisplaySpec(c)).filter(Boolean);
  const specStr = specParts.length ? specParts.join('/') : '';
  const matStr = mergeMaterialsDisplay(comps);
  const remarkPart = comps.map((c) => c.remark?.trim()).filter(Boolean).join(' / ');
  const remarkSuffix = remarkPart ? `（${remarkPart}）` : '';
  return [name, specStr, matStr, remarkSuffix].filter(Boolean).join(' · ');
}
