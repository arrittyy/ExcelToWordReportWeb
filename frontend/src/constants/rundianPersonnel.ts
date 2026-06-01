/**
 * 润电内部检测人员及记录编号用人员代码。
 * 与后端 RunDianPersonnelRegistry.java 保持同步。
 */
export interface RundianPersonnelEntry {
  name: string;
  code: number;
}

export const RUNDIAN_PERSONNEL: RundianPersonnelEntry[] = [
  { name: '蔡红生', code: 10 },
  { name: '李世涛', code: 12 },
  { name: '杨旭', code: 14 },
  { name: '魏泉泉', code: 20 },
  { name: '靳峰', code: 22 },
  { name: '陈岩', code: 27 },
  { name: '胡锋涛', code: 33 },
  { name: '马东方', code: 34 },
  { name: '牛保献', code: 37 },
  { name: '周书康', code: 40 },
  { name: '李世铭', code: 43 },
  { name: '李艳军', code: 44 },
  { name: '侯家绪', code: 45 },
  { name: '高建忠', code: 46 },
  { name: '徐亮', code: 47 },
  { name: '郭文', code: 48 },
  { name: '符勇', code: 52 },
  { name: '闫宁', code: 53 },
  { name: '高秀娜', code: 54 },
  { name: '魏烁', code: 56 },
  { name: '张书浩', code: 57 },
  { name: '王志永', code: 59 },
  { name: '王红宝', code: 60 },
  { name: '张晓霓', code: 61 },
  { name: '蒋豹', code: 62 },
  { name: '王凌颉', code: 64 },
  { name: '王鹏飞', code: 65 },
  { name: '贾新杰', code: 67 },
  { name: '宋可可', code: 69 },
  { name: '武莹莹', code: 70 },
  { name: '王强', code: 71 },
  { name: '杨希锐', code: 72 },
  { name: '肖乐园', code: 73 },
  { name: '马泽军', code: 74 },
  { name: '张庆巍', code: 75 },
  { name: '张博炜', code: 76 },
  { name: '陈莉君', code: 77 },
  { name: '白鹏辉', code: 78 },
  { name: '卢申', code: 90 },
  { name: '王佳朋', code: 91 },
  { name: '孙赞', code: 92 },
  { name: '王志明', code: 93 },
  { name: '朱培营', code: 94 },
  { name: '句慧文', code: 95 },
];

export const RUNDIAN_PERSONNEL_NAMES: string[] = RUNDIAN_PERSONNEL.map((p) => p.name).sort((a, b) =>
  a.localeCompare(b, 'zh-CN'),
);
