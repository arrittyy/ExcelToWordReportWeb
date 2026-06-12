/** GET /api/reports/:id/export-text-preview */
export interface ExportTextPreview {
  detectionNarrativeBodyDefault: string;
  detectionNarrativeBodySaved: string;
  conclusionParagraphDefault: string;
  conclusionParagraphSaved: string;
  overviewWorkContentLineDefault: string;
  overviewWorkContentLineSaved: string;
  /** 与 Word 导出一致：自定义句内编号已按当前报告编号替换 */
  overviewWorkContentLineEffective: string;
  overviewDefectLineDefault: string;
  overviewDefectLineSaved: string;
  showDefectSection: boolean;
  contentRowIndex?: number;
  contentRowCount?: number;
  contentRowType?: string;
  overviewMultiSegment?: boolean;
}

/** PUT /api/reports/:id/export-text-overrides */
export interface ExportTextOverridesPatch {
  detectionNarrativeBody?: string;
  conclusionParagraph?: string;
  overviewWorkContentLine?: string;
  overviewDefectLine?: string;
  /** 非 undefined 时仅更新该 detectionContent 行的单项报告覆盖 */
  contentRowIndex?: number;
}

/** GET /api/projects/:id/overview-preview */
export interface OverviewPreviewItem {
  number: string;
  text: string;
}

export interface OverviewPreviewComponent {
  componentName: string;
  componentIndex: number;
  items: OverviewPreviewItem[];
}

export interface OverviewPreviewCategory {
  category: string;
  chapter2CategoryIndex: number;
  chapter3CategoryIndex: number;
  categoryIndex: number;
  chapter2Components: OverviewPreviewComponent[];
  chapter3Components: OverviewPreviewComponent[];
}

export interface ProjectOverviewPreview {
  abstractParagraph: string;
  section1Body: string;
  showChapter2: boolean;
  categories: OverviewPreviewCategory[];
}

// Instrument related types
export interface Instrument {
  id: number;
  instrumentName: string;
  instrumentModel?: string;
  instrumentNumber?: string;
  createdAt: string;
  updatedAt: string;
}

// Project related types

export interface ProjectList {
  id: number;
  projectNumber: string;
  /** 第三方项目编号（可选） */
  thirdPartyProjectNumber?: string;
  /** 第三方名称（可选） */
  thirdPartyName?: string;
  projectName: string;
  projectType?: string; // 项目类型（必填）
  customer?: string; // 客户方
  customerContact?: string; // 客户方人员
  powerPlantId?: number; // 电厂ID
  unitId?: number; // 机组ID
  startDate: string;
  endDate?: string;
  status: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
  userFullName?: string;
  /** 项目归属主账号 ID */
  userId?: string;
  reportCount: number;
  responsiblePerson?: string;
  // 无损检测相关字段
  reviewerNdt?: string;
  reviewDateNdt?: string;
  approverNdt?: string;
  approvalDateNdt?: string;
  writerNdt?: string;
  writerDateNdt?: string;
  // 理化检测相关字段
  reviewerChem?: string;
  reviewDateChem?: string;
  approverChem?: string;
  approvalDateChem?: string;
  writerChem?: string;
  writerDateChem?: string;
  /** 无损审批步骤 0=编写 1=待审核 2=待批准 3=已通过 */
  approvalStepNdt?: number;
  /** 理化审批步骤 */
  approvalStepChem?: number;
  /** 无损：曾不通过的节点 1=审核 2=批准 */
  rejectionStepNdt?: number | null;
  rejectionStepChem?: number | null;
  staff?: string;
  ndtSignatureLevels?: Record<string, Record<string, string>>;
  /** 第三方单项签批：key = experimentTypeId 字符串 */
  thirdPartyApprovalByExperimentType?: Record<string, Record<string, string>>;
}

