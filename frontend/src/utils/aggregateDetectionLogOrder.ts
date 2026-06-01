import type { ReportList } from '@/types';
import type { ProjectComponent } from '@/services/componentService';
import { normalizeComponentNameForGroupSort } from '@/utils/sortProjectComponents';

export type AggregateOrderState = {
  version: number;
  componentKeys: string[];
  reportIdsByComponent: Record<string, number[]>;
  /** 全局检测类型顺序；与 Word 中解析的 experimentTypeOrder 一致 */
  experimentTypeOrder?: string[];
};

/** 与 WordGeneratorServiceImpl.CATEGORY_ORDER 一致；修改须同步 Java 及 sortProjectComponents.COMPONENT_CATEGORY_ORDER */
export const OVERVIEW_CATEGORY_ORDER: readonly string[] = [
  '汽机',
  '锅炉本体',
  '四大管道',
  '机炉外管道',
  '钢结构',
  '其他',
] as const;

const AGGREGATE_ORDER_VERSION = 4;
/** 仍接受库中旧数据 */
const LEGACY_AGGREGATE_ORDER_VERSION = 2;

/** 与 WordGeneratorServiceImpl.EXPERIMENT_TYPE_ORDER 同序；改顺序须同步 Java */
export const DEFAULT_DETECTION_TYPE_ORDER: readonly string[] = [
  '目视检测',
  '相控阵超声波检测',
  '超声波检测',
  '磁粉检测',
  '渗透检测',
  '金相检测',
  '硬度检测（里氏、布氏、洛氏、维氏硬度）',
  '氧化皮检测',
  '超声测厚',
  '合金分析检测',
  '圆度测量',
  '管径测量',
  '内窥镜检查',
  '射线检测',
  '涡流检测',
  '室温拉伸',
  '高温拉伸',
  '冲击吸收能',
  '有效硬化层',
  '高温持久强度',
] as const;

const UNKNOWN_TYPE_ORDER = DEFAULT_DETECTION_TYPE_ORDER.length;

/**
 * 检测类型在默认顺序中的序号；未知类型排在末尾（与后端 getExperimentTypeOrderIndexFixed 同义规则对齐）。
 */
export function detectionTypeOrderIndex(experimentTypeName: string | null | undefined): number {
  const name = experimentTypeName?.trim() ?? '';
  if (!name) return UNKNOWN_TYPE_ORDER;
  const exact = DEFAULT_DETECTION_TYPE_ORDER.indexOf(name);
  if (exact >= 0) return exact;
  if (name.includes('相控阵')) return 1;
  if (name.includes('测厚')) return 8;
  if (name === '超声检测' || name === '超声波检测') return 2;
  if (name.includes('磁粉')) return 3;
  if (name.includes('渗透')) return 4;
  if (name.includes('金相')) return 5;
  if (
    name.includes('里氏') ||
    name.includes('布氏') ||
    name.includes('布什') ||
    name.includes('洛氏') ||
    name.includes('维氏')
  ) {
    return 6;
  }
  if (name.includes('氧化皮')) return 7;
  if (name.includes('合金分析')) return 9;
  if (name.includes('圆度')) return 10;
  if (name.includes('管径')) return 11;
  if (name.includes('内窥镜')) return 12;
  if (name.includes('射线')) return 13;
  if (name.includes('涡流')) return 14;
  if (name.includes('室温拉伸')) return 15;
  if (name.includes('高温拉伸')) return 16;
  if (name.includes('冲击吸收')) return 17;
  if (name.includes('有效硬化层')) return 18;
  if (name.includes('高温持久')) return 19;
  if (name.includes('目视')) return 0;
  if (name.includes('硬度')) return 6;
  return UNKNOWN_TYPE_ORDER;
}

/** 在自定义全局类型序下的序号；未出现在列表中的类型排在列表之后，再用默认模糊序 */
export function detectionTypeOrderIndexWithCustomList(
  experimentTypeName: string | null | undefined,
  customOrder: string[],
): number {
  if (!customOrder.length) {
    return detectionTypeOrderIndex(experimentTypeName);
  }
  const name = experimentTypeName?.trim() ?? '';
  if (!name) {
    return customOrder.length + UNKNOWN_TYPE_ORDER;
  }
  for (let i = 0; i < customOrder.length; i++) {
    if (customOrder[i]?.trim() === name) {
      return i;
    }
  }
  return customOrder.length + detectionTypeOrderIndex(experimentTypeName);
}

