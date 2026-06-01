/** 第三方名称预设选项；与后端 ThirdPartyReportNumbering 中华图判定名称保持一致 */
export const THIRD_PARTY_NAME_PRESETS = [
  '天津市思维奇检测技术有限公司',
  '安徽华图电力科技有限公司',
] as const;

export const thirdPartyNameOptions = THIRD_PARTY_NAME_PRESETS.map((name) => ({
  value: name,
}));
