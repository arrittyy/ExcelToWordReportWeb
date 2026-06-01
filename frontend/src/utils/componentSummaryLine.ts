import type { ProjectComponent } from '@/services/componentService';
import { getComponentDisplaySpec } from '@/utils/projectComponentDisplaySpec';

const SEP = ' / ';

/** 检测内容表「部件摘要」列：名称 + 材质 + 规格（与 getComponentDisplaySpec 一致），空段省略 */
export function buildComponentSummaryLine(comp: ProjectComponent | null | undefined): string {
  if (!comp) return '';
  const parts: string[] = [];
  const name = (comp.componentName ?? '').trim();
  if (name) parts.push(name);
  const mat = (comp.material ?? '').trim();
  if (mat) parts.push(mat);
  const spec = getComponentDisplaySpec(comp).trim();
  if (spec) parts.push(spec);
  return parts.join(SEP);
}