export function reportSortKey(r: ReportList): string {
  return r.reportNumber || '';
}

function compareReportsForAggregateDefault(a: ReportList, b: ReportList): number {
  const ta = detectionTypeOrderIndex(a.experimentTypeName);
  const tb = detectionTypeOrderIndex(b.experimentTypeName);
  if (ta !== tb) return ta - tb;
  return reportSortKey(a).localeCompare(reportSortKey(b), undefined, { numeric: true });
}

export function compareReportsForAggregateWithCustomOrder(
  a: ReportList,
  b: ReportList,
  experimentTypeOrder: string[],
): number {
  const ta = detectionTypeOrderIndexWithCustomList(a.experimentTypeName, experimentTypeOrder);
  const tb = detectionTypeOrderIndexWithCustomList(b.experimentTypeName, experimentTypeOrder);
  if (ta !== tb) return ta - tb;
  return reportSortKey(a).localeCompare(reportSortKey(b), undefined, { numeric: true });
}

/** 稳定分组键：多选部件按 id 排序后拼接 */
export function componentGroupKey(report: ReportList): string {
  const ids =
    report.projectComponentIds && report.projectComponentIds.length > 0
      ? [...report.projectComponentIds]
      : report.projectComponentId != null
        ? [report.projectComponentId]
        : [];
  if (ids.length === 0) return 'none';
  return [...ids].sort((a, b) => a - b).join(',');
}

function namePrefix2Normalized(rawName: string | undefined | null): string {
  const n = normalizeComponentNameForGroupSort(rawName);
  return n.trim().slice(0, 2);
}

export function parseComponentGroupIds(key: string): number[] {
  if (!key || key === 'none') return [];
  return key
    .split(',')
    .map((s) => parseInt(s.trim(), 10))
    .filter((n) => !Number.isNaN(n));
}

/** 与 WordGeneratorServiceImpl 概述部件组 Comparator 同义 */
export function compareComponentGroupKeysForOverview(
  keyA: string,
  keyB: string,
  componentsById: Map<number, ProjectComponent>,
): number {
  if (keyA === 'none' && keyB === 'none') return 0;
  if (keyA === 'none') return 1;
  if (keyB === 'none') return -1;

  const triple = (key: string) => {
    const ids = parseComponentGroupIds(key);
    let minPrefix = '';
    let minFull = '';
    let minId = Number.MAX_SAFE_INTEGER;
    let first = true;
    for (const id of ids) {
      minId = Math.min(minId, id);
      const c = componentsById.get(id);
      const norm = normalizeComponentNameForGroupSort(c?.componentName);
      const p2 = namePrefix2Normalized(norm);
      if (first) {
        minPrefix = p2;
        minFull = norm;
        first = false;
      } else {
        if (p2.localeCompare(minPrefix, 'zh-CN') < 0) minPrefix = p2;
        if (norm.localeCompare(minFull, 'zh-CN') < 0) minFull = norm;
      }
    }
    return { minPrefix, minFull, minId: minId === Number.MAX_SAFE_INTEGER ? 0 : minId };
  };

  const ta = triple(keyA);
  const tb = triple(keyB);
  let c = ta.minPrefix.localeCompare(tb.minPrefix, 'zh-CN');
  if (c !== 0) return c;
  c = ta.minFull.localeCompare(tb.minFull, 'zh-CN');
  if (c !== 0) return c;
  return ta.minId - tb.minId;
}

/** 部件组所属类别（与 Word 概述一致：部件 category，空为「其他」） */
export function categoryForComponentGroupKey(
  key: string,
  componentsById: Map<number, ProjectComponent>,
): string {
  if (key === 'none') return '其他';
  const ids = parseComponentGroupIds(key);
  for (const id of ids) {
    const c = componentsById.get(id);
    const cat = c?.category?.trim();
    if (cat) return cat;
  }
  return '其他';
}

function buildComponentsById(components?: ProjectComponent[]): Map<number, ProjectComponent> {
  const byId = new Map<number, ProjectComponent>();
  if (components?.length) {
    for (const c of components) {
      if (c.id != null) byId.set(c.id, c);
    }
  }
  return byId;
}