/** 个人代办项（按姓名匹配角色与节点） */
export interface TodoItem {
  projectId: number;
  projectNumber: string;
  projectName: string;
  customer?: string;
  track: 'ndt' | 'chem';
  role: 'writer' | 'reviewer' | 'approver';
  step: number;
  stepLabel: string;
}

export interface ProjectDetail extends ProjectList {
  /** 当前用户是否可回退审批（后端计算） */
  canRollbackApproval?: boolean;
  unitNumber?: string; // 机组编号
  summaryNotificationSignedRelPath?: string;
  summaryNotificationSignedOriginalName?: string;
  summaryThirdPartyFullRelPath?: string;
  summaryThirdPartyFullOriginalName?: string;
  reportFigures?: ImageAttachment[];
  selectedExperimentTypeIds?: number[];
  reports: ReportList[];
  /** 总检测日志按部件顺序（JSON） */
  aggregateDetectionLogOrder?: string | null;
}

export interface CreateProject {
  projectNumber: string;
  thirdPartyProjectNumber?: string;
  thirdPartyName?: string;
  projectName: string;
  projectType: string;
  customer?: string; // 客户方
  customerContact?: string; // 客户方人员
  powerPlantId?: number; // 电厂ID
  unitId?: number; // 机组ID
  startDate: string;
  endDate?: string;
  description?: string;
  selectedExperimentTypeIds?: number[];
  responsiblePerson?: string;
  // 无损检测相关字段
  reviewerNdt?: string;
  reviewDateNdt?: string;
  approverNdt?: string;
  approvalDateNdt?: string;
  writerNdt?: string;
  writerDateNdt?: string;
  ndtSignatureLevels?: Record<string, Record<string, string>>;
  // 理化检测相关字段
  reviewerChem?: string;
  reviewDateChem?: string;
  approverChem?: string;
  approvalDateChem?: string;
  writerChem?: string;
  writerDateChem?: string;
  staff?: string;
  thirdPartyApprovalByExperimentType?: Record<string, Record<string, string>>;
}

export interface UpdateProject extends CreateProject {
  status?: string;
}

// Report related types

export interface ReportList {
  id: number;
  projectId: number;
  projectNumber?: string;
  projectName?: string;
  title: string;
  reportNumber: string;
  inspector?: string;
  testMethod?: string;
  equipment?: string;
  testStandard?: string;
  componentName?: string;
  equipmentCategory?: string;
  equipmentName?: string;
  componentSpec?: string;
  instrumentModel?: string;
  instrumentNumber?: string;
  projectComponentId?: number;
  /** 多选部件 ID 顺序；单部件时通常为空，由 projectComponentId 表示 */
  projectComponentIds?: number[];
  projectInstrumentId?: number;
  /** 报告级检测类型（总日志默认排序用） */
  experimentTypeId?: number;
  experimentTypeName?: string;
  testDate: string;
  location: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  userFullName?: string;
  itemCount: number;
  reportImage?: string;
  hasDefect?: string;
  summary?: string;
  imageAttachments?: ImageAttachment[];
  customFields?: Record<string, any>;
  reportItems?: ReportItem[];
  detectionContent?: DetectionContentPayload | null;
  /** 与单项 Word「检测内容」一致的整段叙述；无则 DetectionLog 回退本地 format */
  detectionContentNarrative?: string | null;
}

export interface ReportDetail extends ReportList {
  reportItems: ReportItem[];
}

export interface CreateReport {
  projectId: number;
  experimentTypeId?: number;
  projectComponentId?: number | null;
  projectComponentIds?: number[] | null;
  projectInstrumentId?: number | null;
  title: string;
  inspector?: string;
  testMethod?: string;
  equipment?: string;
  testStandard?: string;
  componentName?: string;
  equipmentCategory?: string;
  equipmentName?: string;
  componentSpec?: string;
  instrumentModel?: string;
  instrumentNumber?: string;
  testDate: string;
  location: string;
  reportImage?: string;
  hasDefect?: string;
  imageAttachments?: ImageAttachment[];
  customFields?: Record<string, any>;
  reportItems?: CreateReportItem[];
  detectionContent?: DetectionContentPayload | null;
}

