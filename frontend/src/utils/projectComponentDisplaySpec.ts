import type { ProjectComponent } from '@/services/componentService';

/** 与 ReportComponentMergeHelper.resolveSpecPrefixMode 一致（displaySpec 缺失时的 fallback） */
export function resolveSpecPrefixMode(comp: ProjectComponent): 'PHI' | 'M' | 'NONE' {
  const raw = comp.specPrefix;
  if (raw == null || String(raw).trim() === '') {
    const nm = comp.componentName ?? '';
    if (nm.includes('螺栓') || nm.includes('螺帽')) return 'M';
    return 'PHI';
  }
  const t = String(raw).trim();
  const u = t.toUpperCase();
  if (u === 'PHI' || t === 'Φ') return 'PHI';
  if (u === 'M') return 'M';
  if (u === 'NONE') return 'NONE';
  return 'PHI';
}

/** 与 ReportComponentMergeHelper.formatSpecUnified 一致（无 API displaySpec 时使用） */
export function formatSpecUnifiedFallback(comp: ProjectComponent): string {
  const mode = resolveSpecPrefixMode(comp);
  const pd = comp.pipeDiameter?.trim() ?? '';
  const wt = comp.wallThickness?.trim() ?? '';
  const pitch = comp.threadPitch?.trim() ?? '';
  let sb = '';
  if (pd) {
    if (mode === 'PHI') sb += `Φ${pd}`;
    else if (mode === 'M') sb += `M${pd}`;
    else sb += pd;
  }
  if (wt) sb += `mm × ${wt}mm`;
  if (pitch) {
    if (sb) sb += ' × ';
    sb += pitch;
  }
  return sb;
}

/** 优先使用后端 displaySpec，与 Word/报告拼接规则一致 */
export function getComponentDisplaySpec(comp: ProjectComponent): string {
  const d = comp.displaySpec?.trim();
  if (d) return d;
  return formatSpecUnifiedFallback(comp);
}