/** 默认全局检测类型序：标准表 + 项目中出现但未在表中的类型名（保持首次出现顺序） */
export function buildDefaultExperimentTypeOrderFromReports(reports: ReportList[]): string[] {
  const out = [...DEFAULT_DETECTION_TYPE_ORDER];
  const seen = new Set(out.map((s) => s.trim()));
  for (const r of reports) {
    const n = r.experimentTypeName?.trim();
    if (n && !seen.has(n)) {
      seen.add(n);
      out.push(n);
    }
  }
  return out;
}

/** 报告中首次出现的非空 experimentTypeName（去重、保序） */
export function collectProjectExperimentTypeNamesInOrder(reports: ReportList[]): string[] {
  const out: string[] = [];
  const seen = new Set<string>();
  for (const r of reports) {
    const n = r.experimentTypeName?.trim();
    if (!n || seen.has(n)) continue;
    seen.add(n);
    out.push(n);
  }
  return out;
}

/**
 * 弹窗与保存：在合并后的全局类型序上只保留「项目实际用到的」检测类型名；
 * 已用但未出现在 mergedOrder 中的类型按报告首次出现顺序接在末尾。
 */
export function buildProjectScopedExperimentTypeOrder(
  mergedOrder: string[],
  reports: ReportList[],
): string[] {
  const usedInOrder = collectProjectExperimentTypeNamesInOrder(reports);
  const usedSet = new Set(usedInOrder);
  const out: string[] = [];
  const seen = new Set<string>();
  for (const t of mergedOrder) {
    const s = t?.trim();
    if (!s || !usedSet.has(s) || seen.has(s)) continue;
    seen.add(s);
    out.push(s);
  }
  for (const n of usedInOrder) {
    if (!seen.has(n)) {
      seen.add(n);
      out.push(n);
    }
  }
  return out;
}

/** 标准类按 OVERVIEW_CATEGORY_ORDER，自定义类在其后按 zh-CN 排序；与 Java sortCategoriesForOverview 一致 */
export function sortCategoriesForOverview(categories: Set<string>): string[] {
  const list = [...categories];
  const known = new Set<string>(OVERVIEW_CATEGORY_ORDER);
  const ordered: string[] = [];
  for (const c of OVERVIEW_CATEGORY_ORDER) {
    if (categories.has(c)) ordered.push(c);
  }
  const rest = list.filter((c) => !known.has(c));
  rest.sort((a, b) => a.localeCompare(b, 'zh-CN'));
  return [...ordered, ...rest];
}

function defaultOrder(reports: ReportList[], components?: ProjectComponent[]): AggregateOrderState {
  const withId = reports.filter((r) => r.id != null);
  const byId = buildComponentsById(components);

  const uniqueKeys = new Set<string>();
  for (const r of withId) uniqueKeys.add(componentGroupKey(r));

  const keysByCategory = new Map<string, string[]>();
  for (const key of uniqueKeys) {
    const cat = categoryForComponentGroupKey(key, byId);
    if (!keysByCategory.has(cat)) keysByCategory.set(cat, []);
    keysByCategory.get(cat)!.push(key);
  }

  const categoryOrder = sortCategoriesForOverview(new Set(keysByCategory.keys()));

  const componentKeys: string[] = [];
  for (const cat of categoryOrder) {
    const keys = keysByCategory.get(cat) || [];
    keys.sort((a, b) => compareComponentGroupKeysForOverview(a, b, byId));
    componentKeys.push(...keys);
  }

  const experimentTypeOrder = buildProjectScopedExperimentTypeOrder(
    buildDefaultExperimentTypeOrderFromReports(withId),
    withId,
  );

  const reportIdsByComponent: Record<string, number[]> = {};
  for (const k of componentKeys) {
    reportIdsByComponent[k] = [];
  }
  for (const r of withId) {
    const key = componentGroupKey(r);
    reportIdsByComponent[key].push(r.id as number);
  }
  for (const k of componentKeys) {
    const rows = (reportIdsByComponent[k] || [])
      .map((id) => withId.find((x) => x.id === id))
      .filter((x): x is ReportList => x != null);
    rows.sort((a, b) => compareReportsForAggregateWithCustomOrder(a, b, experimentTypeOrder));
    reportIdsByComponent[k] = rows.map((x) => x.id as number);
  }

  return {
    version: AGGREGATE_ORDER_VERSION,
    componentKeys,
    reportIdsByComponent,
    experimentTypeOrder,
  };
}