export interface UpdateReport extends CreateReport {
  status?: string;
}

export interface ReportItem {
  id: number;
  experimentTypeId: number;
  experimentTypeName: string;
  experimentTypeCode: string;
  tableData: string;
  summary?: string;
}

export interface CreateReportItem {
  experimentTypeId: number;
  tableData: string;
  summary?: string;
}

// Experiment type & schema

export interface ExperimentType {
  id: number;
  name: string;
  code: string;
  tableSchema: string;
  reportFieldsSchema: string;
  isActive: boolean;
}

export interface ReportFieldsSchema {
  fields: ReportField[];
}

export interface ReportField {
  name: string;
  label: string;
  type: 'text' | 'number' | 'date' | 'select' | 'image' | 'radio' | 'button';
  autoGenerate?: boolean;
  required?: boolean;
  options?: string[];
  defaultValue?: any;
}

export interface TableSchema {
  columns: TableColumn[];
}

export interface TableColumn {
  key: string;
  label: string;
  type: 'text' | 'number' | 'select';
  name?: string;
  required?: boolean;
  options?: string[];
  width?: number;
}

export interface TableData {
  rows: TableRowData[];
}

/** 表格单元格持久化为字符串；imageIds 为图片附件列 */
export interface TableRowData {
  [key: string]: string | number[] | undefined;
  imageIds?: number[];
}

// Detection data types

export interface DetectionDataRow {
  [key: string]: string;
}

/** 与检测内容 table 行一一对应；存在时各块为权威数据源，rows 为合并缓存（与后端 tableData 一致） */
export interface DetectionDataBlockPayload {
  rows: DetectionDataRow[];
}

export interface DetectionData {
  rows: DetectionDataRow[];
  perContentRow?: DetectionDataBlockPayload[];
}

export type DetectionContentMode = 'table' | 'dual-textarea' | 'textarea' | 'single' | 'visual-groups' | 'sod';

export interface DetectionContentTableRow {
  type?: string;
  locationDesc?: string;
  /** 检测方式（与后端 JSON 键 method 一致） */
  method?: string;
  result?: string;
  locationNumber?: string;
  total?: string;
  /** 超声波测厚：与本行 perContentRow 检测数据块对应的最小需要厚度（mm），字符串存储 */
  minRequiredThickness?: string;
  /** 多选部件且检测内容行数超过部件数时：该行绑定的部件 ID（须在报告 projectComponentIds 内） */
  projectComponentId?: number;
}

export interface DetectionContentTablePayload {
  mode: 'table';
  /** @deprecated 优先使用 rows[i].minRequiredThickness；仅作旧数据读取兼容 */
  minRequiredThickness?: string;
  rows: DetectionContentTableRow[];
}

export interface DetectionContentDualTextareaPayload {
  mode: 'dual-textarea';
  position: string;
  conclusion: string;
  /** 金相检测：浸蚀剂（预设或自定义文本） */
  etchant?: string;
}

export interface DetectionContentTextareaPayload {
  mode: 'textarea';
  conclusion: string;
}

export interface DetectionContentSingleFieldPayload {
  mode: 'single';
  value: string;
}

export interface DetectionContentVisualGroupItem {
  resultDesc: string;
  imageIds: number[];
}

export interface DetectionContentVisualGroup {
  locationDesc: string;
  items: DetectionContentVisualGroupItem[];
}

export interface DetectionContentVisualGroupsPayload {
  mode: 'visual-groups';
  numberingRule: string;
  groups: DetectionContentVisualGroup[];
}

export interface DetectionContentSodPayload {
  mode: 'sod';
  probeSpec: string;
  tubeSample: string;
  sensitivityCalibration: string;
  rows: DetectionContentTableRow[];
}

