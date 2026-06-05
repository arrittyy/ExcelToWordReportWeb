export type MaterialCategory = 'alloy' | 'leeb' | 'bolt' | 'mechanical' | 'hardness';

export const MATERIAL_CATEGORY_LABELS: Record<MaterialCategory, string> = {
  alloy: '合金分析',
  leeb: '里氏硬度',
  bolt: '螺栓',
  mechanical: '力学性能',
  hardness: '布氏/维氏/洛氏',
};

export const MATERIAL_META_FIELDS = [
  { key: 'GB5310牌号', label: '国标牌号' },
  { key: '国外牌号', label: '国外牌号' },
] as const;

export const MATERIAL_CATEGORY_FIELDS: Record<MaterialCategory, { key: string; label: string }[]> = {
  alloy: [
    { key: 'Mn', label: 'Mn' },
    { key: 'Cr', label: 'Cr' },
    { key: 'Mo', label: 'Mo' },
    { key: 'V', label: 'V' },
    { key: 'Ti', label: 'Ti' },
    { key: 'Ni', label: 'Ni' },
    { key: 'Al', label: 'Al' },
    { key: 'Cu', label: 'Cu' },
    { key: 'Nb', label: 'Nb' },
    { key: 'W', label: 'W' },
    { key: 'Co', label: 'Co' },
    { key: 'Mg', label: 'Mg' },
    { key: 'Zr', label: 'Zr' },
  ],
  leeb: [
    { key: '里氏-管件', label: '管件' },
    { key: '里氏-钢管', label: '钢管' },
    { key: '里氏-焊缝', label: '焊缝' },
    { key: '里氏', label: '母材' },
  ],
  bolt: [{ key: '里氏-螺栓', label: '螺栓' }],
  mechanical: [
    { key: '抗拉强度', label: '抗拉强度' },
    { key: '下屈服强度', label: '下屈服强度' },
    { key: '断后伸长率', label: '断后伸长率' },
  ],
  hardness: [
    { key: '布氏', label: '布氏' },
    { key: '维氏', label: '维氏' },
    { key: '洛氏', label: '洛氏' },
  ],
};