function mergeExperimentTypeOrderLists(
  saved: string[] | undefined,
  reports: ReportList[],
): string[] {
  const base = buildDefaultExperimentTypeOrderFromReports(reports);
  if (!saved?.length) return base;
  const out: string[] = [];
  const seen = new Set<string>();
  for (const t of saved) {
    const s = t?.trim();
    if (!s || seen.has(s)) continue;
    seen.add(s);
    out.push(s);
  }
  for (const t of base) {
    if (!seen.has(t)) {
      seen.add(t);
      out.push(t);
    }
  }
  return out;
}

/**
 * 与数据库已保存顺序合并：保留用户部件块顺序；组内报告按 experimentTypeOrder 重排。
 */
export function mergeSavedOrder(
  reports: ReportList[],
  savedJson: string | null | undefined,
  components?: ProjectComponent[],
): AggregateOrderState {
  const defaults = defaultOrder(reports, components);
  if (!savedJson?.trim()) return defaults;

  let saved: Partial<AggregateOrderState> & { version?: number };
  try {
    saved = JSON.parse(savedJson) as Partial<AggregateOrderState> & { version?: number };
  } catch {
    return defaults;
  }

  const v = saved.version;
  if (v !== AGGREGATE_ORDER_VERSION && v !== LEGACY_AGGREGATE_ORDER_VERSION) {
    return defaults;
  }
  if (!Array.isArray(saved.componentKeys) || !saved.reportIdsByComponent || typeof saved.reportIdsByComponent !== 'object') {
    return defaults;
  }

  const validIds = new Set(
    reports.map((r) => r.id).filter((id): id is number => id != null),
  );

  const defaultKeys = defaults.componentKeys;
  const savedKeysFiltered = saved.componentKeys.filter((k) => defaultKeys.includes(k));
  const newKeys = defaultKeys.filter((k) => !savedKeysFiltered.includes(k));
  const mergedKeys = [...savedKeysFiltered, ...newKeys];

  const withId = reports.filter((r) => r.id != null);
  const mergedTypeOrderRaw = mergeExperimentTypeOrderLists(saved.experimentTypeOrder, reports);
  const experimentTypeOrder = buildProjectScopedExperimentTypeOrder(mergedTypeOrderRaw, withId);

  const mergedReports: Record<string, number[]> = {};
  for (const key of mergedKeys) {
    const inDefault = new Set((defaults.reportIdsByComponent[key] || []).filter((id) => validIds.has(id)));
    const ids = [...inDefault];
    const rows = ids
      .map((id) => withId.find((x) => x.id === id))
      .filter((x): x is ReportList => x != null);
    rows.sort((a, b) => compareReportsForAggregateWithCustomOrder(a, b, experimentTypeOrder));
    mergedReports[key] = rows.map((x) => x.id as number);
  }

  return {
    version: AGGREGATE_ORDER_VERSION,
    componentKeys: mergedKeys,
    reportIdsByComponent: mergedReports,
    experimentTypeOrder,
  };
}

/** 组内用于展示的参考报告（按当前全局类型序下第一条） */
export function getRepresentativeReport(
  reports: ReportList[],
  groupKey: string,
  experimentTypeOrder?: string[],
): ReportList | null {
  const groupReports = reports.filter(
    (r) => r.id != null && componentGroupKey(r) === groupKey,
  );
  if (groupReports.length === 0) return null;
  if (!experimentTypeOrder?.length) {
    return [...groupReports].sort(compareReportsForAggregateDefault)[0];
  }
  return [...groupReports].sort((a, b) =>
    compareReportsForAggregateWithCustomOrder(a, b, experimentTypeOrder),
  )[0];
}

/** 从合并后的顺序构造弹窗用的「类别顺序 + 各类别下部件组键」 */
export function buildCategoryStructureFromMergedOrder(
  merged: AggregateOrderState,
  components?: ProjectComponent[],
): { categoryOrder: string[]; componentKeysByCategory: Record<string, string[]> } {
  const byId = buildComponentsById(components);
  const categoryOrder: string[] = [];
  const componentKeysByCategory: Record<string, string[]> = {};

  for (const key of merged.componentKeys) {
    const cat = categoryForComponentGroupKey(key, byId);
    if (!componentKeysByCategory[cat]) {
      componentKeysByCategory[cat] = [];
      categoryOrder.push(cat);
    }
    componentKeysByCategory[cat].push(key);
  }
  return { categoryOrder, componentKeysByCategory };
}