export type DetectionContentPayload =
  | DetectionContentTablePayload
  | DetectionContentDualTextareaPayload
  | DetectionContentTextareaPayload
  | DetectionContentSingleFieldPayload
  | DetectionContentVisualGroupsPayload
  | DetectionContentSodPayload;

// Image attachment & gallery

export interface ImageAttachment {
  id?: number;
  imageUrls: string[];
  description: string;
  displayOrder?: number;
}

export interface ImageDTO {
  id: number;
  fileName: string;
  storagePath: string;
  fileSize: number;
  mimeType: string;
  userId: string;
  uploadedAt: string;
}

export interface ImageUploadResponse {
  id: number;
  fileName: string;
  url: string;
  fileSize: number;
  mimeType: string;
  uploadedAt: string;
}

// Auth types

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  userId: string;
  username: string;
  fullName?: string;
  department?: string;
  email?: string;
  role?: string; // 用户角色：ADMIN、USER 或 SUB_USER
  parentUserId?: string; // 子账号的主账号 ID
}

// User types
export type UserRole = 'ADMIN' | 'USER' | 'SUB_USER';

export interface User {
  id: string;
  username: string;
  fullName?: string;
  email?: string;
  role?: UserRole;
  department?: string;
  createdAt?: string;
}

// User management types
export interface UserList {
  id: string;
  username: string;
  fullName?: string;
  email?: string;
  role: UserRole;
  department?: string;
  createdAt: string;
  parentUserId?: string;
  parentFullName?: string;
}

export interface CreateUserRequest {
  username: string;
  password: string;
  email: string;
  fullName: string;
  department?: string;
  role: UserRole;
  parentUserId?: string; // 创建子账号时必填
}

export interface UpdateUserRequest {
  fullName: string;
  email: string;
  department?: string;
  role: UserRole;
}

export interface UserStats {
  adminCount: number;
  userCount: number;
  subUserCount: number;
  totalCount: number;
}

export interface RegisterRequest {
  username: string;
  password: string;
  email: string;
  fullName: string;
  department?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

// Statistics types

export interface OverviewStatistics {
  totalReports: number;
  completedReports: number;
  draftReports: number;
  completionRate: number;
  overallQualityRate: number;
  totalExperimentItems: number;
}

export interface ExperimentDistribution {
  experimentTypeId: number;
  experimentTypeName: string;
  experimentTypeCode: string;
  count: number;
}

export interface ReportTrend {
  date: string;
  count: number;
}

export interface QualityStatistics {
  experimentTypeId: number;
  experimentTypeName: string;
  experimentTypeCode: string;
  qualifiedCount: number;
  unqualifiedCount: number;
  qualityRate: number;
}

export interface ReportTrend {
  date: string;
  count: number;
}

export interface QualityStatistics {
  experimentTypeId: number;
  experimentTypeName: string;
  experimentTypeCode: string;
  qualifiedCount: number;
  unqualifiedCount: number;
  qualityRate: number;
}

export interface ReportTrend {
  date: string;
  count: number;
}

export interface QualityStatistics {
  experimentTypeId: number;
  experimentTypeName: string;
  experimentTypeCode: string;
  qualifiedCount: number;
  unqualifiedCount: number;
  qualityRate: number;
}

export interface ReportTrend {
  date: string;
  count: number;
}

export interface QualityStatistics {
  experimentTypeId: number;
  experimentTypeName: string;
  experimentTypeCode: string;
  qualifiedCount: number;
  unqualifiedCount: number;
  qualityRate: number;
}

export interface ReportTrend {
  date: string;
  count: number;
}

export interface QualityStatistics {
  experimentTypeId: number;
  experimentTypeName: string;
  experimentTypeCode: string;
  qualifiedCount: number;
  unqualifiedCount: number;
  qualityRate: number;
}
