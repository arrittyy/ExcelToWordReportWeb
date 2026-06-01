import type { ProjectComponent } from '@/services/componentService';

/** 与 WordGeneratorServiceImpl.CATEGORY_ORDER、aggregateDetectionLogOrder.OVERVIEW_CATEGORY_ORDER 同序；修改须同步 Java */
export const COMPONENT_CATEGORY_ORDER = [
  '汽机',
  '锅炉本体',
  '四大管道',
  '机炉外管道',
  '钢结构',
  '其他',
] as const;

const CATEGORY_INDEX = new Map<string, number>(
  COMPONENT_CATEGORY_ORDER.map((c, i) => [c, i]),
);

function categoryRank(category: string | undefined | null): number {
  const c = (category ?? '').trim();
  if (!c) return COMPONENT_CATEGORY_ORDER.length + 2;
  const idx = CATEGORY_INDEX.get(c);
  if (idx !== undefined) return idx;
  return COMPONENT_CATEGORY_ORDER.length + 1;
}

function compareUnknownCategories(a: string, b: string): number {
  return a.localeCompare(b, 'zh-CN');
}

const COMPONENT_NAME_LEADING_NUMBER_SIGN = /^#\d+号/;

/**
 * 去掉首部「# + 阿拉伯数字 + 号」后再排序（概述/部件表/报告列表/总日志组键共用）；与 WordGeneratorServiceImpl 同语义。
 * 剥空则回退为原始 trim 名称。
 */
export function normalizeComponentNameForGroupSort(name: string | undefined | null): string {
  const t = (name ?? '').trim();
  if (!t) return '';
  let s = t;
  for (;;) {
    const m = COMPONENT_NAME_LEADING_NUMBER_SIGN.exec(s);
    if (!m || m.index !== 0) break;
    s = s.slice(m[0].length).trim();
  }
  return s.length === 0 ? t : s;
}

function namePrefix2(name: string | undefined | null): string {
  return (name ?? '').trim().slice(0, 2);
}

function compareComponents(a: ProjectComponent, b: ProjectComponent): number {
  const ra = categoryRank(a.category);
  const rb = categoryRank(b.category);
  if (ra !== rb) return ra - rb;
  const ca = (a.category ?? '').trim();
  const cb = (b.category ?? '').trim();
  if (ra >= COMPONENT_CATEGORY_ORDER.length + 1 && ca !== cb) {
    return compareUnknownCategories(ca, cb);
  }

  const naNorm = normalizeComponentNameForGroupSort(a.componentName);
  const nbNorm = normalizeComponentNameForGroupSort(b.componentName);
  const pa = namePrefix2(naNorm);
  const pb = namePrefix2(nbNorm);
  const pc = pa.localeCompare(pb, 'zh-CN');
  if (pc !== 0) return pc;

  const nc = naNorm.localeCompare(nbNorm, 'zh-CN');
  if (nc !== 0) return nc;

  return a.id - b.id;
}

/** 返回新数组，不修改入参 */
export function sortProjectComponents(list: ProjectComponent[] | null | undefined): ProjectComponent[] {
  if (!list?.length) return [];
  return [...list].sort(compareComponents);
}

export function buildComponentSortRankMap(sortedList: ProjectComponent[]): Map<number, number> {
  const m = new Map<number, number>();
  sortedList.forEach((c, i) => m.set(c.id, i));
  return m;
}

export interface ReportLikeForComponentSort {
  id?: number;
  projectComponentId?: number;
  projectComponentIds?: number[];
}

function reportPrimaryRank(row: ReportLikeForComponentSort, rankMap: Map<number, number>): number {
  const ids =
    row.projectComponentIds && row.projectComponentIds.length > 0
      ? row.projectComponentIds
      : row.projectComponentId != null
        ? [row.projectComponentId]
        : [];
  let min = Number.MAX_SAFE_INTEGER;
  for (const cid of ids) {
    const r = rankMap.get(cid);
    if (r !== undefined && r < min) min = r;
  }
  return min;
}

/** 按关联部件在排序部件表中的名次排序；无关联垫底的稳定排序 */
export function sortReportsByComponentOrder<T extends ReportLikeForComponentSort>(
  rows: T[],
  rankMap: Map<number, number>,
): T[] {
  const copy = [...rows];
  copy.sort((a, b) => {
    const ra = reportPrimaryRank(a, rankMap);
    const rb = reportPrimaryRank(b, rankMap);
    if (ra !== rb) return ra - rb;
    const ida = a.id ?? Number.MAX_SAFE_INTEGER;
    const idb = b.id ?? Number.MAX_SAFE_INTEGER;
    return ida - idb;
  });
  return copy;
}
