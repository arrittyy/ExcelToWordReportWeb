import { detectionTypeOrderIndex } from '@/utils/aggregateDetectionLogOrder';

/**
 * 检测内容「类型」用于叙述/摘要拼接时去掉括注（与后端 TypeLabelUtil 一致）。
 * 入库与下拉展示仍保留完整选项。
 */
export function stripTypeParentheticalForConcat(label: string): string {
  if (!label) return '';
  return label
    .replace(/（[^）]*）/g, '')
    .replace(/\([^)]*\)/g, '')
    .trim();
}

/** Ant Tag 预设色，按检测类型顺序循环映射 */
const DETECTION_TYPE_TAG_COLORS = [
  'success',
  'processing',
  'blue',
  'cyan',
  'purple',
  'magenta',
  'orange',
  'gold',
  'lime',
  'geekblue',
  'volcano',
] as const;

/**
 * 检测类型简称：去掉「检测 / 测量 / 检查」后缀（与 EXPERIMENT_TYPE_ORDER 展示一致）。
 * 带括注时先处理「检测（」→「（」。
 */
export function abbreviateDetectionTypeName(fullName: string): string {
  const trimmed = fullName?.trim() ?? '';
  if (!trimmed) return '';
  return trimmed
    .replace(/检测(?=（)/, '')
    .replace(/(检测|测量|检查)$/, '');
}

/** 同一检测类型始终映射到相同 Tag 颜色 */
export function detectionTypeTagColor(fullName: string): string {
  const idx = detectionTypeOrderIndex(fullName);
  return DETECTION_TYPE_TAG_COLORS[idx % DETECTION_TYPE_TAG_COLORS.length];
}
