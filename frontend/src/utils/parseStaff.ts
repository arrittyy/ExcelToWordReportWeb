/** 项目工作人员字段默认分隔符 */
export const STAFF_DELIMITER = '、';

/**
 * 解析项目 staff 字段为姓名数组，支持 |、、、, 分隔符。
 */
export function parseStaff(staffStr: string | undefined): string[] {
  if (!staffStr || !staffStr.trim()) return [];
  const s = staffStr.trim();
  if (s.includes('|')) {
    return s.split('|').map((x) => x.trim()).filter(Boolean);
  }
  if (s.includes(STAFF_DELIMITER)) {
    return s.split(STAFF_DELIMITER).map((x) => x.trim()).filter(Boolean);
  }
  if (s.includes(',')) {
    return s.split(',').map((x) => x.trim()).filter(Boolean);
  }
  return [s];
}

/** 解析检测人员字段（inspector）为姓名数组 */
export function parseInspectorNames(value: string | undefined): string[] {
  if (!value || !value.trim() || value === '/') return [];
  return value.split(/[|、,，\s]+/).map((t) => t.trim()).filter(Boolean);
}
