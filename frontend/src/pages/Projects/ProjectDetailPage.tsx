import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  Card,
  Descriptions,
  Button,
  Space,
  Typography,
  Tag,
  Table,
  message,
  Popconfirm,
  Row,
  Col,
  Statistic,
  Select,
  Modal,
  Dropdown,
  Tooltip,
  Drawer,
  DatePicker,
  Form,
  Divider,
  List,
  Upload,
  Collapse,
  Input,
  Tabs,
  Segmented,
} from 'antd';
import {
  EditOutlined,
  FileWordOutlined,
  PlusOutlined,
  DeleteOutlined,
  ArrowLeftOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
  BarChartOutlined,
  SaveOutlined,
  SendOutlined,
  CheckCircleOutlined as PassOutlined,
  CloseCircleOutlined as RejectOutlined,
  ArrowRightOutlined,
  SettingOutlined,
  HistoryOutlined,
  RollbackOutlined,
  UploadOutlined,
  DownOutlined,
  UpOutlined,
  SortAscendingOutlined,
  FilterOutlined,
  PictureOutlined,
} from '@ant-design/icons';
import './ProjectDetailPage.css';
import { useNavigate, useParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import { isSubUser } from '@/utils/auth';
import { projectService, type ApprovalLogEntry, type WordExportJob } from '@/services/projectService';
import { RUNDIAN_PERSONNEL_NAMES } from '@/constants/rundianPersonnel';
import { reportService } from '@/services/reportService';
import { experimentTypeService } from '@/services/experimentTypeService';
import { componentService, ProjectComponent } from '../../services/componentService';
import { getComponentDisplaySpec } from '@/utils/projectComponentDisplaySpec';
import { buildComponentSummaryLine } from '@/utils/componentSummaryLine';
import {
  buildRowComponentSummary,
  resolveDetectionContentRowComponentId,
  sanitizeTableRowComponentIds,
} from '@/utils/detectionContentRowComponent';
import {
  normalizeTableDetectionContentRows,
  emptyTableDetectionContentRow,
  mergeSentTableRowMinThickness,
} from '@/utils/detectionContentTable';
import {
  sortProjectComponents,
  buildComponentSortRankMap,
  sortReportsByComponentOrder,
} from '@/utils/sortProjectComponents';
import {
  formatComponentLabel,
  formatMultiComponentDisplay,
  resolveProjectComponentsByIds,
} from '@/utils/reportComponentDisplay';
import { instrumentService, ProjectInstrument } from '../../services/instrumentService';
import type {
  ReportList,
  ReportDetail,
  CreateReport,
  ExperimentType,
  ReportFieldsSchema,
  ImageAttachment,
  ProjectDetail,
  DetectionContentPayload,
  DetectionContentTablePayload,
  DetectionContentTableRow,
  UpdateProject,
  ExportTextPreview,
  ProjectOverviewPreview,
} from '@/types';
import DynamicDetectionDataTable from '@/components/DynamicDetectionDataTable/DynamicDetectionDataTable';
import ImageAttachmentsTable from '@/components/ImageAttachmentsTable/ImageAttachmentsTable';
import DetectionContentEditor, { DetectionContentConfig } from '@/components/DetectionContentEditor/DetectionContentEditor';
import ProjectComponentsTable from '@/components/ProjectComponentsTable/ProjectComponentsTable';
import ProjectInstrumentsTable from '@/components/ProjectInstrumentsTable/ProjectInstrumentsTable';
import DetectionLog from '@/components/DetectionLog/DetectionLog';
import ProjectReportChangeLogPanel from '@/components/ProjectReportChangeLog/ProjectReportChangeLogPanel';
import ReportOverviewOrderModal from '@/components/ReportOverviewOrderModal/ReportOverviewOrderModal';
import OverviewPreviewModal from '@/components/OverviewPreviewModal/OverviewPreviewModal';
import LeebHardnessCategorySaveModal from '@/components/LeebHardnessCategorySaveModal/LeebHardnessCategorySaveModal';
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary';
import PersonnelSelect from '@/components/PersonnelSelect/PersonnelSelect';
import dayjs from 'dayjs';
import {
  validateSummaryNotificationFile,
  validateThirdPartyFullFile,
} from '@/utils/summaryAttachmentUpload';
import { waitForWordExportJob } from '@/utils/wordExportJob';
import {
  extractPerContentRowBlocks,
  alignPerContentRowToContentRowCount,
  canonicalizeTableDataPayload,
  type ParsedTableDataShape,
  buildTableDataPayload,
  mergeBlockIntoTableData,
  hasNonEmptyDetectionBlocks,
  isDetectionTableTrailingSlashPlaceholderRow,
} from '@/utils/detectionTableData';
import { compressLocationNumbers } from '@/utils/locationNumberRange';
import { parseStaff, parseInspectorNames } from '@/utils/parseStaff';

const { Title } = Typography;
const { Option } = Select;

interface ProjectStats {
  totalReports: number;
  defectCount: number;
  workDays: number;
  detectionTypeTotal: number;
}

interface ActiveExperimentType {
  id: number;
  name: string;
  experimentType: ExperimentType;
}

interface ReportRow {
  id?: number;
  experimentTypeId: number;
  projectComponentId?: number; // 关联检测部件ID（多选时为第一个）
  projectComponentIds?: number[]; // 多选部件顺序
  projectInstrumentId?: number; // 关联仪器设备ID
  /** 报告编号（搜索用；表格可不展示） */
  reportNumber?: string;
  title: string;
  componentName?: string; // 部件名称（新增）
  equipment?: string; // 使用仪器/设备（新增）
  equipmentCategory?: string;
  equipmentName?: string;
  componentSpec?: string;
  instrumentModel?: string;
  instrumentNumber?: string; // 仪器编号（新增）
  testStandard?: string; // 检测标准（新增）
  inspector?: string;
  location?: string;
  testDate?: string;
  status?: string;
  /** 后端创建时间 ISO，用于「仅看今日新建」过滤 */
  createdAt?: string;
  isNew?: boolean;
  isEditing?: boolean;
  expanded?: boolean;
  detectionData?: any;
  reportImage?: string;  // 保留旧字段（向后兼容）
  imageAttachments?: ImageAttachment[];  // 新增：附图列表
  imageAttachmentsExpanded?: boolean;  // 控制展开/收起
  detectionContent?: DetectionContentPayload | null;
  detectionContentExpanded?: boolean;
  hasDefect?: string;
  summary?: string;
  customFields?: Record<string, any>; // 自定义字段
  nonComplianceRecords?: Array<{
    number?: string;
    itemName?: string;
    standardValue?: string;
    actualValue?: string;
    result?: string;
  }>;
  saving?: boolean; // 保存状态
  validationErrors?: Set<string>; // 新增：记录哪些字段为空
}

type LeebCategoryMapping = { blockIndex: number; rowIndex: number };

type ReportRowWithKey = ReportRow & { key?: number };

function buildReportRowSearchHaystack(
  row: ReportRow,
  components?: ProjectComponent[],
  instruments?: ProjectInstrument[],
): string {
  const parts: string[] = [];
  const push = (v: unknown) => {
    if (v == null || v === '') return;
    parts.push(String(v));
  };
  push(row.reportNumber);
  push(row.title);
  push(row.componentName);
  /** 与表格「部件名称」列一致：按 ID 解析（兼容 ID 为 string；并拼接单列标签便于搜材质/规格） */
  if (components && components.length > 0) {
    const comps = resolveProjectComponentsByIds(
      row.projectComponentIds,
      row.projectComponentId,
      components,
    );
    for (const c of comps) {
      push(c.componentName);
      push(formatComponentLabel(c));
    }
    const compDisp = formatMultiComponentDisplay(
      row.projectComponentIds,
      row.projectComponentId,
      components,
    );
    if (compDisp && compDisp !== '-') push(compDisp);
  }
  push(row.equipment);
  push(row.equipmentCategory);
  push(row.equipmentName);
  push(row.componentSpec);
  push(row.instrumentModel);
  push(row.instrumentNumber);
  /** 与「仪器设备」列只读展示一致：按关联 ID 解析仪器名称/型号/编号 */
  if (instruments && instruments.length > 0 && row.projectInstrumentId != null) {
    const iid = Number(row.projectInstrumentId);
    const inst = Number.isFinite(iid)
      ? instruments.find((i) => Number(i.id) === iid)
      : undefined;
    if (inst) {
      push(inst.instrumentName);
      push(inst.instrumentModel);
      push(inst.instrumentNumber);
    }
  }
  push(row.testStandard);
  push(row.inspector);
  push(row.location);
  push(row.testDate);
  push(row.status);
  push(row.hasDefect);
  push(row.summary);
  if (row.customFields && typeof row.customFields === 'object') {
    for (const v of Object.values(row.customFields)) {
      if (v == null) continue;
      if (typeof v === 'object' && !Array.isArray(v)) {
        parts.push(JSON.stringify(v));
      } else {
        parts.push(String(v));
      }
    }
  }
  return parts.join('\u0001').toLowerCase();
}

/** 「仅看今日新建」：未落库的新行始终显示；已保存行按 createdAt 是否为本机自然日。 */
function reportRowMatchesTodayCreatedFilter(row: ReportRow): boolean {
  if (row.isNew && !row.id) {
    return true;
  }
  const ca = row.createdAt;
  if (!ca || !String(ca).trim()) {
    return false;
  }
  const d = dayjs(ca);
  return d.isValid() && d.isSame(dayjs(), 'day');
}

function reportRowMatchesSearch(
  row: ReportRow,
  queryLower: string,
  components?: ProjectComponent[],
  instruments?: ProjectInstrument[],
): boolean {
  if (!queryLower) return true;
  return buildReportRowSearchHaystack(row, components, instruments).includes(queryLower);
}

function reportStoreIndex(record: ReportRowWithKey, fallbackTableIndex: number): number {
  const k = record.key;
  return typeof k === 'number' && Number.isFinite(k) ? k : fallbackTableIndex;
}

/** 项目信息 / 报告列表等主要操作按钮统一阴影（位置与 size 不变，仅增强层次） */
const PROMINENT_ACTION_BTN_SHADOW: React.CSSProperties = {
  boxShadow: '0 2px 8px rgba(15, 23, 42, 0.14), 0 1px 3px rgba(15, 23, 42, 0.08)',
};

type DetailSectionTabKey = 'detectionLog' | 'components' | 'instruments' | 'reports' | 'approval' | 'reportChanges';

function extractNonComplianceRecords(customFields: Record<string, any> | undefined) {
  if (!customFields || typeof customFields !== 'object') return [];
  const candidates = [
    customFields.nonComplianceRecords,
    customFields.non_compliance_records,
    customFields.nonCompliance,
    customFields.compareErrors,
    customFields.comparisonErrors,
  ];
  const hit = candidates.find((item) => Array.isArray(item));
  if (!Array.isArray(hit)) return [];
  return hit
    .map((item: any) => ({
      number: item?.number != null ? String(item.number) : undefined,
      itemName: item?.itemName != null ? String(item.itemName) : undefined,
      standardValue: item?.standardValue != null ? String(item.standardValue) : undefined,
      actualValue: item?.actualValue != null ? String(item.actualValue) : undefined,
      result: item?.result != null ? String(item.result) : undefined,
    }))
    .filter((item) => item.itemName);
}

const CUSTOM_TYPE_OPTION_LABEL = '自定义';

const defaultDetectionContentConfig: DetectionContentConfig = {
  mode: 'table',
  typeOptions: [CUSTOM_TYPE_OPTION_LABEL],
};

/** table 模式保证 typeOptions 含「自定义」，便于编辑器下拉 + 自定义输入 */
function ensureTableTypeOptionsIncludeCustom(config: DetectionContentConfig): DetectionContentConfig {
  if (config.mode !== 'table') {
    return config;
  }
  const base =
    config.typeOptions && config.typeOptions.length > 0
      ? [...config.typeOptions]
      : [...(defaultDetectionContentConfig.typeOptions || [CUSTOM_TYPE_OPTION_LABEL])];
  if (!base.includes(CUSTOM_TYPE_OPTION_LABEL)) {
    base.push(CUSTOM_TYPE_OPTION_LABEL);
  }
  return { ...config, typeOptions: base };
}

const detectionContentConfigMap: Record<string, DetectionContentConfig> = {
  '超声检测': {
    mode: 'table',
    typeOptions: ['对接焊缝', '弯头', '弯管', '绝缘子', '螺栓', '叶片（表面波）', '轴瓦', '轴颈', '推力瓦', '密封瓦', '角焊缝', '自定义'],
  },
  '渗透检测': {
    mode: 'table',
    typeOptions: ['对接焊缝', '弯头', '弯管', '轴瓦', '推力瓦', '密封瓦', '角焊缝', '自定义'],
  },
  '磁粉检测': {
    mode: 'table',
    typeOptions: ['对接焊缝', '弯头', '弯管', '角焊缝', '热工仪表管-角焊缝对接焊缝', '受热面联箱-角焊缝对接焊缝', '自定义'],
  },
  '射线检测': {
    mode: 'table',
    typeOptions: ['受热面管'],
  },
  '内窥镜检测': {
    mode: 'table',
    typeOptions: ['联箱', '换热器管', '自定义'],
    tableOptions: {
      showType: true,
      showLocationDesc: true,
      showDetectionMethod: true,
      methodLabel: '检测方式',
      showResult: true,
      showLocationNumber: false,
      showTotal: false,
      locationDescLabel: '检测位置',
      resultLabel: '检测结果',
      typeColumnWidth: 150,
    },
  },
  '涡流检测': {
    mode: 'table',
    typeOptions: ['远场涡流', '常规涡流'],
    tableOptions: {
      showLocationDesc: true,
      showResult: true,
      locationDescLabel: '检测位置',
      resultLabel: '检测结果',
      showLocationNumber: false,
      showTotal: false,
    },
  },
  '超声波测厚': {
    mode: 'table',
    typeOptions: ['对接焊缝', '直管段', '弯头', '弯管', '自定义'],
    readOnlyFields: ['locationNumber', 'total'],
    requireMinRequiredThickness: true,
  },
  '管径测量': {
    mode: 'table',
    typeOptions: ['低合金钢管', '碳素钢管', 'T91/T122类管', '奥氏体耐热钢管'],
    readOnlyFields: ['locationNumber', 'total'],
  },
  '氧化皮堆积检测': {
    mode: 'sod',
  },
  '里氏硬度检测': {
    mode: 'table',
    typeOptions: ['管件/对接焊缝', '螺栓（检测部位：端部）', '螺栓（检测部位：腰部）', '螺帽', '大轴', '自定义'],
    readOnlyFields: ['locationNumber', 'total'],
  },
  '布什硬度检测': {
    mode: 'table',
    typeOptions: ['管件/对接焊缝', '螺栓', '螺帽', '大轴', '自定义'],
    readOnlyFields: ['locationNumber', 'total'],
  },
  '合金分析检测': {
    mode: 'table',
    typeOptions: ['焊缝', '弯头', '弯管', '母管', '自定义'],
    readOnlyFields: ['locationNumber', 'total'],
  },
  '相控阵超声波检测': {
    mode: 'table',
    typeOptions: ['叶片', '叶根-叉形', '叶根-菌形', '叶根-T形', '叶根-枞树形', '角焊缝（联箱）', '对接焊缝（受热面管）', '穿顶棚套管焊缝（受热面管）', '对接焊缝（插管）', '角焊缝（插管）'],
  },
  '圆度测量': {
    mode: 'table',
    typeOptions: ['弯头', '弯管'],
    readOnlyFields: ['locationNumber', 'total'],
  },
  '维氏硬度检测': {
    mode: 'table',
    typeOptions: ['对接焊缝', '弯头', '弯管', '螺栓', '螺帽', '大轴', '母管', '自定义'],
    readOnlyFields: ['locationNumber', 'total'],
  },
  '洛氏硬度检测': {
    mode: 'table',
    typeOptions: ['对接焊缝', '弯头', '弯管', '螺栓', '螺帽', '大轴', '母管', '自定义'],
    readOnlyFields: ['locationNumber', 'total'],
  },
  '金相检测': {
    mode: 'dual-textarea',
    labels: { position: '检测部位', conclusion: '检测结论' },
    placeholders: { position: '请输入检测部位', conclusion: '请输入检测结论' },
    extraSelect: {
      key: 'etchant',
      label: '浸蚀剂',
      options: ['4%硝酸酒精溶液', '三氯化铁盐酸水溶液'],
      allowCustom: true,
    },
  },
  '目视检测': {
    mode: 'visual-groups',
  },
  '冲击吸收能量检测': {
    mode: 'dual-textarea',
    labels: { position: '检测部位', conclusion: '检测结论' },
    placeholders: { position: '请输入检测部位', conclusion: '请输入检测结论' },
  },
  '室温拉伸检测': {
    mode: 'textarea',
    labels: { conclusion: '检测结论' },
    placeholders: { conclusion: '请输入检测结论' },
  },
  '高温拉伸检测': {
    mode: 'textarea',
    labels: { conclusion: '检测结论' },
    placeholders: { conclusion: '请输入检测结论' },
  },
  '高温持久强度检测': {
    mode: 'textarea',
    labels: { conclusion: '检测结论' },
    placeholders: { conclusion: '请输入检测结论' },
  },
  '有效硬化层深度检测': {
    mode: 'single',
    labels: { single: '有效硬化层深度' },
    placeholders: { single: '请输入有效硬化层深度' },
  },
};

const getDetectionContentConfigByName = (typeName?: string): DetectionContentConfig =>
  ensureTableTypeOptionsIncludeCustom(
    (typeName ? detectionContentConfigMap[typeName] : null) || defaultDetectionContentConfig,
  );

/** 部分检测类型：检测内容区域上方显示「类型、位置编号、总计」只读摘要（与后端自动填充一致） */
const AUTO_FILL_READONLY_TYPE_NAMES: string[] = [
  '冲击吸收能量检测',
  '室温拉伸检测',
  '高温拉伸检测',
  '高温持久强度检测',
  '有效硬化层深度检测',
];

/** 是否存在缺陷由用户手动选择（与后端 DefectDetectionService 返回 null 的类型一致） */
const MANUAL_DEFECT_TYPE_CODES = new Set(['SOD', 'MET', 'VIS', 'VT']);

/** 导出文案预览：列表当前行的 hasDefect 覆盖 API 的 showDefectSection（未保存时 API 可能仍为旧值） */
function mergeExportPreviewShowDefectFromLocalRow(
  preview: ExportTextPreview,
  _experimentTypeCode: string,
  localHasDefect: string | undefined,
): ExportTextPreview {
  if (localHasDefect === '是' || localHasDefect === '否') {
    return { ...preview, showDefectSection: localHasDefect === '是' };
  }
  return preview;
}

const LOCATION_NUMBER_COLUMN_BY_CODE: Record<string, string> = {
  SOD: '编号',
  BHT: '编号',
  LHT: '编号',
  BHD: '编号',
  LHD: '编号',
  RDM: '弯头编号',
  VHT: '编号',
  RHT: '编号',
  VHN: '编号',
  RHN: '编号',
  PDM: '测点编号',
  HTC: '编号',
  CHD: '至边缘距离',
  HTN: '编号',
  IMP: '编号',
  RTN: '编号',
  PMI: '编号',
  AAT: '编号',
  UTM: '测点编号',
  UTT: '测点编号',
};

const DEFAULT_LOCATION_NUMBER_KEYS = ['编号', '序号', '缺陷编号', '测点编号', '位置编号'] as const;
const RECORD_ONLY_DEFECT_KEY = '是否为记录缺陷';

function pickLocationNumberValue(
  row: Record<string, unknown>,
  preferredKey: string,
): string {
  const keys = [preferredKey, ...DEFAULT_LOCATION_NUMBER_KEYS].filter(Boolean);
  for (const key of keys) {
    const val = row[key];
    if (val != null && String(val).trim() !== '') {
      return String(val).trim();
    }
  }
  return '';
}

function getDetectionDataRowsForContentBlock(
  detectionData: { rows?: Record<string, unknown>[]; perContentRow?: { rows: Record<string, unknown>[] }[] } | undefined,
  blockIndex: number,
): Record<string, unknown>[] {
  const blocks = extractPerContentRowBlocks(detectionData as { rows?: Record<string, unknown>[]; perContentRow?: { rows: Record<string, unknown>[] }[] });
  const b = blocks[blockIndex];
  return b?.rows ?? [];
}

function detectionContentTableRowCount(detectionContent: DetectionContentPayload | null | undefined): number {
  if (
    detectionContent &&
    (detectionContent.mode === 'table' || detectionContent.mode === 'sod') &&
    Array.isArray((detectionContent as { rows?: unknown[] }).rows)
  ) {
    return Math.max(1, (detectionContent as { rows: unknown[] }).rows.length);
  }
  return 1;
}

function alignDetectionDataToContent(
  detectionData: unknown,
  detectionContent: DetectionContentPayload,
): ParsedTableDataShape {
  return canonicalizeTableDataPayload(detectionData as ParsedTableDataShape, detectionContentTableRowCount(detectionContent));
}

function detectionDataFromReportItems(
  reportItems: { tableData?: string }[] | undefined,
  detectionContent: DetectionContentPayload,
): ParsedTableDataShape | undefined {
  const tableData = reportItems?.[0]?.tableData;
  if (!tableData) return undefined;
  try {
    return alignDetectionDataToContent(JSON.parse(tableData), detectionContent);
  } catch {
    return undefined;
  }
}

function computeAutoFillFromTableData(
  typeName: string,
  typeCode: string,
  detectionData: { rows?: Record<string, unknown>[]; perContentRow?: { rows: Record<string, unknown>[] }[] } | undefined,
  contentBlockIndex?: number,
): { type: string; locationNumber: string; total: string } {
  const colKey = typeCode ? LOCATION_NUMBER_COLUMN_BY_CODE[String(typeCode).toUpperCase()] : '编号';
  const key = colKey || '编号';
  const rows =
    contentBlockIndex != null
      ? getDetectionDataRowsForContentBlock(detectionData, contentBlockIndex)
      : detectionData?.rows ?? [];
  const locationNumbers: string[] = [];
  const dataRows = rows.filter((r) => !isDetectionTableTrailingSlashPlaceholderRow(r as Record<string, unknown>));
  for (const row of dataRows) {
    const value = pickLocationNumberValue(row, key);
    if (value) {
      locationNumbers.push(value);
    }
  }
  return {
    type: typeName || '',
    locationNumber: compressLocationNumbers(locationNumbers),
    total: String(dataRows.length),
  };
}

const ELBOW_TYPE_ALIASES = new Set(['弯头/弯管', '弯头弯管']);
const ELBOW_TYPE_CANONICAL = '弯头';

/** 旧数据仅存字面量「自定义」时清空，进入自定义输入路径；弯头/弯管等别名规范为「弯头」 */
function normalizeDetectionContentTableRowsLegacyCustom(rows: unknown): unknown[] {
  if (!Array.isArray(rows)) return [];
  return rows.map((row: any) => {
    if (!row || typeof row !== 'object') return row;
    let next = row;
    const t = row.type;
    if (typeof t === 'string') {
      const trimmed = t.trim();
      if (trimmed === CUSTOM_TYPE_OPTION_LABEL) {
        next = { ...next, type: '' };
      } else if (ELBOW_TYPE_ALIASES.has(trimmed)) {
        next = { ...next, type: ELBOW_TYPE_CANONICAL };
      }
    }
    return next;
  });
}

/** 将 table 检测内容扩展/截断为 targetCount 行；新增行继承首行模板，清空位置编号与总计等（多部件拆分时与部件数对齐） */
function resizeDetectionContentTableRows(rows: unknown[], targetCount: number): DetectionContentTableRow[] {
  const n = Math.max(1, targetCount);
  const normalized = normalizeTableDetectionContentRows(
    Array.isArray(rows) && rows.length > 0 ? rows : [emptyTableDetectionContentRow()],
  );
  const next = normalized.slice(0, n);
  while (next.length < n) {
    next.push(emptyTableDetectionContentRow());
  }
  return next;
}

const createDefaultDetectionContent = (config: DetectionContentConfig): DetectionContentPayload => {
  switch (config.mode) {
    case 'table':
      return {
        mode: 'table',
        rows: [emptyTableDetectionContentRow()],
      };
    case 'dual-textarea':
      return { mode: 'dual-textarea', position: '', conclusion: '', etchant: '' };
    case 'textarea':
      return { mode: 'textarea', conclusion: '' };
    case 'single':
      return { mode: 'single', value: '' };
    case 'visual-groups':
      return { mode: 'visual-groups', numberingRule: '', groups: [] };
    case 'sod':
      return { mode: 'sod', probeSpec: '', tubeSample: '', sensitivityCalibration: '', rows: [] };
    default:
      return { mode: 'table', rows: [] };
  }
};

const normalizeDetectionContentValue = (
  value: DetectionContentPayload | null | undefined,
  config: DetectionContentConfig,
): DetectionContentPayload => {
  if (!value || typeof value !== 'object') {
    return createDefaultDetectionContent(config);
  }

  if ('mode' in value && value.mode === config.mode) {
    switch (config.mode) {
      case 'table': {
        const tableVal = value as DetectionContentTablePayload;
        const legacyTop =
          typeof tableVal.minRequiredThickness === 'string' ? tableVal.minRequiredThickness : '';
        const customRows = normalizeDetectionContentTableRowsLegacyCustom(
          Array.isArray((value as any).rows) ? (value as any).rows : [],
        );
        return {
          mode: 'table',
          rows: normalizeTableDetectionContentRows(customRows, legacyTop),
        };
      }
      case 'dual-textarea':
        return {
          mode: 'dual-textarea',
          position: (value as any).position || '',
          conclusion: (value as any).conclusion || '',
          etchant: typeof (value as any).etchant === 'string' ? (value as any).etchant : '',
        };
      case 'textarea':
        return {
          mode: 'textarea',
          conclusion: (value as any).conclusion || '',
        };
      case 'single':
        return {
          mode: 'single',
          value: (value as any).value || '',
        };
      case 'visual-groups':
        return {
          mode: 'visual-groups',
          numberingRule: (value as any).numberingRule || '',
          groups: Array.isArray((value as any).groups) ? (value as any).groups : [],
        };
      case 'sod':
        return {
          mode: 'sod',
          probeSpec: (value as any).probeSpec || '',
          tubeSample: (value as any).tubeSample || '',
          sensitivityCalibration: (value as any).sensitivityCalibration || '',
          rows: normalizeDetectionContentTableRowsLegacyCustom(
            Array.isArray((value as any).rows) ? (value as any).rows : [],
          ) as any[],
        };
      default:
        return createDefaultDetectionContent(config);
    }
  }

  // 5 种类型：后端返回 mode=table 但配置为 textarea/single/dual-textarea，保留 conclusion/position/value
  if ('mode' in value && (value as any).mode === 'table' && config.mode !== 'table') {
    const v = value as any;
    if (config.mode === 'sod') {
      return {
        mode: 'sod',
        probeSpec: v.probeSpec || '',
        tubeSample: v.tubeSample || '',
        sensitivityCalibration: v.sensitivityCalibration || '',
        rows: normalizeDetectionContentTableRowsLegacyCustom(Array.isArray(v.rows) ? v.rows : []) as any[],
      };
    }
    if (config.mode === 'textarea') {
      return { mode: 'textarea', conclusion: v.conclusion || '' };
    }
    if (config.mode === 'dual-textarea') {
      return {
        mode: 'dual-textarea',
        position: v.position || '',
        conclusion: v.conclusion || '',
        etchant: typeof v.etchant === 'string' ? v.etchant : '',
      };
    }
    if (config.mode === 'single') {
      return { mode: 'single', value: v.value || '' };
    }
  }

  // 兼容旧格式（无 mode 信息）
  if (config.mode === 'table') {
    if (Array.isArray((value as any).rows)) {
      return {
        mode: 'table',
        rows: normalizeDetectionContentTableRowsLegacyCustom((value as any).rows) as any[],
      };
    }
    if (Array.isArray(value)) {
      return {
        mode: 'table',
        rows: normalizeDetectionContentTableRowsLegacyCustom(value) as any[],
      };
    }
  }

  if (config.mode === 'sod') {
    if (Array.isArray((value as any).rows)) {
      return {
        mode: 'sod',
        probeSpec: (value as any).probeSpec || '',
        tubeSample: (value as any).tubeSample || '',
        sensitivityCalibration: (value as any).sensitivityCalibration || '',
        rows: normalizeDetectionContentTableRowsLegacyCustom((value as any).rows) as any[],
      };
    }
    if (Array.isArray(value)) {
      return {
        mode: 'sod',
        probeSpec: '',
        tubeSample: '',
        sensitivityCalibration: '',
        rows: normalizeDetectionContentTableRowsLegacyCustom(value) as any[],
      };
    }
  }

  if (config.mode === 'textarea' && typeof (value as any).conclusion === 'string') {
    return { mode: 'textarea', conclusion: (value as any).conclusion };
  }

  // 兼容金相检测从 textarea 模式迁移到 dual-textarea 模式
  // 情况1：旧数据是 textarea 模式（mode: 'textarea'），需要转换为 dual-textarea
  if (config.mode === 'dual-textarea' && (value as any).mode === 'textarea' && typeof (value as any).conclusion === 'string') {
    return {
      mode: 'dual-textarea',
      position: '',
      conclusion: (value as any).conclusion || '',
      etchant: typeof (value as any).etchant === 'string' ? (value as any).etchant : '',
    };
  }
  // 情况2：旧数据没有 mode，但有 conclusion 字段（可能是旧格式）
  if (config.mode === 'dual-textarea' && typeof (value as any).conclusion === 'string' && !(value as any).position && !(value as any).mode) {
    return {
      mode: 'dual-textarea',
      position: '',
      conclusion: (value as any).conclusion || '',
      etchant: typeof (value as any).etchant === 'string' ? (value as any).etchant : '',
    };
  }

  if (config.mode === 'dual-textarea') {
    return {
      mode: 'dual-textarea',
      position: (value as any).position || '',
      conclusion: (value as any).conclusion || '',
      etchant: typeof (value as any).etchant === 'string' ? (value as any).etchant : '',
    };
  }

  if (config.mode === 'single' && typeof (value as any).value === 'string') {
    return { mode: 'single', value: (value as any).value };
  }

  if (config.mode === 'visual-groups') {
    return {
      mode: 'visual-groups',
      numberingRule: (value as any).numberingRule || '',
      groups: Array.isArray((value as any).groups) ? (value as any).groups : [],
    };
  }

  return createDefaultDetectionContent(config);
};

const getDetectionContentConfigByTypeId = (
  typeId: number,
  activeTypes: ActiveExperimentType[],
): DetectionContentConfig => {
  const type = activeTypes.find(aet => aet.id === typeId);
  const typeName = type?.experimentType?.name || type?.name;
  return getDetectionContentConfigByName(typeName);
};

const STEP_WRITER = 0;
const STEP_PENDING_REVIEW = 1;
const STEP_PENDING_APPROVAL = 2;
const STEP_APPROVED = 3;

function getNodeBoxStyle(
  step: number,
  nodeIndex: number,
  _rejectionStep: number | null | undefined
): React.CSSProperties {
  const isCurrent = step === nodeIndex;
  const isPassed = step > nodeIndex;
  const border = '1px solid #e8e8e8';
  let background = '#fafafa';
  if (isCurrent) {
    background = '#CD853F';
  } else if (isPassed) {
    background = '#2E8B57';
  }
  return {
    padding: '8px 12px',
    minWidth: 120,
    borderRadius: 30,
    border,
    background,
    textAlign: 'center' as const,
  };
}

function getNodeTextColor(
  step: number,
  nodeIndex: number,
  rejectionStep: number | null | undefined
): string {
  const isRejected = rejectionStep != null && rejectionStep === nodeIndex && step <= nodeIndex;
  const isCurrent = step === nodeIndex;
  const isPassed = step > nodeIndex;
  const isDefault = !isRejected && !isCurrent && !isPassed;
  return (isDefault || isRejected) ? '#000' : '#fff';
}

interface ApprovalFlowCardProps {
  project: ProjectDetail;
  fullName: string;
  isProjectTypeMissing: boolean;
  canRollbackApproval: boolean;
  submitApprovalMutation: { mutate: (p: { track: 'ndt' | 'chem' | 'both' }) => void; isPending: boolean };
  approvalPassMutation: { mutate: (t: 'ndt' | 'chem') => void; isPending: boolean };
  approvalRejectMutation: { mutate: (t: 'ndt' | 'chem') => void; isPending: boolean };
  approvalRollbackMutation: {
    mutate: (t: 'ndt' | 'chem') => void;
    mutateAsync: (t: 'ndt' | 'chem') => Promise<unknown>;
    isPending: boolean;
  };
  onRefresh: () => void;
  projectId: number;
}

const trackHasApprovalData = (
  step: number,
  rejectionStep: number | null | undefined,
  writer?: string,
  reviewer?: string,
  approver?: string,
  writerDate?: string,
  reviewDate?: string,
  approvalDate?: string,
): boolean => {
  if (step > 0) return true;
  if (rejectionStep != null) return true;
  if ([writer, reviewer, approver].some((n) => n != null && n.trim() !== '')) return true;
  return [writerDate, reviewDate, approvalDate].some((d) => d != null && d !== '');
};

const ApprovalFlowCard: React.FC<ApprovalFlowCardProps> = ({
  project,
  fullName,
  isProjectTypeMissing,
  canRollbackApproval,
  submitApprovalMutation,
  approvalPassMutation,
  approvalRejectMutation,
  approvalRollbackMutation,
  onRefresh,
  projectId,
}) => {
  const [settingModalOpen, setSettingModalOpen] = useState(false);
  const [logModalOpen, setLogModalOpen] = useState(false);
  const [settingForm] = Form.useForm();

  const personnelOptions = useMemo(
    () =>
      RUNDIAN_PERSONNEL_NAMES.map((name) => ({ label: name, value: name })),
    [],
  );

  const { data: approvalLogs = [], isLoading: logsLoading } = useQuery({
    queryKey: ['approval-logs', projectId],
    queryFn: () => projectService.getApprovalLogs(projectId),
    enabled: logModalOpen,
  });

  const handleOpenSetting = () => {
    settingForm.setFieldsValue({
      writerNdt: project.writerNdt || undefined,
      reviewerNdt: project.reviewerNdt || undefined,
      approverNdt: project.approverNdt || undefined,
      writerDateNdt: project.writerDateNdt ? dayjs(project.writerDateNdt) : undefined,
      reviewDateNdt: project.reviewDateNdt ? dayjs(project.reviewDateNdt) : undefined,
      approvalDateNdt: project.approvalDateNdt ? dayjs(project.approvalDateNdt) : undefined,
      writerChem: project.writerChem || undefined,
      reviewerChem: project.reviewerChem || undefined,
      approverChem: project.approverChem || undefined,
      writerDateChem: project.writerDateChem ? dayjs(project.writerDateChem) : undefined,
      reviewDateChem: project.reviewDateChem ? dayjs(project.reviewDateChem) : undefined,
      approvalDateChem: project.approvalDateChem ? dayjs(project.approvalDateChem) : undefined,
    });
    setSettingModalOpen(true);
  };

  const handleSettingSubmit = async () => {
    if (isProjectTypeMissing) {
      message.warning('请先到“编辑项目”页面设置项目类型');
      return;
    }
    const values = await settingForm.validateFields();
    const ndtStep = project.approvalStepNdt ?? 0;
    const chemStep = project.approvalStepChem ?? 0;
    const payload: UpdateProjectPayload = {
      projectNumber: project.projectNumber,
      projectName: project.projectName,
      projectType: project.projectType?.trim() || '',
      customer: project.customer,
      customerContact: project.customerContact,
      powerPlantId: project.powerPlantId,
      unitId: project.unitId,
      startDate: project.startDate,
      endDate: project.endDate ?? undefined,
      description: project.description,
      responsiblePerson: project.responsiblePerson,
      staff: project.staff,
      status: project.status,
      selectedExperimentTypeIds: project.selectedExperimentTypeIds,
      writerNdt: ndtStep >= 1 ? project.writerNdt : values.writerNdt,
      reviewerNdt: ndtStep >= 2 ? project.reviewerNdt : values.reviewerNdt,
      approverNdt: ndtStep >= 3 ? project.approverNdt : values.approverNdt,
      writerDateNdt: ndtStep >= 1 ? project.writerDateNdt : values.writerDateNdt?.format?.('YYYY-MM-DD'),
      reviewDateNdt: ndtStep >= 2 ? project.reviewDateNdt : values.reviewDateNdt?.format?.('YYYY-MM-DD'),
      approvalDateNdt: ndtStep >= 3 ? project.approvalDateNdt : values.approvalDateNdt?.format?.('YYYY-MM-DD'),
      writerChem: chemStep >= 1 ? project.writerChem : values.writerChem,
      reviewerChem: chemStep >= 2 ? project.reviewerChem : values.reviewerChem,
      approverChem: chemStep >= 3 ? project.approverChem : values.approverChem,
      writerDateChem: chemStep >= 1 ? project.writerDateChem : values.writerDateChem?.format?.('YYYY-MM-DD'),
      reviewDateChem: chemStep >= 2 ? project.reviewDateChem : values.reviewDateChem?.format?.('YYYY-MM-DD'),
      approvalDateChem: chemStep >= 3 ? project.approvalDateChem : values.approvalDateChem?.format?.('YYYY-MM-DD'),
    };
    await projectService.update(projectId, payload);
    message.success('已保存');
    setSettingModalOpen(false);
    onRefresh();
  };

  const renderTrackRow = (
    track: 'ndt' | 'chem',
    label: string,
    step: number,
    rejectionStep: number | null | undefined,
    writer: string | undefined,
    reviewer: string | undefined,
    approver: string | undefined,
    writerDate: string | undefined,
    reviewDate: string | undefined,
    approvalDate: string | undefined
  ) => {
    const nodes = [
      { key: 0, role: '编写', name: writer, date: writerDate },
      { key: 1, role: '审核', name: reviewer, date: reviewDate },
      { key: 2, role: '批准', name: approver, date: approvalDate },
    ];
    const isCurrentNode = (nodeKey: number) => step === nodeKey;
    const principalNames = [
      fullName || '',
    ].filter((n, idx, arr) => n && arr.indexOf(n) === idx);
    const matchesPrincipal = (name?: string) => {
      const roleName = (name || '').trim();
      return roleName !== '' && principalNames.includes(roleName);
    };
    const isResponsible = matchesPrincipal(project.responsiblePerson);
    const showSubmit = track === 'ndt'
      ? matchesPrincipal(project.writerNdt) || isResponsible
      : matchesPrincipal(project.writerChem) || isResponsible;
    const showPassRejectReviewer = track === 'ndt'
      ? matchesPrincipal(project.reviewerNdt)
      : matchesPrincipal(project.reviewerChem);
    const showPassRejectApprover = track === 'ndt'
      ? matchesPrincipal(project.approverNdt)
      : matchesPrincipal(project.approverChem);

    return (
      <div style={{ marginBottom: 24 }}>
        <div style={{ marginBottom: 12, fontWeight: 500 }}>{label}</div>
        <div style={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 100 }}>
          {nodes.map((node, idx) => {
            const textColor = getNodeTextColor(step, node.key, rejectionStep);
            const isRejected = rejectionStep != null && rejectionStep === node.key && step <= node.key;
            return (
            <React.Fragment key={node.key}>
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                <div style={getNodeBoxStyle(step, node.key, rejectionStep)}>
                  <div style={{ fontSize: 12, color: textColor }}>{node.role}</div>
                  <div style={{ fontWeight: 500, color: textColor }}>{node.name || '未设置'}</div>
                  {node.date && (
                    <div style={{ fontSize: 12, color: textColor }}>{dayjs(node.date).format('YYYY-MM-DD')}</div>
                  )}
                  {step < STEP_APPROVED && isCurrentNode(node.key) && fullName && (
                    <div style={{ marginTop: 8 }}>
                      {node.key === STEP_WRITER && showSubmit && (
                        <Button
                          type="primary"
                          size="small"
                          icon={<SendOutlined />}
                          loading={submitApprovalMutation.isPending}
                          onClick={() => submitApprovalMutation.mutate({ track })}
                          style={{ borderRadius: 12 }}
                        >
                          提交审批
                        </Button>
                      )}
                      {node.key === STEP_PENDING_REVIEW && showPassRejectReviewer && (
                        <Space size="small">
                          <Button
                            type="primary"
                            size="small"
                            icon={<PassOutlined />}
                            loading={approvalPassMutation.isPending}
                            onClick={() => approvalPassMutation.mutate(track)}
                            style={{ borderRadius: 12 }}
                          >
                            通过
                          </Button>
                          <Button
                            size="small"
                            danger
                            icon={<RejectOutlined />}
                            loading={approvalRejectMutation.isPending}
                            onClick={() => approvalRejectMutation.mutate(track)}
                            style={{ borderRadius: 12 }}
                          >
                            不通过
                          </Button>
                        </Space>
                      )}
                      {node.key === STEP_PENDING_APPROVAL && showPassRejectApprover && (
                        <Space size="small">
                          <Button
                            type="primary"
                            size="small"
                            icon={<PassOutlined />}
                            loading={approvalPassMutation.isPending}
                            onClick={() => approvalPassMutation.mutate(track)}
                          >
                            通过
                          </Button>
                          <Button
                            size="small"
                            danger
                            icon={<RejectOutlined />}
                            loading={approvalRejectMutation.isPending}
                            onClick={() => approvalRejectMutation.mutate(track)}
                          >
                            不通过
                          </Button>
                        </Space>
                      )}
                    </div>
                  )}
                </div>
                {isRejected && (
                  <div style={{ color: '#B22222', fontSize: 12, marginTop: 4 }}>已拒绝</div>
                )}
              </div>
              {idx < nodes.length - 1 && (
                <span style={{ padding: '0 8px', color: '#bfbfbf', fontSize: 12, display: 'inline-block', transform: 'scaleX(1.5)' }}>
                  <ArrowRightOutlined />
                </span>
              )}
            </React.Fragment>
            );
          })}
        </div>
      </div>
    );
  };

  const ndtStep = project.approvalStepNdt ?? 0;
  const chemStep = project.approvalStepChem ?? 0;
  const rejectionStepNdt = project.rejectionStepNdt ?? null;
  const rejectionStepChem = project.rejectionStepChem ?? null;

  const canRollbackNdt = canRollbackApproval && trackHasApprovalData(
    ndtStep,
    rejectionStepNdt,
    project.writerNdt,
    project.reviewerNdt,
    project.approverNdt,
    project.writerDateNdt,
    project.reviewDateNdt,
    project.approvalDateNdt,
  );
  const canRollbackChem = canRollbackApproval && trackHasApprovalData(
    chemStep,
    rejectionStepChem,
    project.writerChem,
    project.reviewerChem,
    project.approverChem,
    project.writerDateChem,
    project.reviewDateChem,
    project.approvalDateChem,
  );
  const showRollbackButton = canRollbackNdt || canRollbackChem;

  const confirmRollbackTrack = (track: 'ndt' | 'chem', trackLabel: string) => {
    Modal.confirm({
      title: '确认回退',
      content: `将清空${trackLabel}审批人员与进度，回退到未设置状态，是否继续？`,
      okText: '确认回退',
      cancelText: '取消',
      onOk: () => approvalRollbackMutation.mutateAsync(track),
    });
  };

  const rollbackButton = canRollbackApproval ? (
    showRollbackButton ? (
      canRollbackNdt && canRollbackChem ? (
        <Dropdown
          menu={{
            items: [
              {
                key: 'ndt',
                label: '无损检测',
                onClick: () => confirmRollbackTrack('ndt', '无损检测'),
              },
              {
                key: 'chem',
                label: '理化检测',
                onClick: () => confirmRollbackTrack('chem', '理化检测'),
              },
            ],
          }}
          disabled={approvalRollbackMutation.isPending}
        >
          <Button type="primary" icon={<RollbackOutlined />} loading={approvalRollbackMutation.isPending}>
            回退
          </Button>
        </Dropdown>
      ) : (
        <Popconfirm
          title={`将清空${canRollbackNdt ? '无损检测' : '理化检测'}审批人员与进度，回退到未设置状态，是否继续？`}
          onConfirm={() => approvalRollbackMutation.mutate(canRollbackNdt ? 'ndt' : 'chem')}
          okText="确认回退"
          cancelText="取消"
        >
          <Button type="primary" icon={<RollbackOutlined />} loading={approvalRollbackMutation.isPending}>
            回退
          </Button>
        </Popconfirm>
      )
    ) : (
      <Tooltip title="当前轨道无需回退">
        <Button type="primary" icon={<RollbackOutlined />} disabled>
          回退
        </Button>
      </Tooltip>
    )
  ) : null;

  return (
    <>
      <Card
        title="审批流程"
        className="project-section-card"
        extra={
          <Space>
            <Button type="primary" icon={<SettingOutlined />} onClick={handleOpenSetting}>
              设置
            </Button>
            {rollbackButton}
            <Button type="primary" icon={<HistoryOutlined />} onClick={() => setLogModalOpen(true)}>
              审批日志
            </Button>
          </Space>
        }
      >
        {renderTrackRow(
          'ndt',
          '无损检测',
          ndtStep,
          rejectionStepNdt,
          project.writerNdt,
          project.reviewerNdt,
          project.approverNdt,
          project.writerDateNdt,
          project.reviewDateNdt,
          project.approvalDateNdt
        )}
        {renderTrackRow(
          'chem',
          '理化检测',
          chemStep,
          rejectionStepChem,
          project.writerChem,
          project.reviewerChem,
          project.approverChem,
          project.writerDateChem,
          project.reviewDateChem,
          project.approvalDateChem
        )}
      </Card>

      <Modal
        title="设置审批人"
        open={settingModalOpen}
        onCancel={() => setSettingModalOpen(false)}
        onOk={() => handleSettingSubmit()}
        width={640}
        okText="保存"
      >
        <Form form={settingForm} layout="vertical">
          <Divider orientation="left">无损检测</Divider>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item label="编写人" name="writerNdt">
                <PersonnelSelect options={personnelOptions} placeholder="请选择编写人" disabled={ndtStep >= 1} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="编写日期" name="writerDateNdt">
                <DatePicker style={{ width: '100%' }} disabled={ndtStep >= 1} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item label="审核人" name="reviewerNdt">
                <PersonnelSelect options={personnelOptions} placeholder="请选择审核人" disabled={ndtStep >= 2} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="审核日期" name="reviewDateNdt">
                <DatePicker style={{ width: '100%' }} disabled={ndtStep >= 2} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item label="批准人" name="approverNdt">
                <PersonnelSelect options={personnelOptions} placeholder="请选择批准人" disabled={ndtStep >= 3} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="批准日期" name="approvalDateNdt">
                <DatePicker style={{ width: '100%' }} disabled={ndtStep >= 3} />
              </Form.Item>
            </Col>
          </Row>
          <Divider orientation="left">理化检测</Divider>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item label="编写人" name="writerChem">
                <PersonnelSelect options={personnelOptions} placeholder="请选择编写人" disabled={chemStep >= 1} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="编写日期" name="writerDateChem">
                <DatePicker style={{ width: '100%' }} disabled={chemStep >= 1} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item label="审核人" name="reviewerChem">
                <PersonnelSelect options={personnelOptions} placeholder="请选择审核人" disabled={chemStep >= 2} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="审核日期" name="reviewDateChem">
                <DatePicker style={{ width: '100%' }} disabled={chemStep >= 2} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item label="批准人" name="approverChem">
                <PersonnelSelect options={personnelOptions} placeholder="请选择批准人" disabled={chemStep >= 3} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="批准日期" name="approvalDateChem">
                <DatePicker style={{ width: '100%' }} disabled={chemStep >= 3} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      <Modal
        title="审批日志"
        open={logModalOpen}
        onCancel={() => setLogModalOpen(false)}
        footer={null}
        width={560}
      >
        {logsLoading ? (
          <div style={{ textAlign: 'center', padding: 24 }}>加载中...</div>
        ) : approvalLogs.length === 0 ? (
          <div style={{ color: '#999', textAlign: 'center', padding: 24 }}>暂无审批记录</div>
        ) : (
          <List
            dataSource={[...approvalLogs].sort(
              (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
            )}
            renderItem={(item: ApprovalLogEntry) => (
              <List.Item>
                <div>
                  <span style={{ marginRight: 8 }}>{dayjs(item.createdAt).format('YYYY-MM-DD HH:mm')}</span>
                  <Tag>{item.track === 'ndt' ? '无损' : '理化'}</Tag>
                  <Tag color="blue">
                    {item.action === 'submit'
                      ? '提交审批'
                      : item.action === 'pass'
                        ? '通过'
                        : item.action === 'reject'
                          ? '不通过'
                          : item.action === 'rollback'
                            ? '回退'
                            : item.action}
                  </Tag>
                  {item.actorName && <span>{item.actorName}</span>}
                </div>
              </List.Item>
            )}
          />
        )}
      </Modal>
    </>
  );
};

interface UpdateProjectPayload {
  projectNumber: string;
  projectName: string;
  projectType: string;
  customer?: string;
  customerContact?: string;
  powerPlantId?: number;
  unitId?: number;
  startDate: string;
  endDate?: string;
  description?: string;
  responsiblePerson?: string;
  staff?: string;
  status: string;
  selectedExperimentTypeIds?: number[];
  writerNdt?: string;
  reviewerNdt?: string;
  approverNdt?: string;
  writerDateNdt?: string;
  reviewDateNdt?: string;
  approvalDateNdt?: string;
  writerChem?: string;
  reviewerChem?: string;
  approverChem?: string;
  writerDateChem?: string;
  reviewDateChem?: string;
  approvalDateChem?: string;
}

/** 第三方审批弹窗中单检测类型的表单状态 */
interface ThirdPartyApprovalFormRow {
  writer: string;
  writerDate: dayjs.Dayjs | null;
  reviewer: string;
  reviewDate: dayjs.Dayjs | null;
  approver: string;
  approvalDate: dayjs.Dayjs | null;
  writerLevel: string;
  reviewerLevel: string;
}

function emptyThirdPartyApprovalFormRow(): ThirdPartyApprovalFormRow {
  return {
    writer: '',
    writerDate: null,
    reviewer: '',
    reviewDate: null,
    approver: '',
    approvalDate: null,
    writerLevel: '',
    reviewerLevel: '',
  };
}

/** 将库中旧写法规范为下拉值 Ⅱ / Ⅲ（与后端 NdtQualificationRegistry 一致） */
function normalizeNdtLevelToSelectValue(raw: string | undefined): string {
  if (!raw?.trim()) return '';
  const t = raw.trim();
  const u = t.toUpperCase();
  if (u === '2' || u === 'II' || t === 'Ⅱ' || t === 'ⅱ') return 'Ⅱ';
  if (u === '3' || u === 'III' || t === 'Ⅲ' || t === 'ⅲ') return 'Ⅲ';
  return '';
}

function parseThirdPartyRow(raw: Record<string, string> | undefined): ThirdPartyApprovalFormRow {
  const e = emptyThirdPartyApprovalFormRow();
  if (!raw) return e;
  return {
    writer: raw.writer ?? '',
    writerDate: raw.writerDate ? dayjs(raw.writerDate) : null,
    reviewer: raw.reviewer ?? '',
    reviewDate: raw.reviewDate ? dayjs(raw.reviewDate) : null,
    approver: raw.approver ?? '',
    approvalDate: raw.approvalDate ? dayjs(raw.approvalDate) : null,
    writerLevel: normalizeNdtLevelToSelectValue(raw.writerLevel),
    reviewerLevel: normalizeNdtLevelToSelectValue(raw.reviewerLevel),
  };
}

/** 与后端 Word 中无损级别逻辑一致：MT/PT/UT/RT/ET（LP 视为 PT） */
function experimentTypeSupportsNdtLevelCode(code: string | undefined): boolean {
  if (!code) return false;
  const c = code.trim().toUpperCase();
  const n = c === 'LP' ? 'PT' : c;
  return n === 'MT' || n === 'PT' || n === 'UT' || n === 'RT' || n === 'ET';
}

/** 单项 Word 中该段多为「检测结果」的实验类型代码（与后端结论格用语一致） */
/** 检测内容多行时按段预览/覆盖：仅 UT/PAUT（与后端 reportWordSegmentCount 一致，多部件单报告不拆段） */
const EXPORT_TEXT_PER_CONTENT_ROW_TYPE_CODES = new Set(['UT', 'PAUT']);

const EXPORT_TEXT_RESULT_LIKE_TYPE_CODES = new Set([
  'UT',
  'PAUT',
  'MT',
  'PT',
  'LP',
  'RT',
  'VT',
  'UTM',
  'RDM',
  'PDM',
]);

const ProjectDetailPage: React.FC = () => {
  const navigate = useNavigate();
  const { id } = useParams();
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const fullName = user?.fullName?.trim() || user?.username?.trim() || '';

  // 状态管理
  const [activeExperimentTypes, setActiveExperimentTypes] = useState<ActiveExperimentType[]>([]);
  const [currentActiveType, setCurrentActiveType] = useState<number | null>(null);
  const [detailSectionTab, setDetailSectionTab] = useState<DetailSectionTabKey>('detectionLog');
  const [showAddTypeModal, setShowAddTypeModal] = useState(false);
  const [selectedTypeToAdd, setSelectedTypeToAdd] = useState<number | null>(null);
  const [reportRows, setReportRows] = useState<Record<number, ReportRow[]>>({});
  
  // 新增状态：部件列表和批量操作
  const [components, setComponents] = useState<ProjectComponent[]>([]);
  const [instruments, setInstruments] = useState<ProjectInstrument[]>([]);
  const [selectedReportIds, setSelectedReportIds] = useState<number[]>([]);
  const [batchOperationLoading, setBatchOperationLoading] = useState(false);
  const [reportSearchKeyword, setReportSearchKeyword] = useState('');
  const [showTodayCreatedReportsOnly, setShowTodayCreatedReportsOnly] = useState(false);
  const [reportFiguresModalOpen, setReportFiguresModalOpen] = useState(false);
  const [projectReportFigures, setProjectReportFigures] = useState<ImageAttachment[]>([]);
  const [reportFiguresSaving, setReportFiguresSaving] = useState(false);

  // 检测数据Modal状态
  const [detectionDataModal, setDetectionDataModal] = useState<{
    open: boolean;
    typeId: number | null;
    rowIndex: number | null;
    contentRowIndex: number | null;
  }>({ open: false, typeId: null, rowIndex: null, contentRowIndex: null });
  const [leebCategoryModalState, setLeebCategoryModalState] = useState<{
    open: boolean;
    typeId: number | null;
    rowIndex: number | null;
    sourceRows: Record<string, unknown>[];
    mapping: LeebCategoryMapping[];
    detectionData: ParsedTableDataShape | null;
    rowData?: ReportRow;
    detectionContentOverride?: DetectionContentPayload | null;
  }>({
    open: false,
    typeId: null,
    rowIndex: null,
    sourceRows: [],
    mapping: [],
    detectionData: null,
    rowData: undefined,
    detectionContentOverride: undefined,
  });

  const [thirdPartyApprovalModalOpen, setThirdPartyApprovalModalOpen] = useState(false);
  const [thirdPartyApprovalDraft, setThirdPartyApprovalDraft] = useState<
    Record<string, ThirdPartyApprovalFormRow>
  >({});
  const [thirdPartyApprovalSaving, setThirdPartyApprovalSaving] = useState(false);

  const [exportTextDrawerOpen, setExportTextDrawerOpen] = useState(false);
  const [exportTextContext, setExportTextContext] = useState<{
    reportId: number | null;
    typeId: number;
    rowIndex: number;
  } | null>(null);
  const [exportTextPreview, setExportTextPreview] = useState<ExportTextPreview | null>(null);
  const [exportTextContentRowIndex, setExportTextContentRowIndex] = useState(0);
  const [exportTextLoading, setExportTextLoading] = useState(false);
  const [exportTextSaving, setExportTextSaving] = useState(false);
  const [reportOrderModalOpen, setReportOrderModalOpen] = useState(false);
  const [exportTextFields, setExportTextFields] = useState({
    detectionNarrativeBody: '',
    conclusionParagraph: '',
    overviewWorkContentLine: '',
    overviewDefectLine: '',
  });

  const exportTextConclusionBlockTitle = useMemo(() => {
    if (!exportTextContext) return '检测结论';
    const aet = activeExperimentTypes.find((a) => a.id === exportTextContext.typeId);
    const code = (aet?.experimentType?.code ?? '').toUpperCase();
    return EXPORT_TEXT_RESULT_LIKE_TYPE_CODES.has(code) ? '检测结论 / 检测结果' : '检测结论';
  }, [exportTextContext, activeExperimentTypes]);

  const exportTextDrawerReportRow = useMemo(() => {
    if (!exportTextContext) return undefined;
    return reportRows[exportTextContext.typeId]?.[exportTextContext.rowIndex];
  }, [exportTextContext, reportRows]);

  const exportTextShowDefectSection = useMemo(() => {
    if (exportTextPreview == null) return false;
    if (!exportTextContext) return exportTextPreview.showDefectSection;
    const code =
      activeExperimentTypes.find((a) => a.id === exportTextContext.typeId)?.experimentType?.code ?? '';
    const local = reportRows[exportTextContext.typeId]?.[exportTextContext.rowIndex]?.hasDefect;
    return mergeExportPreviewShowDefectFromLocalRow(exportTextPreview, code, local).showDefectSection;
  }, [exportTextPreview, exportTextContext, reportRows, activeExperimentTypes]);

  const exportTextPerContentRowEnabled = useMemo(() => {
    if (!exportTextContext || !exportTextDrawerReportRow) return false;
    const rowCount = detectionContentTableRowCount(exportTextDrawerReportRow.detectionContent);
    if (rowCount <= 1) return false;
    const aet = activeExperimentTypes.find((a) => a.id === exportTextContext.typeId);
    const code = (aet?.experimentType?.code ?? '').toUpperCase();
    return EXPORT_TEXT_PER_CONTENT_ROW_TYPE_CODES.has(code);
  }, [exportTextContext, activeExperimentTypes, exportTextDrawerReportRow]);

  const exportTextSegmentOptions = useMemo(() => {
    if (!exportTextPerContentRowEnabled || !exportTextDrawerReportRow) return [];
    const dc = exportTextDrawerReportRow.detectionContent;
    const rows =
      dc && (dc.mode === 'table' || dc.mode === 'sod') && Array.isArray((dc as { rows?: unknown[] }).rows)
        ? ((dc as { rows: Array<{ type?: string }> }).rows)
        : [];
    const compIds = exportTextDrawerReportRow.projectComponentIds;
    return rows.map((r, i) => {
      const parts: string[] = [];
      const summary = buildRowComponentSummary(
        r as DetectionContentTableRow,
        i,
        compIds,
        components,
      );
      if (summary.trim()) {
        parts.push(...summary.split(' / ').filter(Boolean));
      }
      if (parts.length === 0 && r.type?.trim()) {
        parts.push(r.type.trim());
      }
      if (parts.length === 0) {
        parts.push(`部件 ${i + 1}`);
      }
      const fullLabel = parts.join(' / ');
      const shortLabel = fullLabel.length > 28 ? `${fullLabel.slice(0, 26)}…` : fullLabel;
      return {
        value: i,
        fullLabel,
        label: (
          <Tooltip title={fullLabel}>
            <span>{shortLabel}</span>
          </Tooltip>
        ),
      };
    });
  }, [exportTextPerContentRowEnabled, exportTextDrawerReportRow, components]);

  const highlightBackground = '#f6f2ff';
  const highlightBorderColor = '#cbb5ff';
  const selectedRowKeySet = useMemo(() => new Set(selectedReportIds), [selectedReportIds]);

  const currentRowsForFilter = currentActiveType ? reportRows[currentActiveType] || [] : [];

  const reportSearchFilteredRows = useMemo(() => {
    const rows = currentRowsForFilter;
    const q = reportSearchKeyword.trim().toLowerCase();
    const afterSearch = !q
      ? rows.map((row, i) => ({ row, storeIndex: i }))
      : rows
          .map((row, i) => ({ row, storeIndex: i }))
          .filter(({ row }) => reportRowMatchesSearch(row, q, components, instruments));
    if (!showTodayCreatedReportsOnly) {
      return afterSearch;
    }
    return afterSearch.filter(({ row }) => reportRowMatchesTodayCreatedFilter(row));
  }, [
    currentRowsForFilter,
    reportSearchKeyword,
    showTodayCreatedReportsOnly,
    components,
    instruments,
  ]);

  useEffect(() => {
    if (typeof document === 'undefined') {
      return;
    }

    const styleId = 'project-detail-custom-styles';
    if (document.getElementById(styleId)) {
      return;
    }

    const style = document.createElement('style');
    style.id = styleId;
    style.innerHTML = `
      .report-detail-table .ant-table-tbody > tr.report-row--editing > td,
      .report-detail-table .ant-table-tbody > tr.report-row--selected > td {
        background: ${highlightBackground} !important;
      }

      .report-detail-table .ant-table-tbody > tr.report-row--editing:hover > td,
      .report-detail-table .ant-table-tbody > tr.report-row--selected:hover > td {
        background: #efe5ff !important;
      }

      .report-detail-table .ant-table-tbody > tr.report-row--expanded > td {
        border-bottom: none;
      }

      .report-detail-table .ant-table-tbody > tr.report-row--expanded + tr.ant-table-expanded-row > td {
        border-top: none;
      }

      .report-detail-table .ant-table-tbody > tr.ant-table-expanded-row > td {
        background: #e8e0f7 !important;
        padding: 12px 16px !important;
        border-left: 4px solid ${highlightBorderColor};
        vertical-align: top;
      }

      .report-expanded-panel {
        margin: 4px 0 12px;
      }

      .report-detail-table .ant-table-tbody > tr > td {
        vertical-align: top;
      }

      .report-subcard {
        background: #f3ecff;
        border-radius: 12px;
        padding: 12px 0 10px;
        margin: 8px 0;
        border: 1px solid rgba(100, 65, 214, 0.22);
        box-shadow:
          inset 0 0 0 1px rgba(100, 65, 214, 0.08),
          0 2px 8px rgba(100, 65, 214, 0.12);
      }

      /* Project 页面统一 Card 样式（替代重复的 inline style）  border-radius: 12px;
        box-shadow: 0 6px 20px rgba(0, 0, 0, 0.06);
        border: 1px solid #f0f0f0;
        overflow: hidden;*/
      .project-section-card {
        margin-bottom: 16px;
        border-radius: 12px;
        box-shadow: 0 6px 20px rgba(0, 0, 0, 0.06);
        border: 1px solid #f0f0f0;
        overflow: hidden;
      }

      /* 检测部件/仪器设备列表：与「项目报告列表」Card 内容区左右边距一致（去掉 Collapse 默认水平内缩） */
      .project-inline-list-card .ant-collapse > .ant-collapse-item > .ant-collapse-header {
        padding-inline: 0 !important;
      }
      .project-inline-list-card .ant-collapse > .ant-collapse-item > .ant-collapse-content > .ant-collapse-content-box {
        padding-inline: 0 !important;
      }

      .report-subcard-section + .report-subcard-section {
        margin-top: 8px;
      }

      .report-subcard-content {
        padding: 0;
      }

      .report-subcard-section-body {
        display: flex;
        flex-direction: column;
        gap: 12px;
      }

      .report-subcard-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0 12px; /* header 与外围留出左右空间 */
        color: #6441d6;
        font-weight: 500;
      }

      .report-subcard-title {
        margin: 0;
        font-size: 14px;
      }

      /* ========== 原始样式备份（如需恢复，取消下面的注释并删除新样式） ========== */
      /*
      .report-subcard-table {
        border: 1px solid rgba(100, 65, 214, 0.16);
        border-radius: 0 0 12px 12px;
        background: #ffffff;
        overflow: hidden;
        width: 100%;
        margin: 0;
      }

      .report-subcard-table .ant-table-thead > tr > th:first-child,
      .report-subcard-table .ant-table-cell:first-child {
        padding-left: 0 !important;
        padding-right: 8px;
      }
      */
      /* ========== 原始样式备份结束 ========== */

      /* ========== 新样式（与弹窗一致） ========== */
      .report-subcard-table {
        border: 1px solid rgba(100, 65, 214, 0.16);
        border-radius: 10px;
        background: #ffffff;
        overflow: hidden;
        width: 100%;
        margin: 0;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      }

      /* 确保表格第一行和最后一行也有圆角 */
      .report-subcard-table .ant-table-thead > tr:first-child > th:first-child {
        border-top-left-radius: 10px;
      }

      .report-subcard-table .ant-table-thead > tr:first-child > th:last-child {
        border-top-right-radius: 10px;
      }

      .report-subcard-table .ant-table-tbody > tr:last-child > td:first-child {
        border-bottom-left-radius: 10px;
      }

      .report-subcard-table .ant-table-tbody > tr:last-child > td:last-child {
        border-bottom-right-radius: 10px;
      }

      .report-subcard-table .ant-table-thead > tr > th {
        background: #f9f9f9 !important;
        color: #434343;
        font-weight: 600;
        border-bottom: 1px solid #ededed;
        padding: 12px 12px;
        white-space: nowrap;
      }

      /* 表格最左侧列添加左边距，不紧贴表格边缘 */
      .report-subcard-table .ant-table-thead > tr > th:first-child,
      .report-subcard-table .ant-table-tbody > tr > td:first-child {
        padding-left: 16px !important;
        padding-right: 8px;
      }

      /* 表格最右侧列添加右边距 */
      .report-subcard-table .ant-table-thead > tr > th:last-child,
      .report-subcard-table .ant-table-tbody > tr > td:last-child {
        padding-right: 16px !important;
      }

      .report-subcard-table .ant-table-tbody > tr > td {
        background: #ffffff !important;
        border-bottom: 1px solid #f1f1f1;
        padding: 10px 12px;
        width: auto;
        vertical-align: top;
      }

      .report-subcard-table .ant-table-tbody > tr > td:first-child .ant-form-item,
      .report-subcard-table .ant-table-tbody > tr > td:first-child .ant-input,
      .report-subcard-table .ant-table-tbody > tr > td:first-child .ant-select {
        width: 100%;
      }

      .report-subcard-table .ant-table-tbody > tr:hover > td {
        background: #f8f7ff !important;
      }

      .report-subcard-table .ant-table-tbody > tr:last-child > td {
        border-bottom: none;
      }

      .report-subcard-table .ant-table-footer {
        background: #ffffff !important;
        border-top: 1px solid #f1f1f1;
        padding: 12px 12px 16px;
        margin: 0;
      }

      .report-subcard-footer {
        display: flex;
        justify-content: center;
        width: 100%;
      }

      /* 如果表格为空，placeholder 也需要圆角 */
      .report-subcard-table .ant-table-placeholder {
        border-radius: 10px;
      }

      /* 检测数据弹窗样式 - 与子卡片一致 */
      .detection-data-modal .ant-modal-content {
        background: #f3ecff;
        border-radius: 12px;
        padding: 0;
        border: 1px solid rgba(100, 65, 214, 0.16);
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2), 0 4px 12px rgba(0, 0, 0, 0.15);
        overflow: hidden;
      }

      .detection-data-modal .ant-modal-body {
        border-radius: 12px;
        padding: 0;
      }

      .detection-data-modal .ant-modal-close {
        color: #6441d6;
        top: 8px;
        right: 8px;
        width: 32px;
        height: 32px;
        line-height: 32px;
        border-radius: 6px;
        background: rgba(255, 255, 255, 0.9);
        z-index: 10;
      }

      .detection-data-modal .ant-modal-close:hover {
        color: #8b6ce8;
        background: rgba(255, 255, 255, 1);
      }

      .detection-data-modal .report-subcard-section-body {
        padding: 0;
        border-radius: 0;
        background: transparent;
      }

      /* 调整弹窗中的 header，为关闭按钮留出空间 */
      .detection-data-modal .report-subcard-header {
        padding-right: 48px; /* 为关闭按钮留出空间 */
      }

      /* 弹窗中的子卡片样式调整 */
      .detection-data-modal .report-subcard {
        margin: 0;
        box-shadow: none;
      }

      /* 弹窗中的表格添加圆角 */
      .detection-data-modal .report-subcard-table {
        border-radius: 10px !important;
        overflow: hidden;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      }

      /* 确保表格第一行和最后一行也有圆角 */
      .detection-data-modal .report-subcard-table .ant-table-thead > tr:first-child > th:first-child {
        border-top-left-radius: 10px;
      }

      .detection-data-modal .report-subcard-table .ant-table-thead > tr:first-child > th:last-child {
        border-top-right-radius: 10px;
      }

      .detection-data-modal .report-subcard-table .ant-table-tbody > tr:last-child > td:first-child {
        border-bottom-left-radius: 10px;
      }

      .detection-data-modal .report-subcard-table .ant-table-tbody > tr:last-child > td:last-child {
        border-bottom-right-radius: 10px;
      }

      /* 如果表格为空，footer 也需要圆角 */
      .detection-data-modal .report-subcard-table .ant-table-placeholder {
        border-radius: 10px;
      }

      /* 弹窗中表格最左侧列添加左边距，不紧贴表格边缘 */
      .detection-data-modal .report-subcard-table .ant-table-thead > tr > th:first-child,
      .detection-data-modal .report-subcard-table .ant-table-tbody > tr > td:first-child {
        padding-left: 16px !important;
      }

      /* 弹窗中表格最右侧列添加右边距 */
      .detection-data-modal .report-subcard-table .ant-table-thead > tr > th:last-child,
      .detection-data-modal .report-subcard-table .ant-table-tbody > tr > td:last-child {
        padding-right: 16px !important;
      }

      /* 项目类型 Select：复用“客户方下拉”视觉效果（下划线 + 左侧图标留白） */
      .select-with-icon .ant-select-selector {
        border: none !important;
        border-bottom: 1px solid #D1D5DC !important;
        border-top: none !important;
        border-left: none !important;
        border-right: none !important;
        border-radius: 0 !important;
        background-color: transparent !important;
        box-shadow: none !important;
      }

      .select-with-icon .ant-select-selection-placeholder,
      .select-with-icon .ant-select-selection-item,
      .select-with-icon .ant-select-selection-search {
        padding-left: 36px !important;
      }

      .select-with-icon .anticon {
        color: #bfbfbf !important;
        transition: color 0.3s !important;
        pointer-events: none !important;
        z-index: 10 !important;
      }
    `;

    document.head.appendChild(style);

    return () => {
      const existing = document.getElementById(styleId);
      if (existing && existing.parentNode) {
        existing.parentNode.removeChild(existing);
      }
    };
  }, [highlightBackground, highlightBorderColor]);

  const getRowClassName = useCallback(
    (record: ReportRow & { key: number }) => {
      const classes: string[] = [];
      if (record.isEditing || record.isNew) {
        classes.push('report-row--editing');
      }
      if (selectedRowKeySet.has(record.key)) {
        classes.push('report-row--selected');
      }
      if (record.detectionContentExpanded || record.imageAttachmentsExpanded || record.expanded) {
        classes.push('report-row--expanded');
      }
      return classes.join(' ');
    },
    [selectedRowKeySet]
  );

  const { data: project, isLoading, error: projectError } = useQuery<ProjectDetail>({
    queryKey: ['project', id],
    queryFn: () => projectService.getById(Number(id)),
    retry: (failureCount, error: any) => {
      // 如果是404错误，不重试
      if (error?.response?.status === 404) {
        return false;
      }
      // 其他错误最多重试3次
      return failureCount < 3;
    },
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
  });

  const formatOptionalText = useCallback((value?: string | null) => {
    if (!value) {
      return '未选定';
    }
    const trimmed = value.trim();
    return trimmed.length > 0 ? trimmed : '未选定';
  }, []);

  const getApiErrorMessage = useCallback(async (error: any, fallback: string) => {
    const data = error?.response?.data;
    if (data instanceof Blob) {
      try {
        const text = await data.text();
        const parsed = JSON.parse(text);
        if (parsed?.message) {
          return parsed.message as string;
        }
      } catch {
        return fallback;
      }
    }
    return error?.response?.data?.message || fallback;
  }, []);

  // 处理项目加载错误
  useEffect(() => {
    if (projectError) {
      console.error('Failed to load project:', projectError);
      const error = projectError as any;
      if (error?.response?.status === 404) {
        message.error('项目不存在或已被删除');
      } else {
        message.error('加载项目详情失败，请稍后重试');
      }
    }
  }, [projectError]);

  const { data: experimentTypes, error: experimentTypesError } = useQuery<ExperimentType[]>({
    queryKey: ['experimentTypes'],
    queryFn: () => experimentTypeService.getAll(),
    staleTime: 0,           // 新增：数据立即过期
    gcTime: 0,              // 新增：完全不缓存（原 cacheTime）
    refetchOnMount: true,   // 新增：每次挂载时重新获取
    retry: 3,
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
  });

  // 处理检测类型加载错误
  useEffect(() => {
    if (experimentTypesError) {
      console.error('Failed to load experiment types:', experimentTypesError);
      message.error('加载检测类型失败，请稍后重试');
    }
  }, [experimentTypesError]);

  const { data: projectReports, error: projectReportsError } = useQuery<ReportList[]>({
    queryKey: ['projectReports', id],
    queryFn: () => reportService.getByProject(Number(id)),
    enabled: !!id,
    retry: 3,
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
  });

  // 处理项目报告加载错误
  useEffect(() => {
    if (projectReportsError) {
      console.error('Failed to load project reports:', projectReportsError);
      message.error('加载项目报告失败，请稍后重试');
    }
  }, [projectReportsError]);

  // 查询部件列表
  const { data: projectComponents, refetch: refetchComponents } = useQuery({
    queryKey: ['projectComponents', id],
    queryFn: () => componentService.getProjectComponents(Number(id)),
    enabled: !!id,
    retry: 3,
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
  });

  // 处理部件列表加载错误
  useEffect(() => {
    // 错误处理通过组件内部的错误处理机制
  }, []);

  // 查询仪器设备列表
  const { data: projectInstruments, refetch: refetchInstruments } = useQuery({
    queryKey: ['projectInstruments', id],
    queryFn: () => instrumentService.getProjectInstruments(Number(id)),
    enabled: !!id,
    retry: 3,
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
  });

  // 处理仪器设备列表加载错误
  useEffect(() => {
    // 错误处理通过组件内部的错误处理机制
  }, []);

  // 同步 projectComponents 到 components 状态（与部件表一致排序）
  useEffect(() => {
    if (projectComponents) {
      setComponents(sortProjectComponents(projectComponents));
    }
  }, [projectComponents]);

  const sortedProjectComponentsForRank = useMemo(
    () => sortProjectComponents(projectComponents ?? []),
    [projectComponents],
  );
  const componentRankMap = useMemo(
    () => buildComponentSortRankMap(sortedProjectComponentsForRank),
    [sortedProjectComponentsForRank],
  );

  // 同步 projectInstruments 到 instruments 状态
  useEffect(() => {
    if (projectInstruments) {
      setInstruments(projectInstruments);
    }
  }, [projectInstruments]);

  // 组件卸载时刷新数据
  React.useEffect(() => {
    return () => {
      // 组件卸载时刷新项目报告数据
      queryClient.invalidateQueries({ queryKey: ['projectReports', id] });
    };
  }, [id, queryClient]);

  // 初始化历史报告数据和已选检测类型
  React.useEffect(() => {
    if (!project || !experimentTypes) return;
    // 确保 projectReports 已加载完成（undefined 表示还在加载中）
    if (projectReports === undefined) return;

    // 如果projectReports加载失败，使用空数组
    const reports = projectReports || [];
    
    // 按检测类型分组报告
    const typeMap = new Map<number, ReportRow[]>();
    const loadedTypeIds = new Set<number>();
    
    reports.forEach(report => {
      const expType = experimentTypes.find(et => et.name === report.testMethod);
      if (!expType) return;
      
      loadedTypeIds.add(expType.id);
      
      if (!typeMap.has(expType.id)) {
        typeMap.set(expType.id, []);
      }
      
      const detectionContentConfig = getDetectionContentConfigByName(expType.name);
      let detectionContentValue = normalizeDetectionContentValue(
        (report as ReportList).detectionContent,
        detectionContentConfig,
      );

      const multiIds = (report as ReportList).projectComponentIds;
      const isMultiComponentTable =
        Array.isArray(multiIds) && multiIds.length > 1 && (detectionContentValue.mode === 'table' || detectionContentValue.mode === 'sod');

      if (isMultiComponentTable) {
        const rawRows = (detectionContentValue as { rows?: DetectionContentTableRow[] }).rows ?? [];
        const rows =
          rawRows.length > 0
            ? sanitizeTableRowComponentIds(rawRows, multiIds)
            : sanitizeTableRowComponentIds(resizeDetectionContentTableRows([], 1), multiIds);
        if (detectionContentValue.mode === 'sod') {
          detectionContentValue = {
            ...(detectionContentValue as DetectionContentPayload),
            rows,
          } as DetectionContentPayload;
        } else {
          detectionContentValue = {
            mode: 'table',
            rows,
          } as DetectionContentPayload;
        }
      }

      const contentRowCountForData =
        (detectionContentValue.mode === 'table' || detectionContentValue.mode === 'sod') &&
        Array.isArray((detectionContentValue as { rows?: unknown[] }).rows)
          ? (detectionContentValue as { rows: unknown[] }).rows.length
          : 1;
      const alignN = Math.max(1, contentRowCountForData);

      let detectionData = buildTableDataPayload(
        alignPerContentRowToContentRowCount([{ rows: [] }], alignN),
      );
      if (report.reportItems && report.reportItems.length > 0) {
        try {
          const tableData = report.reportItems[0].tableData;
          if (tableData) {
            const parsed = JSON.parse(tableData);
            const normalizeRowsWithSchema = (rows: any[]): any[] => {
              const activeType = activeExperimentTypes.find(
                (aet) => aet.experimentType.id === report.reportItems?.[0]?.experimentTypeId,
              );
              if (!activeType?.experimentType?.tableSchema) {
                return rows;
              }
              try {
                const schema = JSON.parse(activeType.experimentType.tableSchema);
                // 合金化学成分等：schema 只有「编号」，元素列为运行时多选；按 schema 投影会丢掉 Mn/Cr 等键与数值
                const cols = schema.columns;
                const isAlloyDynamicElementTable =
                  Array.isArray(cols) && cols.length === 1 && cols[0]?.key === '编号';
                if (isAlloyDynamicElementTable) {
                  return rows.map((row: any) => ({ ...row }));
                }
                return rows.map((row: any) => {
                  const normalizedRow: any = {};
                  schema.columns.forEach((col: any) => {
                    const value = row[col.key];
                    normalizedRow[col.key] =
                      value === null || value === undefined || value === '' ? '' : String(value);
                  });
                  // 保留扩展业务字段：不在 schema.columns 中，但需要在前端回显。
                  if (Object.prototype.hasOwnProperty.call(row, RECORD_ONLY_DEFECT_KEY)) {
                    const recordOnly = row[RECORD_ONLY_DEFECT_KEY];
                    normalizedRow[RECORD_ONLY_DEFECT_KEY] =
                      recordOnly === null || recordOnly === undefined || recordOnly === ''
                        ? ''
                        : String(recordOnly);
                  }
                  return normalizedRow;
                });
              } catch (schemaError) {
                console.error('Failed to parse tableSchema:', schemaError);
                return rows;
              }
            };
            let blocks = extractPerContentRowBlocks(parsed);
            blocks = blocks.map((b) => ({ rows: normalizeRowsWithSchema(b.rows) }));
            blocks = alignPerContentRowToContentRowCount(blocks, alignN);
            detectionData = buildTableDataPayload(blocks);
          }
        } catch (error) {
          console.error('Failed to parse detectionData for report:', report.id, error);
        }
      }
      
      const customFields = report.customFields || {};
      typeMap.get(expType.id)?.push({
        id: report.id,
        experimentTypeId: expType.id,
        reportNumber: report.reportNumber,
        projectComponentId: report.projectComponentId,
        projectComponentIds: (report as ReportList).projectComponentIds,
        projectInstrumentId: report.projectInstrumentId,
        title: report.title,
        componentName: report.componentName,
        equipmentCategory: report.equipmentCategory,
        equipmentName: report.equipmentName,
        componentSpec: report.componentSpec,
        instrumentModel: report.instrumentModel,
        instrumentNumber: report.instrumentNumber,  // ✅ 关键字段 - 仪器编号
        inspector: report.inspector,
        location: report.location,
        testDate: report.testDate,
        reportImage: report.reportImage,
        imageAttachments: (report as any).imageAttachments || [],  // ✅ 直接从报告字段读取
        imageAttachmentsExpanded: false,
        hasDefect: report.hasDefect,
        summary: (report as any).summary || '',
        customFields,
        nonComplianceRecords: extractNonComplianceRecords(customFields),
        status: report.status,
        createdAt: report.createdAt,
        isNew: false,
        isEditing: false,
        expanded: false,
        detectionData: detectionData,  // ✅ 使用从后端加载的检测数据
        detectionContent: detectionContentValue,
        detectionContentExpanded: false,
      });
    });
    
    // 合并现有的检测类型（保留用户手动添加的类型）
    const mergedTypes: ActiveExperimentType[] = [];
    const mergedRows: Record<number, ReportRow[]> = {};
    const allTypeIds = new Set<number>();
    
    // 首先添加从数据库保存的已选检测类型
    const selectedTypeIds = (project as any).selectedExperimentTypeIds || [];
    selectedTypeIds.forEach((typeId: number) => {
      const expType = experimentTypes.find(et => et.id === typeId);
      if (expType) {
        allTypeIds.add(typeId);
        mergedTypes.push({
          id: typeId,
          name: expType.name,
          experimentType: expType,
        });
        mergedRows[typeId] = typeMap.get(typeId) || [];
      }
    });
    
    // 然后添加有报告但未在已选列表中的类型（向后兼容）
    loadedTypeIds.forEach(typeId => {
      if (!allTypeIds.has(typeId)) {
        const expType = experimentTypes.find(et => et.id === typeId);
        if (expType) {
          allTypeIds.add(typeId);
          mergedTypes.push({
            id: typeId,
            name: expType.name,
            experimentType: expType,
          });
          mergedRows[typeId] = typeMap.get(typeId) || [];
        }
      }
    });
    
    setActiveExperimentTypes(mergedTypes);
    // 保留 refetch 前用户展开状态，避免保存后 invalidate 重建行时收起检测内容/附图区
    setReportRows((prev) => {
      const uiByReportId = new Map<
        number,
        { detectionContentExpanded: boolean; imageAttachmentsExpanded: boolean; expanded: boolean }
      >();
      for (const rows of Object.values(prev)) {
        for (const r of rows) {
          if (r.id != null) {
            uiByReportId.set(r.id, {
              detectionContentExpanded: !!r.detectionContentExpanded,
              imageAttachmentsExpanded: !!r.imageAttachmentsExpanded,
              expanded: !!r.expanded,
            });
          }
        }
      }
      const next: Record<number, ReportRow[]> = {};
      for (const [typeIdStr, rows] of Object.entries(mergedRows)) {
        const typeId = Number(typeIdStr);
        const mergedUi = rows.map((row) => {
          const ui = row.id != null ? uiByReportId.get(row.id) : undefined;
          if (!ui) return row;
          return {
            ...row,
            detectionContentExpanded: ui.detectionContentExpanded,
            imageAttachmentsExpanded: ui.imageAttachmentsExpanded,
            expanded: ui.expanded,
          };
        });
        next[typeId] = mergedUi;
      }
      return next;
    });

    setCurrentActiveType((cur) => {
      if (mergedTypes.length === 0) return cur;
      if (cur != null && mergedTypes.some((t) => t.id === cur)) return cur;
      return mergedTypes[0].id;
    });
  }, [project, projectReports, experimentTypes]);

  // 计算项目统计信息
  const projectStats: ProjectStats | null = React.useMemo(() => {
    if (!project && !projectReports) return null;

    // 统计口径与检测日志保持一致：优先使用 projectReports（与 DetectionLog 同源）
    const reports = projectReports || project?.reports || [];
    const totalReports = reports.length;
    const defectCount = reports.filter((r) => r.testDate && r.hasDefect === '是').length;
    const detectionTypeTotal = new Set(
      reports
        .map((r) => (r.testMethod || '').trim())
        .filter(Boolean)
    ).size;

    const validDates = reports
      .map((r) => r.testDate)
      .filter((date): date is string => !!date && dayjs(date).isValid())
      .map((date) => dayjs(date).startOf('day'));

    const workDays = (() => {
      if (validDates.length === 0) return 0;
      const minDate = validDates.reduce((min, current) => (current.isBefore(min) ? current : min));
      const maxDate = validDates.reduce((max, current) => (current.isAfter(max) ? current : max));
      return maxDate.diff(minDate, 'day') + 1;
    })();

    return {
      totalReports,
      defectCount,
      workDays,
      detectionTypeTotal,
    };
  }, [project, projectReports]);

  const invalidateReportChangeLogQueries = useCallback(() => {
    const projectId = Number(id);
    if (!projectId) return;
    void queryClient.invalidateQueries({ queryKey: ['report-change-logs', projectId] });
    void queryClient.invalidateQueries({ queryKey: ['report-change-summary', projectId] });
  }, [id, queryClient]);

  // 创建报告
  const createReportMutation = useMutation({
    mutationFn: (report: CreateReport) => reportService.create(report),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', id] });
      invalidateReportChangeLogQueries();
      // 不要在这里刷新 projectReports，让 handleSaveReport 处理
    },
    onError: () => {
      message.error('报告创建失败');
    },
  });

  // 删除报告
  const deleteReportMutation = useMutation({
    mutationFn: (reportId: number) => reportService.delete(reportId),
    onSuccess: (_data, reportId) => {
      // 先同步 projectReports 缓存，避免 useEffect 基于旧缓存重建 reportRows 造成闪烁
      queryClient.setQueryData<ReportList[] | undefined>(['projectReports', id], (oldReports) =>
        (oldReports || []).filter((report) => report.id !== reportId)
      );
      queryClient.invalidateQueries({ queryKey: ['project', id] });
      queryClient.invalidateQueries({ queryKey: ['projectReports', id] });
      invalidateReportChangeLogQueries();
      message.success('报告删除成功');
    },
    onError: () => {
      message.error('报告删除失败');
    },
  });

  // 审批：提交审批
  const submitApprovalMutation = useMutation({
    mutationFn: ({ track }: { track: 'ndt' | 'chem' | 'both' }) =>
      projectService.submitApproval(Number(id), track),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', id] });
      queryClient.invalidateQueries({ queryKey: ['my-todos'] });
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      message.success('已提交审批');
    },
    onError: (err: any) => {
      message.error(err?.response?.data?.message || '提交审批失败');
    },
  });

  // 审批：通过
  const approvalPassMutation = useMutation({
    mutationFn: (track: 'ndt' | 'chem') => projectService.approvalPass(Number(id), track),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', id] });
      queryClient.invalidateQueries({ queryKey: ['my-todos'] });
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      message.success('已通过');
    },
    onError: (err: any) => {
      message.error(err?.response?.data?.message || '操作失败');
    },
  });

  // 审批：不通过（退回编写人）
  const approvalRejectMutation = useMutation({
    mutationFn: (track: 'ndt' | 'chem') => projectService.approvalReject(Number(id), track),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', id] });
      queryClient.invalidateQueries({ queryKey: ['my-todos'] });
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      message.success('已退回编写人');
    },
    onError: (err: any) => {
      message.error(err?.response?.data?.message || '操作失败');
    },
  });

  const approvalRollbackMutation = useMutation({
    mutationFn: (track: 'ndt' | 'chem') => projectService.approvalRollback(Number(id), track),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', id] });
      queryClient.invalidateQueries({ queryKey: ['my-todos'] });
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      queryClient.invalidateQueries({ queryKey: ['approval-logs', Number(id)] });
      message.success('已回退审批流程');
    },
    onError: (err: any) => {
      message.error(err?.response?.data?.message || '回退失败');
    },
  });

  const canRollbackApproval = useMemo(() => {
    if (project?.canRollbackApproval != null) {
      return project.canRollbackApproval;
    }
    if (!project || !user) return false;
    if (isSubUser()) return false;
    const currentUserId = user.userId?.trim();
    if (currentUserId && project.userId && project.userId === currentUserId) {
      return true;
    }
    const names = [user.fullName?.trim(), user.username?.trim()].filter(Boolean) as string[];
    const responsible = project.responsiblePerson?.trim();
    return !!responsible && names.some((n) => n === responsible);
  }, [project, user]);

  const uploadSummaryNotificationSignedMutation = useMutation({
    mutationFn: (file: File) => projectService.uploadSummaryNotificationSigned(Number(id), file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', id] });
      message.success('通知单签字版上传成功');
    },
    onError: async (error: any) => {
      message.error(await getApiErrorMessage(error, '通知单签字版上传失败'));
    },
  });

  const deleteSummaryNotificationSignedMutation = useMutation({
    mutationFn: () => projectService.deleteSummaryNotificationSigned(Number(id)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', id] });
      message.success('通知单签字版已删除');
    },
    onError: async (error: any) => {
      message.error(await getApiErrorMessage(error, '删除通知单签字版失败'));
    },
  });

  const uploadSummaryThirdPartyFullMutation = useMutation({
    mutationFn: (file: File) => projectService.uploadSummaryThirdPartyFull(Number(id), file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', id] });
      message.success('第三方报告完整版 PDF 上传成功');
    },
    onError: async (error: any) => {
      message.error(await getApiErrorMessage(error, '第三方报告完整版上传失败，请上传 PDF 文件'));
    },
  });

  const deleteSummaryThirdPartyFullMutation = useMutation({
    mutationFn: () => projectService.deleteSummaryThirdPartyFull(Number(id)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', id] });
      message.success('第三方报告完整版已删除');
    },
    onError: async (error: any) => {
      message.error(await getApiErrorMessage(error, '删除第三方报告完整版失败'));
    },
  });

  const [summaryWordJob, setSummaryWordJob] = useState<WordExportJob | null>(null);
  const [summaryWordGenerating, setSummaryWordGenerating] = useState(false);
  const [overviewPreviewOpen, setOverviewPreviewOpen] = useState(false);
  const [overviewPreviewLoading, setOverviewPreviewLoading] = useState(false);
  const [overviewPreviewData, setOverviewPreviewData] = useState<ProjectOverviewPreview | null>(null);
  const [thirdPartyWordJob, setThirdPartyWordJob] = useState<WordExportJob | null>(null);
  const [thirdPartyWordGenerating, setThirdPartyWordGenerating] = useState(false);
  const isWordJobRunning = (job: WordExportJob | null) =>
    job?.status === 'PENDING' || job?.status === 'RUNNING';

  useEffect(() => {
    if (!id) return;
    let cancelled = false;
    (async () => {
      try {
        const latestSummary = await projectService.getLatestWordExportJob(Number(id), 'SUMMARY');
        if (!cancelled) setSummaryWordJob(latestSummary);
      } catch {
        if (!cancelled) setSummaryWordJob(null);
      }
      try {
        const latestThirdParty = await projectService.getLatestWordExportJob(Number(id), 'THIRD_PARTY');
        if (!cancelled) setThirdPartyWordJob(latestThirdParty);
      } catch {
        if (!cancelled) setThirdPartyWordJob(null);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [id]);

  const openOverviewPreview = async () => {
    if (!id) return;
    setOverviewPreviewOpen(true);
    setOverviewPreviewLoading(true);
    setOverviewPreviewData(null);
    try {
      const preview = await projectService.getOverviewPreview(Number(id));
      setOverviewPreviewData(preview);
    } catch (error) {
      message.error(await getApiErrorMessage(error, '概述预览加载失败'));
      setOverviewPreviewOpen(false);
    } finally {
      setOverviewPreviewLoading(false);
    }
  };

  const prepareSummaryWord = async () => {
    const key = 'word-summary-project';
    setSummaryWordGenerating(true);
    try {
      message.loading({ content: '正在创建导出任务...', key, duration: 0 });
      const created = await projectService.createWordExportJob(Number(id), { type: 'SUMMARY' });
      setSummaryWordJob(created);
      message.loading({ content: '后台正在生成项目总报告，请稍候...', key, duration: 0 });
      const finalJob = await waitForWordExportJob(Number(id), created.jobId);
      setSummaryWordJob(finalJob);
      if (finalJob.status === 'SUCCEEDED') {
        message.success({ content: '项目总报告已生成，可点击下载', key });
      } else {
        throw new Error(finalJob.errorMessage || '项目总报告生成失败');
      }
    } catch (error: any) {
      message.error({ content: await getApiErrorMessage(error, '项目总报告生成失败'), key });
    } finally {
      setSummaryWordGenerating(false);
    }
  };

  const downloadSummaryWord = async () => {
    if (!summaryWordJob || summaryWordJob.status !== 'SUCCEEDED') {
      message.warning('请先预生成并等待完成');
      return;
    }
    const key = 'word-summary-project-download';
    try {
      message.loading({ content: '正在下载项目总报告...', key, duration: 0 });
      const blob = await projectService.downloadWordExportJob(Number(id), summaryWordJob.jobId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = summaryWordJob.suggestedFileName || `${project?.projectName}_总报告.docx`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      message.success({ content: '项目总报告下载成功', key });
    } catch (error: any) {
      message.error({ content: await getApiErrorMessage(error, '项目总报告下载失败'), key });
    }
  };

  // 仅生成第三方样式单项正文 Word（无封面/概述）
  const runThirdPartyWordGeneration = async () => {
    const key = 'word-third-party-project';
    setThirdPartyWordGenerating(true);
    try {
      message.loading({ content: '正在创建导出任务...', key, duration: 0 });
      const created = await projectService.createWordExportJob(Number(id), { type: 'THIRD_PARTY' });
      setThirdPartyWordJob(created);
      message.loading({ content: '后台正在生成第三方报告，请稍候...', key, duration: 0 });
      const finalJob = await waitForWordExportJob(Number(id), created.jobId);
      setThirdPartyWordJob(finalJob);
      if (finalJob.status === 'SUCCEEDED') {
        message.success({ content: '第三方报告已生成，可点击下载', key });
      } else {
        throw new Error(finalJob.errorMessage || '第三方报告生成失败');
      }
    } catch (error: any) {
      message.error({ content: await getApiErrorMessage(error, '第三方报告生成失败'), key });
    } finally {
      setThirdPartyWordGenerating(false);
    }
  };

  const downloadThirdPartyWord = async () => {
    if (!thirdPartyWordJob || thirdPartyWordJob.status !== 'SUCCEEDED') {
      message.warning('请先预生成并等待完成');
      return;
    }
    const key = 'word-third-party-project-download';
    try {
      message.loading({ content: '正在下载第三方报告...', key, duration: 0 });
      const blob = await projectService.downloadWordExportJob(Number(id), thirdPartyWordJob.jobId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = thirdPartyWordJob.suggestedFileName || `${project?.projectName ?? '项目'}_第三方报告.docx`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      message.success({ content: '第三方报告下载成功', key });
    } catch (error: any) {
      message.error({ content: await getApiErrorMessage(error, '第三方报告下载失败'), key });
    }
  };

  const handleGenerateThirdPartyWord = () => {
    const num = project?.thirdPartyProjectNumber?.trim();
    const name = project?.thirdPartyName?.trim();
    if (!num || !name) {
      Modal.confirm({
        title: '提示',
        content:
          '未填写完整的第三方信息（项目编号（第三方）与第三方名称），是否仍要继续生成第三方报告？',
        okText: '继续生成',
        cancelText: '取消',
        onOk: () => runThirdPartyWordGeneration(),
      });
      return;
    }
    void runThirdPartyWordGeneration();
  };

  const patchThirdPartyDraft = (typeKey: string, patch: Partial<ThirdPartyApprovalFormRow>) => {
    setThirdPartyApprovalDraft((prev) => ({
      ...prev,
      [typeKey]: { ...(prev[typeKey] ?? emptyThirdPartyApprovalFormRow()), ...patch },
    }));
  };

  const openThirdPartyApprovalModal = () => {
    if (!project) return;
    const draft: Record<string, ThirdPartyApprovalFormRow> = {};
    const ids = project.selectedExperimentTypeIds ?? [];
    const byType = project.thirdPartyApprovalByExperimentType ?? {};
    for (const tid of ids) {
      const key = String(tid);
      draft[key] = parseThirdPartyRow(byType[key]);
    }
    setThirdPartyApprovalDraft(draft);
    setThirdPartyApprovalModalOpen(true);
  };

  const saveThirdPartyApproval = async () => {
    if (!project || !id) return;
    setThirdPartyApprovalSaving(true);
    try {
      const nextMap: Record<string, Record<string, string>> = {};
      const ids = project.selectedExperimentTypeIds ?? [];
      for (const tid of ids) {
        const key = String(tid);
        const d = thirdPartyApprovalDraft[key] ?? emptyThirdPartyApprovalFormRow();
        const row: Record<string, string> = {};
        const w = d.writer.trim();
        if (w) row.writer = w;
        if (d.writerDate) row.writerDate = d.writerDate.format('YYYY-MM-DD');
        const rv = d.reviewer.trim();
        if (rv) row.reviewer = rv;
        if (d.reviewDate) row.reviewDate = d.reviewDate.format('YYYY-MM-DD');
        const ap = d.approver.trim();
        if (ap) row.approver = ap;
        if (d.approvalDate) row.approvalDate = d.approvalDate.format('YYYY-MM-DD');
        const et = experimentTypes?.find((e) => e.id === tid);
        if (experimentTypeSupportsNdtLevelCode(et?.code)) {
          const wl = d.writerLevel.trim();
          if (wl) row.writerLevel = wl;
          const rl = d.reviewerLevel.trim();
          if (rl) row.reviewerLevel = rl;
        }
        if (Object.keys(row).length > 0) {
          nextMap[key] = row;
        }
      }
      const payload: UpdateProject = {
        projectNumber: project.projectNumber,
        thirdPartyProjectNumber: project.thirdPartyProjectNumber,
        thirdPartyName: project.thirdPartyName,
        projectName: project.projectName,
        projectType: project.projectType?.trim() || '',
        customer: project.customer,
        customerContact: project.customerContact,
        powerPlantId: project.powerPlantId,
        unitId: project.unitId,
        startDate: project.startDate,
        endDate: project.endDate ?? undefined,
        description: project.description,
        responsiblePerson: project.responsiblePerson,
        staff: project.staff,
        status: project.status,
        selectedExperimentTypeIds: project.selectedExperimentTypeIds,
        ndtSignatureLevels: project.ndtSignatureLevels,
        writerNdt: project.writerNdt,
        writerDateNdt: project.writerDateNdt,
        reviewerNdt: project.reviewerNdt,
        reviewDateNdt: project.reviewDateNdt,
        approverNdt: project.approverNdt,
        approvalDateNdt: project.approvalDateNdt,
        writerChem: project.writerChem,
        writerDateChem: project.writerDateChem,
        reviewerChem: project.reviewerChem,
        reviewDateChem: project.reviewDateChem,
        approverChem: project.approverChem,
        approvalDateChem: project.approvalDateChem,
        thirdPartyApprovalByExperimentType: nextMap,
      };
      await projectService.update(Number(id), payload);
      message.success('已保存第三方审批信息');
      setThirdPartyApprovalModalOpen(false);
      await queryClient.invalidateQueries({ queryKey: ['project', id] });
    } catch (error: any) {
      message.error(await getApiErrorMessage(error, '保存失败'));
    } finally {
      setThirdPartyApprovalSaving(false);
    }
  };

  const isProjectTypeMissing = !(project?.projectType?.trim());

  // 添加检测类型
  const handleAddExperimentType = () => {
    setShowAddTypeModal(true);
  };

  const handleConfirmAddType = async () => {
    if (!selectedTypeToAdd || !experimentTypes || !project) return;

    if (isProjectTypeMissing) {
      message.warning('请先到“编辑项目”页面设置项目类型');
      return;
    }

    const experimentType = experimentTypes.find(et => et.id === selectedTypeToAdd);
    if (!experimentType) return;

    // 检查是否已经添加过该类型
    const alreadyExists = activeExperimentTypes.some(aet => aet.id === selectedTypeToAdd);
    if (alreadyExists) {
      message.warning('该检测类型已经添加过了');
      return;
    }

    const newActiveType: ActiveExperimentType = {
      id: experimentType.id,
      name: experimentType.name,
      experimentType: experimentType
    };

    const newActiveTypes = [...activeExperimentTypes, newActiveType];
    
    // 保存原始状态用于错误回滚
    const originalActiveTypes = activeExperimentTypes;
    const originalCurrentType = currentActiveType;
    
    // 立即更新状态
    setActiveExperimentTypes(newActiveTypes);
    setCurrentActiveType(experimentType.id); // 直接激活新类型
    setReportRows(prev => ({
      ...prev,
      [experimentType.id]: []
    }));

    // 保存到数据库（携带完整项目字段，避免后端将未传字段覆盖为 null）
    try {
      await projectService.update(Number(id), {
        projectNumber: project.projectNumber,
        projectName: project.projectName,
        projectType: project.projectType?.trim() || '',
        customer: project.customer,
        customerContact: project.customerContact,
        powerPlantId: project.powerPlantId,
        unitId: project.unitId,
        startDate: project.startDate,
        endDate: project.endDate ?? undefined,
        description: project.description,
        responsiblePerson: project.responsiblePerson,
        staff: project.staff,
        status: project.status,
        selectedExperimentTypeIds: newActiveTypes.map(t => t.id),
        writerNdt: project.writerNdt,
        writerDateNdt: project.writerDateNdt,
        reviewerNdt: project.reviewerNdt,
        reviewDateNdt: project.reviewDateNdt,
        approverNdt: project.approverNdt,
        approvalDateNdt: project.approvalDateNdt,
        writerChem: project.writerChem,
        writerDateChem: project.writerDateChem,
        reviewerChem: project.reviewerChem,
        reviewDateChem: project.reviewDateChem,
        approverChem: project.approverChem,
        approvalDateChem: project.approvalDateChem,
        ndtSignatureLevels: project.ndtSignatureLevels,
        thirdPartyApprovalByExperimentType: project.thirdPartyApprovalByExperimentType,
      });
      message.success(`已添加${experimentType.name}检测类型`);
      setShowAddTypeModal(false);
      setSelectedTypeToAdd(null);
    } catch (error) {
      console.error('保存检测类型失败:', error);
      message.error('保存失败，请重试');
      // 失败时回滚状态
      setActiveExperimentTypes(originalActiveTypes);
      setCurrentActiveType(originalCurrentType);
      setReportRows(prev => {
        const updated = { ...prev };
        delete updated[experimentType.id];
        return updated;
      });
    }
  };

  // 切换激活的检测类型
  const handleSwitchActiveType = (typeId: number) => {
    setCurrentActiveType(typeId);
    setSelectedReportIds([]);
    setReportSearchKeyword('');
  };

  const handleSortReportsByComponentOrder = useCallback(() => {
    if (activeExperimentTypes.length === 0) {
      message.warning('请先添加检测类型');
      return;
    }
    setReportRows((prev) => {
      const next: Record<number, ReportRow[]> = {};
      for (const typeId of Object.keys(prev).map(Number)) {
        next[typeId] = sortReportsByComponentOrder(prev[typeId] ?? [], componentRankMap);
      }
      return next;
    });
    message.success('已按部件表顺序重排报告');
  }, [activeExperimentTypes.length, componentRankMap]);

  const openReportFiguresModal = useCallback(async () => {
    if (!id) return;
    try {
      const figures = await projectService.getReportFigures(Number(id));
      setProjectReportFigures(figures);
      setReportFiguresModalOpen(true);
    } catch (error) {
      message.error(await getApiErrorMessage(error, '加载报告附图失败'));
    }
  }, [id, getApiErrorMessage]);

  const saveProjectReportFigures = useCallback(async () => {
    if (!id) return;
    setReportFiguresSaving(true);
    try {
      await projectService.saveReportFigures(Number(id), projectReportFigures);
      setReportFiguresModalOpen(false);
      queryClient.invalidateQueries({ queryKey: ['project', id] });
      message.success('附图已保存，请重新预生成总报告/第三方报告后下载');
    } catch (error) {
      message.error(await getApiErrorMessage(error, '保存报告附图失败'));
    } finally {
      setReportFiguresSaving(false);
    }
  }, [id, projectReportFigures, queryClient, getApiErrorMessage]);

  // 添加报告行
  const handleAddReportRow = async () => {
    if (!currentActiveType) {
      message.warning('请先选择一个检测类型');
      return;
    }

    const activeType = activeExperimentTypes.find(aet => aet.id === currentActiveType);
    if (!activeType) {
      console.log('No activeType found for currentActiveType:', currentActiveType);
      return;
    }

    console.log('Adding report row for activeType:', activeType);

    const detectionContentConfig = getDetectionContentConfigByTypeId(currentActiveType, activeExperimentTypes);

    // 获取检测类型代码
    const experimentTypeCode = activeType.experimentType?.code;
    
    // 尝试获取默认设备
    let defaultInstrument: ProjectInstrument | null = null;
    if (experimentTypeCode && id) {
      try {
        defaultInstrument = await instrumentService.getDefaultInstrument(Number(id), experimentTypeCode);
      } catch (error) {
        console.log('No default instrument found for experiment type:', experimentTypeCode);
      }
    }

    const currentTypeRows = reportRows[currentActiveType] || [];
    const lastInspector = [...currentTypeRows]
      .reverse()
      .find((row) => row.id && row.inspector && row.inspector.trim() !== '' && row.inspector !== '/')
      ?.inspector;

    const newRow: ReportRow = {
      experimentTypeId: currentActiveType,
      title: `${activeType.name}检测报告`,
      isNew: true,
      isEditing: true,
      expanded: false,
      detectionData: buildTableDataPayload(alignPerContentRowToContentRowCount([{ rows: [] }], 1)),
      detectionContent: createDefaultDetectionContent(detectionContentConfig),
      detectionContentExpanded: false,
      imageAttachmentsExpanded: false,
      reportImage: '',
      hasDefect: '',
      summary: '',
      ...(lastInspector ? { inspector: lastInspector } : {}),
      // 如果找到默认设备，自动设置相关字段
      ...(defaultInstrument && {
        projectInstrumentId: defaultInstrument.id,
        equipment: defaultInstrument.instrumentName,
        equipmentName: defaultInstrument.instrumentName,
        instrumentModel: defaultInstrument.instrumentModel,
        instrumentNumber: defaultInstrument.instrumentNumber,
      }),
    };

    setReportRows(prev => {
      const merged = [...(prev[currentActiveType] || []), newRow];
      console.log('Updated reportRows for type', currentActiveType, ':', merged);
      return {
      ...prev,
        [currentActiveType]: merged
      };
    });
  };

  // 保存报告
  const handleSaveReport = async (
    typeId: number, 
    rowIndex: number, 
    rowData?: ReportRow,  // ✅ 新增参数:直接传递row数据，避免读取旧状态
    detectionDataOverride?: any,  // 整块 tableData 对应对象（含 perContentRow / rows）
    detectionContentOverride?: DetectionContentPayload | null,
  ) => {
    console.log('💾 [前端] handleSaveReport 开始:', { typeId, rowIndex, hasRowData: !!rowData });
    
    // ✅ 使用函数式更新来获取最新状态，确保读取到最新的 projectComponentId 和 projectInstrumentId
    // 这样可以避免 React 状态异步更新导致的问题
    let row: ReportRow | null = null;
    setReportRows(prev => {
      const rows = prev[typeId];
      if (!rows || rowIndex >= rows.length || !rows[rowIndex]) {
        console.error('❌ [前端] handleSaveReport: rows或row为空', { typeId, rowIndex, rowsLength: rows?.length });
        if (rowData) {
          row = rowData; // 兜底使用传入的值
        }
        return prev;
      }
      
      // ✅ 从最新状态读取
      const latestRow = rows[rowIndex];
      
      // ✅ 如果从最新状态读取到的 projectComponentId 或 projectInstrumentId 不是 undefined，使用最新状态
      // 否则，如果传入了 rowData，使用传入的值（但可能已经是旧值）
      if (latestRow.projectComponentId !== undefined || latestRow.projectInstrumentId !== undefined) {
        row = latestRow;
        console.log('✅ [前端] handleSaveReport: 从函数式更新获取到最新状态:', {
          id: row.id,
          projectComponentId: row.projectComponentId,
          projectInstrumentId: row.projectInstrumentId,
          componentName: row.componentName,
          instrumentModel: row.instrumentModel,
          instrumentNumber: row.instrumentNumber,
        });
      } else if (rowData) {
        // 如果最新状态中 ID 也是 undefined，使用传入的值（但记录警告）
        row = rowData;
        console.warn('⚠️ [前端] handleSaveReport: 最新状态中ID也为undefined，使用传入的rowData:', {
          id: rowData.id,
          projectComponentId: rowData.projectComponentId,
          projectInstrumentId: rowData.projectInstrumentId,
        });
      } else {
        // 如果都没有，使用最新状态（即使 ID 是 undefined）
        row = latestRow;
        console.warn('⚠️ [前端] handleSaveReport: 使用最新状态（ID可能为undefined）:', {
          id: latestRow.id,
          projectComponentId: latestRow.projectComponentId,
          projectInstrumentId: latestRow.projectInstrumentId,
        });
      }
      
      return prev; // 不实际更新状态，只是读取
    });
    
    // 如果函数式更新中未能获取到 row，使用兜底方案
    if (!row) {
      if (rowData) {
        row = rowData;
        console.warn('⚠️ [前端] handleSaveReport: 使用传入的rowData作为兜底', {
          id: rowData.id,
          projectComponentId: rowData.projectComponentId,
          projectInstrumentId: rowData.projectInstrumentId,
        });
      } else {
        // 最后尝试从直接读取状态（虽然可能还是旧值）
    const rows = reportRows[typeId];
        if (rows && rowIndex < rows.length && rows[rowIndex]) {
          row = rows[rowIndex];
          console.warn('⚠️ [前端] handleSaveReport: 使用兜底方案从状态读取（可能不是最新值）:', {
            id: row.id,
            projectComponentId: row.projectComponentId,
            projectInstrumentId: row.projectInstrumentId,
          });
        } else {
          console.error('❌ [前端] handleSaveReport: 无法获取row数据', { typeId, rowIndex });
          return;
        }
      }
    }
    
    // ✅ 确保 row 不为 null（TypeScript 类型保护）
    if (!row) {
      console.error('❌ [前端] handleSaveReport: row为null，无法继续', { typeId, rowIndex });
      return;
    }
    
    // ✅ 类型断言：row 不为 null
    const finalRow: ReportRow = row;
    const detectionContentConfig = getDetectionContentConfigByTypeId(typeId, activeExperimentTypes);
    const normalizedDetectionContent = normalizeDetectionContentValue(
      detectionContentOverride || finalRow.detectionContent,
      detectionContentConfig,
    );
    
    console.log('✅ [前端] handleSaveReport: 最终使用的row数据:', {
      id: finalRow.id,
      projectComponentId: finalRow.projectComponentId,
      projectInstrumentId: finalRow.projectInstrumentId,
      componentName: finalRow.componentName,
      instrumentModel: finalRow.instrumentModel,
      instrumentNumber: finalRow.instrumentNumber,
    });

    // 检查必填字段并标记验证错误
    const errors = new Set<string>();
    if (!finalRow.location || finalRow.location.trim() === '') {
      errors.add('location');
    }
    if (!finalRow.inspector || finalRow.inspector.trim() === '') {
      errors.add('inspector');
    }

    // 更新验证状态（用于显示红色边框）
    if (errors.size > 0) {
      setReportRows(prev => {
        const rows = prev[typeId];
        if (!rows) return prev;
        const newRows = [...rows];
        newRows[rowIndex] = { ...newRows[rowIndex], validationErrors: errors };
        return { ...prev, [typeId]: newRows };
      });
    }

    try {
      if (finalRow.isNew) {
        // 创建新报告
        // 构建报告项，包含检测数据
        const canonicalDetectionData = alignDetectionDataToContent(
          detectionDataOverride ?? finalRow.detectionData,
          normalizedDetectionContent,
        );
        const tableJson = JSON.stringify(canonicalDetectionData);
        const reportItems = [{
          experimentTypeId: typeId,
          tableData: tableJson,
          summary: finalRow.summary || ''
        }];

        // 调试日志：检查保存前的数据
        console.log('🔍 [前端] 保存报告前 - row数据:', {
          projectComponentId: finalRow.projectComponentId,
          projectComponentIdType: typeof finalRow.projectComponentId,
          projectInstrumentId: finalRow.projectInstrumentId,
          projectInstrumentIdType: typeof finalRow.projectInstrumentId,
          componentName: finalRow.componentName,
          instrumentModel: finalRow.instrumentModel,
          instrumentNumber: finalRow.instrumentNumber,
        });

        const compIdsForPayload =
          finalRow.projectComponentIds && finalRow.projectComponentIds.length > 0
            ? finalRow.projectComponentIds
            : finalRow.projectComponentId && finalRow.projectComponentId > 0
              ? [finalRow.projectComponentId]
              : [];

        const reportData: CreateReport = {
          projectId: Number(id),
          experimentTypeId: typeId,
          // ✅ 将 undefined 转换为 null，确保 JSON 序列化时包含该字段
          projectComponentId: compIdsForPayload.length > 0 ? compIdsForPayload[0] : null,
          projectComponentIds: compIdsForPayload.length > 1 ? compIdsForPayload : undefined,
          projectInstrumentId: (finalRow.projectInstrumentId && finalRow.projectInstrumentId > 0) ? finalRow.projectInstrumentId : null,
          title: `${activeExperimentTypes.find(aet => aet.id === typeId)?.name}-${dayjs().format('YYYYMMDD-HHmmss')}`,
          inspector: finalRow.inspector && finalRow.inspector.trim() !== '' ? finalRow.inspector : '/',
          testMethod: activeExperimentTypes.find(aet => aet.id === typeId)?.name,
          equipment: '',
          testStandard: '',
          componentName: finalRow.componentName || '',
          equipmentCategory: finalRow.equipmentCategory || '/',
          equipmentName: finalRow.equipmentName || '/',
          componentSpec: finalRow.componentSpec || '/',
          instrumentModel: finalRow.instrumentModel || '/',
          instrumentNumber: finalRow.instrumentNumber || '',
          testDate: finalRow.testDate || dayjs().format('YYYY-MM-DD'),
          location: finalRow.location && finalRow.location.trim() !== '' ? finalRow.location : '/',
          reportImage: finalRow.reportImage,
          hasDefect: finalRow.hasDefect,
          customFields: finalRow.customFields || {},
          imageAttachments: finalRow.imageAttachments || [],  // ✅ 直接作为报告字段发送
          reportItems: reportItems,
          detectionContent: normalizedDetectionContent,
        };

        // 调试日志：检查发送到后端的数据
        console.log('📤 [前端] 发送到后端的数据:', {
          projectComponentId: reportData.projectComponentId,
          projectComponentIdType: typeof reportData.projectComponentId,
          projectInstrumentId: reportData.projectInstrumentId,
          projectInstrumentIdType: typeof reportData.projectInstrumentId,
          componentName: reportData.componentName,
          instrumentModel: reportData.instrumentModel,
          instrumentNumber: reportData.instrumentNumber,
        });
        // 验证 JSON 序列化后的数据
        console.log('📦 [前端] JSON序列化后的数据:', JSON.stringify({
          projectComponentId: reportData.projectComponentId,
          projectInstrumentId: reportData.projectInstrumentId,
        }));

        const createdReport = await createReportMutation.mutateAsync(reportData);
        
        // ✅ 立即更新本地状态,包括检测数据，优先使用后端返回的数据
        setReportRows(prev => {
          const rows = prev[typeId];
          if (!rows) return prev;
        const newRows = [...rows];
          newRows[rowIndex] = { 
            ...newRows[rowIndex], 
            id: createdReport.id,
            isNew: false, 
            isEditing: false,
            createdAt: (createdReport as ReportList).createdAt ?? newRows[rowIndex].createdAt,
            // 优先使用后端返回的数据，如果没有则使用本地数据
            projectComponentId: createdReport.projectComponentId ?? finalRow.projectComponentId,
            projectComponentIds: (createdReport as ReportList).projectComponentIds ?? finalRow.projectComponentIds,
            projectInstrumentId: createdReport.projectInstrumentId ?? finalRow.projectInstrumentId,
            componentName: createdReport.componentName ?? finalRow.componentName,
            instrumentModel: createdReport.instrumentModel ?? finalRow.instrumentModel,
            instrumentNumber: createdReport.instrumentNumber ?? finalRow.instrumentNumber,
            hasDefect: createdReport.hasDefect ?? finalRow.hasDefect,  // 使用后端返回的hasDefect值（自动判断结果）
            detectionData: canonicalDetectionData,
            detectionContent: normalizedDetectionContent,
            validationErrors: errors.size > 0 ? errors : undefined
          };
          return { ...prev, [typeId]: newRows };
        });

        // 先同步 projectReports 缓存，避免本地新增后被旧缓存回写导致闪烁
        queryClient.setQueryData<ReportList[] | undefined>(['projectReports', id], (oldReports) => {
          const current = oldReports || [];
          const reportFromApi = createdReport as ReportList;
          const existed = current.some((item) => item.id === reportFromApi.id);
          if (existed) {
            return current.map((item) => (item.id === reportFromApi.id ? reportFromApi : item));
          }
          return [...current, reportFromApi];
        });
        
        // ✅ 后台刷新数据,确保与后端同步
        await queryClient.invalidateQueries({ queryKey: ['projectReports', id] });
        invalidateReportChangeLogQueries();

        const savedTypeCreate = activeExperimentTypes.find((aet) => aet.id === typeId);
        if (savedTypeCreate?.experimentType?.code === 'PMI' || savedTypeCreate?.experimentType?.code === 'AAT') {
          await refetchComponents();
        }

        message.success('报告创建成功' + (errors.size > 0 ? '，但部分必填字段为空' : ''));
      } else {
        // 更新现有报告
        // 构建报告项，包含检测数据
        const canonicalDetectionData = alignDetectionDataToContent(
          detectionDataOverride ?? finalRow.detectionData,
          normalizedDetectionContent,
        );
        const tableJson = JSON.stringify(canonicalDetectionData);
        const reportItems = [{
          experimentTypeId: typeId,
          tableData: tableJson,
          summary: finalRow.summary || ''
        }];

        // 调试日志：检查更新前的数据
        console.log('🔍 [前端] 更新报告前 - row数据:', {
          projectComponentId: finalRow.projectComponentId,
          projectInstrumentId: finalRow.projectInstrumentId,
          componentName: finalRow.componentName,
          instrumentModel: finalRow.instrumentModel,
          instrumentNumber: finalRow.instrumentNumber,
        });

        const compIdsUpdate =
          finalRow.projectComponentIds && finalRow.projectComponentIds.length > 0
            ? finalRow.projectComponentIds
            : finalRow.projectComponentId && finalRow.projectComponentId > 0
              ? [finalRow.projectComponentId]
              : [];

        const updateData = {
          projectId: Number(id),
          experimentTypeId: typeId,
          // ✅ 将 undefined 转换为 null，确保 JSON 序列化时包含该字段
          projectComponentId: compIdsUpdate.length > 0 ? compIdsUpdate[0] : null,
          projectComponentIds: compIdsUpdate.length > 1 ? compIdsUpdate : undefined,
          projectInstrumentId: (finalRow.projectInstrumentId && finalRow.projectInstrumentId > 0) ? finalRow.projectInstrumentId : null,
          title: finalRow.title,
          inspector: finalRow.inspector && finalRow.inspector.trim() !== '' ? finalRow.inspector : '/',
          equipment: finalRow.equipment || '/',
          testStandard: finalRow.testStandard || '/',
          componentName: finalRow.componentName || '',
          equipmentCategory: finalRow.equipmentCategory || '/',
          equipmentName: finalRow.equipmentName || '/',
          componentSpec: finalRow.componentSpec || '/',
          instrumentModel: finalRow.instrumentModel || '/',
          instrumentNumber: finalRow.instrumentNumber || '',
          testDate: finalRow.testDate || dayjs().format('YYYY-MM-DD'),
          location: finalRow.location && finalRow.location.trim() !== '' ? finalRow.location : '/',
          reportImage: finalRow.reportImage || '/',
          hasDefect: finalRow.hasDefect || '/',
          status: finalRow.status || '/',
          customFields: finalRow.customFields || {},
          imageAttachments: finalRow.imageAttachments || [],  // ✅ 直接作为报告字段发送
          reportItems: reportItems,  // ✅ 添加检测数据
          detectionContent: normalizedDetectionContent,
        };

        // 调试日志：检查发送到后端的数据
        console.log('📤 [前端] 发送到后端的数据 (更新):', {
          projectComponentId: updateData.projectComponentId,
          projectComponentIdType: typeof updateData.projectComponentId,
          projectInstrumentId: updateData.projectInstrumentId,
          projectInstrumentIdType: typeof updateData.projectInstrumentId,
          componentName: updateData.componentName,
          instrumentModel: updateData.instrumentModel,
          instrumentNumber: updateData.instrumentNumber,
        });
        // 验证 JSON 序列化后的数据
        console.log('📦 [前端] JSON序列化后的数据 (更新):', JSON.stringify({
          projectComponentId: updateData.projectComponentId,
          projectInstrumentId: updateData.projectInstrumentId,
        }));

        await reportService.update(finalRow.id!, updateData);
        
        // ✅ 立即重新获取报告详情以获取最新的 hasDefect 及后端同步后的 detectionContent（位置编号、总计）
        let updatedHasDefect: string | undefined = finalRow.hasDefect;
        let updatedReport: ReportDetail | null = null;
        try {
          updatedReport = await reportService.getById(finalRow.id!);
          updatedHasDefect = updatedReport.hasDefect;
        } catch (error) {
          console.warn('重新获取报告详情失败，使用原有hasDefect值:', error);
        }

        let savedDetectionContent =
          normalizeDetectionContentValue(updatedReport?.detectionContent, detectionContentConfig) ??
          normalizedDetectionContent;
        // 后端自动填充 detectionContent 时可能未带回 minRequiredThickness，用本次提交值补全
        if (
          detectionContentConfig.requireMinRequiredThickness &&
          savedDetectionContent.mode === 'table' &&
          normalizedDetectionContent.mode === 'table'
        ) {
          const sentRows = (normalizedDetectionContent as DetectionContentTablePayload).rows ?? [];
          const gotRows = (savedDetectionContent as DetectionContentTablePayload).rows ?? [];
          savedDetectionContent = {
            ...savedDetectionContent,
            rows: mergeSentTableRowMinThickness(gotRows, sentRows),
          };
        }
        const fromApiDetectionData = detectionDataFromReportItems(
          updatedReport?.reportItems,
          savedDetectionContent,
        );
        const savedDetectionData =
          detectionDataOverride != null && !hasNonEmptyDetectionBlocks(fromApiDetectionData)
            ? canonicalDetectionData
            : fromApiDetectionData ?? canonicalDetectionData;
        
        // ✅ 立即更新本地状态中的检测数据，使用后端同步后的 detectionContent 使位置编号、总计自动显示
        setReportRows(prev => {
          const rows = prev[typeId];
          if (!rows) return prev;
        const newRows = [...rows];
          newRows[rowIndex] = { 
            ...newRows[rowIndex], 
            isEditing: false,
            projectComponentId: updatedReport?.projectComponentId ?? finalRow.projectComponentId,
            projectComponentIds: updatedReport?.projectComponentIds ?? finalRow.projectComponentIds,
            projectInstrumentId: finalRow.projectInstrumentId,  // 保留仪器设备ID
            componentName: finalRow.componentName,  // 保留部件名称
            instrumentModel: finalRow.instrumentModel,  // 保留仪器型号
            instrumentNumber: finalRow.instrumentNumber,  // 保留仪器编号
            createdAt: updatedReport?.createdAt ?? finalRow.createdAt,
            hasDefect: updatedHasDefect,  // 使用重新获取的hasDefect值（自动判断结果）
            detectionData: savedDetectionData,
            detectionContent: savedDetectionContent,
          };
          return { ...prev, [typeId]: newRows };
        });

        // 先同步 projectReports 缓存，避免编辑后短暂回退到旧值
        queryClient.setQueryData<ReportList[] | undefined>(['projectReports', id], (oldReports) =>
          (oldReports || []).map((report) => {
            if (report.id !== finalRow.id) return report;
            return {
              ...report,
              ...updatedReport,
              ...updateData,
              id: finalRow.id,
              hasDefect: updatedHasDefect ?? report.hasDefect,
            } as ReportList;
          })
        );
        
        // ✅ 后台刷新数据,确保与后端同步
        await queryClient.invalidateQueries({ queryKey: ['projectReports', id] });
        invalidateReportChangeLogQueries();

        const savedTypeUpdate = activeExperimentTypes.find((aet) => aet.id === typeId);
        if (savedTypeUpdate?.experimentType?.code === 'PMI' || savedTypeUpdate?.experimentType?.code === 'AAT') {
          await refetchComponents();
        }

        message.success('报告更新成功');
      }
      
      // 已移动到上方,保存后立即刷新数据
    } catch (error) {
      console.error('保存报告失败:', error);
      message.error('保存失败，请重试');
    }
  };

  const isLeebHardnessType = useCallback(
    (typeId: number) => {
      const activeType = activeExperimentTypes.find((aet) => aet.id === typeId);
      const code = String(activeType?.experimentType?.code || '').trim().toUpperCase();
      const name = String(activeType?.experimentType?.name || activeType?.name || '').trim();
      return code === 'LHT' || code === 'LHD' || name.includes('里氏硬度');
    },
    [activeExperimentTypes],
  );

  const collectLeebCategoryRows = useCallback((
    detectionData: ParsedTableDataShape | null | undefined,
    detectionContent: DetectionContentPayload | null | undefined,
  ) => {
    const shouldShowLeebModalForType = (rawType: string): boolean => {
      const t = rawType.trim();
      if (!t) return false;
      // 兼容历史与现用文案：管件、对接焊缝、管件/对接焊缝
      return t.includes('管件') || t.includes('对接焊缝');
    };
    const contentRows =
      detectionContent &&
      (detectionContent.mode === 'table' || detectionContent.mode === 'sod') &&
      Array.isArray((detectionContent as { rows?: DetectionContentTableRow[] }).rows)
        ? ((detectionContent as { rows: DetectionContentTableRow[] }).rows)
        : [];
    const blocks = extractPerContentRowBlocks(detectionData);
    const sourceRows: Record<string, unknown>[] = [];
    const mapping: LeebCategoryMapping[] = [];
    blocks.forEach((block, blockIndex) => {
      const selectedType = String(contentRows[blockIndex]?.type ?? '').trim();
      if (!shouldShowLeebModalForType(selectedType)) {
        return;
      }
      const rows = Array.isArray(block?.rows) ? block.rows : [];
      rows.forEach((rawRow, rowIndex) => {
        const numberText = String((rawRow as Record<string, unknown>)['编号'] ?? '').trim();
        if (numberText && numberText !== '/') {
          sourceRows.push({ ...(rawRow as Record<string, unknown>) });
          mapping.push({ blockIndex, rowIndex });
        }
      });
    });
    return { sourceRows, mapping };
  }, []);

  const saveReportWithLeebCategoryFlow = useCallback(
    (
      typeId: number,
      rowIndex: number,
      rowData?: ReportRow,
      detectionDataOverride?: ParsedTableDataShape | null,
      detectionContentOverride?: DetectionContentPayload | null,
    ) => {
      const row = rowData ?? reportRows[typeId]?.[rowIndex];
      if (!row) {
        void handleSaveReport(typeId, rowIndex, rowData, detectionDataOverride, detectionContentOverride);
        return;
      }
      const detectionData = (detectionDataOverride ?? row.detectionData ?? null) as ParsedTableDataShape | null;
      const detectionContent = detectionContentOverride ?? row.detectionContent ?? null;
      if (!isLeebHardnessType(typeId)) {
        void handleSaveReport(typeId, rowIndex, row, detectionData, detectionContentOverride);
        return;
      }
      const { sourceRows, mapping } = collectLeebCategoryRows(detectionData, detectionContent);
      if (sourceRows.length === 0) {
        void handleSaveReport(typeId, rowIndex, row, detectionData, detectionContentOverride);
        return;
      }
      setLeebCategoryModalState({
        open: true,
        typeId,
        rowIndex,
        sourceRows,
        mapping,
        detectionData,
        rowData: row,
        detectionContentOverride,
      });
    },
    [collectLeebCategoryRows, handleSaveReport, isLeebHardnessType, reportRows],
  );

  // 删除报告
  const handleDeleteReport = (typeId: number, rowIndex: number) => {
    const rows = reportRows[typeId];
    if (!rows) return;

    const row = rows[rowIndex];
    
    if (row.id) {
      // 删除已保存的报告
      deleteReportMutation.mutate(row.id);
    }
    
    // 删除行
    const newRows = rows.filter((_, index) => index !== rowIndex);
    setReportRows(prev => ({ ...prev, [typeId]: newRows }));
  };


  const updateDetectionContent = (typeId: number, rowIndex: number, value: DetectionContentPayload) => {
    const normalized = normalizeDetectionContentValue(
      value,
      getDetectionContentConfigByTypeId(typeId, activeExperimentTypes),
    );
    setReportRows(prev => {
      const rows = prev[typeId];
      if (!rows || rowIndex >= rows.length) {
        return prev;
      }
      const row = rows[rowIndex];
      const nextDetectionData = alignDetectionDataToContent(row.detectionData, normalized);
      const newRows = [...rows];
      newRows[rowIndex] = {
        ...row,
        detectionContent: normalized,
        detectionData: nextDetectionData,
      };
      return { ...prev, [typeId]: newRows };
    });
  };

  // 更新报告字段值
  const updateReportField = (typeId: number, rowIndex: number, field: string, value: any) => {
    // ✅ 使用函数式更新，确保从最新状态读取
    setReportRows(prev => {
      const rows = prev[typeId];
      if (!rows || rowIndex >= rows.length) {
        console.warn('⚠️ [前端] updateReportField: rows为空或索引超出范围', { typeId, rowIndex, field, value });
        return prev;
      }

    const newRows = [...rows];
      const oldRow = newRows[rowIndex];
      const oldValue = (oldRow as any)[field];
      
      // ✅ 使用类型安全的方式更新字段
      const updatedRow: ReportRow = {
        ...oldRow,
        ...(field === 'projectComponentId' ? { projectComponentId: value as number | undefined } : {}),
        ...(field === 'projectComponentIds' ? { projectComponentIds: value as number[] | undefined } : {}),
        ...(field === 'projectInstrumentId' ? { projectInstrumentId: value as number | undefined } : {}),
        ...(field === 'componentName' ? { componentName: value as string | undefined } : {}),
        ...(field === 'instrumentModel' ? { instrumentModel: value as string | undefined } : {}),
        ...(field === 'instrumentNumber' ? { instrumentNumber: value as string | undefined } : {}),
        ...(field === 'inspector' ? { inspector: value as string | undefined } : {}),
        ...(field === 'location' ? { location: value as string | undefined } : {}),
        ...(field === 'testDate' ? { testDate: value as string | undefined } : {}),
        ...(field === 'hasDefect' ? { hasDefect: value as string | undefined } : {}),
        ...(field === 'summary' ? { summary: value as string | undefined } : {}),
        ...(field === 'title' ? { title: value as string } : {}),
        ...(field === 'status' ? { status: value as string | undefined } : {}),
        ...(field === 'equipmentCategory' ? { equipmentCategory: value as string | undefined } : {}),
        ...(field === 'equipmentName' ? { equipmentName: value as string | undefined } : {}),
        ...(field === 'componentSpec' ? { componentSpec: value as string | undefined } : {}),
        ...(field === 'testStandard' ? { testStandard: value as string | undefined } : {}),
        ...(field === 'equipment' ? { equipment: value as string | undefined } : {}),
        ...(field === 'testMethod' ? { testMethod: value as string | undefined } : {}),
        ...(field === 'detectionData' ? { detectionData: value } : {}),
        ...(field === 'imageAttachments' ? { imageAttachments: value as ImageAttachment[] | undefined } : {}),
        ...(field === 'customFields' ? { customFields: value as Record<string, any> | undefined } : {}),
      };
      
      if (field === 'hasDefect') {
        const activeTypeForDefect = activeExperimentTypes.find((a) => a.id === typeId);
        const code = activeTypeForDefect?.experimentType?.code;
        if (code && MANUAL_DEFECT_TYPE_CODES.has(code) && value === '否') {
          const overrides = {
            ...((updatedRow.customFields?._exportTextOverrides as Record<string, string>) || {}),
            overviewDefectLine: '',
          };
          updatedRow.customFields = {
            ...(updatedRow.customFields || {}),
            _exportTextOverrides: overrides,
          };
        }
      }

      newRows[rowIndex] = updatedRow;
      
      // 调试日志：确认状态更新
      if (field === 'projectComponentId' || field === 'projectComponentIds' || field === 'projectInstrumentId') {
        console.log(`🔄 [前端] updateReportField: ${field}`, { 
          typeId, 
          rowIndex, 
          oldValue, 
          newValue: value,
          updatedRow: newRows[rowIndex]
        });
      }

      return { ...prev, [typeId]: newRows };
    });
  };

  const syncExportTextDrawerForHasDefect = (
    typeId: number,
    rowIndex: number,
    hasDefect: string,
    rowSnapshot?: ReportRow,
  ) => {
    if (!exportTextDrawerOpen || !exportTextContext) return;
    if (exportTextContext.typeId !== typeId || exportTextContext.rowIndex !== rowIndex) return;
    const row = rowSnapshot ?? reportRows[typeId]?.[rowIndex];
    const showDefect = hasDefect === '是';
    setExportTextPreview((prev) => (prev != null ? { ...prev, showDefectSection: showDefect } : prev));
    if (!showDefect) {
      setExportTextFields((f) => ({ ...f, overviewDefectLine: '' }));
      return;
    }
    const contentRow = exportTextPerContentRowEnabled ? exportTextContentRowIndex : 0;
    const overrides = row?.customFields?._exportTextOverrides as
      | { overviewDefectLine?: string; byContentRow?: Record<string, { overviewDefectLine?: string }> }
      | undefined;
    let savedLine = '';
    if (overrides?.byContentRow?.[String(contentRow)]?.overviewDefectLine?.trim()) {
      savedLine = overrides.byContentRow[String(contentRow)].overviewDefectLine!.trim();
    } else if (!exportTextPerContentRowEnabled && overrides?.overviewDefectLine?.trim()) {
      savedLine = overrides.overviewDefectLine.trim();
    }
    if (savedLine) {
      setExportTextFields((f) => ({ ...f, overviewDefectLine: savedLine }));
      return;
    }
    const reportId = exportTextContext.reportId;
    if (reportId == null) {
      setExportTextFields((f) => ({ ...f, overviewDefectLine: '' }));
      return;
    }
    void reportService.getExportTextPreview(reportId, contentRow).then((p) => {
      const code =
        activeExperimentTypes.find((a) => a.id === typeId)?.experimentType?.code ?? '';
      const merged = mergeExportPreviewShowDefectFromLocalRow(p, code, hasDefect);
      setExportTextPreview({
        ...merged,
        showDefectSection: showDefect,
      });
      setExportTextFields((f) => ({
        ...f,
        overviewDefectLine: exportTextFieldsFromPreview(p).overviewDefectLine,
      }));
    }).catch((e) => {
      console.error(e);
    });
  };

  const updateHasDefectWithOverviewSync = (typeId: number, rowIndex: number, value: string) => {
    const rowSnapshot = reportRows[typeId]?.[rowIndex];
    updateReportField(typeId, rowIndex, 'hasDefect', value);
    syncExportTextDrawerForHasDefect(typeId, rowIndex, value, rowSnapshot);
  };

  const updateReportRowPartial = (typeId: number, rowIndex: number, partial: Partial<ReportRow>) => {
    setReportRows((prev) => {
      const rows = prev[typeId];
      if (!rows || rowIndex >= rows.length) return prev;
      const newRows = [...rows];
      newRows[rowIndex] = { ...newRows[rowIndex], ...partial };
      return { ...prev, [typeId]: newRows };
    });
  };

  const exportTextFieldsFromPreview = (p: ExportTextPreview) => ({
    detectionNarrativeBody:
      (p.detectionNarrativeBodySaved && p.detectionNarrativeBodySaved.trim()) !== ''
        ? p.detectionNarrativeBodySaved
        : p.detectionNarrativeBodyDefault || '',
    conclusionParagraph:
      (p.conclusionParagraphSaved && p.conclusionParagraphSaved.trim()) !== ''
        ? p.conclusionParagraphSaved
        : p.conclusionParagraphDefault || '',
    overviewWorkContentLine:
      (p.overviewWorkContentLineSaved && p.overviewWorkContentLineSaved.trim()) !== ''
        ? p.overviewWorkContentLineSaved
        : p.overviewWorkContentLineDefault || '',
    overviewDefectLine:
      (p.overviewDefectLineSaved && p.overviewDefectLineSaved.trim()) !== ''
        ? p.overviewDefectLineSaved
        : p.overviewDefectLineDefault || '',
  });

  const loadExportTextPreviewForRow = async (
    reportId: number,
    contentRowIndex: number,
    typeId: number,
    rowIndex: number,
  ) => {
    setExportTextLoading(true);
    try {
      const p = await reportService.getExportTextPreview(reportId, contentRowIndex);
      const code = activeExperimentTypes.find((a) => a.id === typeId)?.experimentType?.code ?? '';
      const local = reportRows[typeId]?.[rowIndex]?.hasDefect;
      const merged = mergeExportPreviewShowDefectFromLocalRow(p, code, local);
      setExportTextPreview(merged);
      setExportTextContentRowIndex(merged.contentRowIndex ?? contentRowIndex);
      setExportTextFields(exportTextFieldsFromPreview(merged));
    } catch (e) {
      console.error(e);
      message.error('加载预览失败');
    } finally {
      setExportTextLoading(false);
    }
  };

  const openExportTextDrawer = (reportId: number | null, typeId: number, rowIndex: number) => {
    setExportTextContext({ reportId, typeId, rowIndex });
    setExportTextDrawerOpen(true);
    setExportTextPreview(null);
    setExportTextContentRowIndex(0);
    setExportTextFields({
      detectionNarrativeBody: '',
      conclusionParagraph: '',
      overviewWorkContentLine: '',
      overviewDefectLine: '',
    });
    if (reportId == null) {
      setExportTextLoading(false);
      return;
    }
    void loadExportTextPreviewForRow(reportId, 0, typeId, rowIndex);
  };

  const handleExportTextContentRowChange = (contentRowIndex: number) => {
    if (!exportTextContext?.reportId) return;
    setExportTextContentRowIndex(contentRowIndex);
    void loadExportTextPreviewForRow(
      exportTextContext.reportId,
      contentRowIndex,
      exportTextContext.typeId,
      exportTextContext.rowIndex,
    );
  };

  const applyExportTextDefaults = async () => {
    if (!exportTextContext?.reportId) return;
    setExportTextLoading(true);
    try {
      await reportService.putExportTextOverrides(exportTextContext.reportId, {
        contentRowIndex: exportTextPerContentRowEnabled ? exportTextContentRowIndex : undefined,
        detectionNarrativeBody: '',
        conclusionParagraph: '',
        overviewWorkContentLine: '',
        overviewDefectLine: '',
      });
      const p = await reportService.getExportTextPreview(
        exportTextContext.reportId,
        exportTextContentRowIndex,
      );
      const code =
        activeExperimentTypes.find((a) => a.id === exportTextContext.typeId)?.experimentType?.code ?? '';
      const local = reportRows[exportTextContext.typeId]?.[exportTextContext.rowIndex]?.hasDefect;
      const merged = mergeExportPreviewShowDefectFromLocalRow(p, code, local);
      setExportTextPreview(merged);
      setExportTextFields({
        detectionNarrativeBody: merged.detectionNarrativeBodyDefault || '',
        conclusionParagraph: merged.conclusionParagraphDefault || '',
        overviewWorkContentLine: merged.overviewWorkContentLineDefault || '',
        overviewDefectLine: merged.overviewDefectLineDefault || '',
      });
      message.success('已恢复为自动生成文案');
    } catch (e) {
      console.error(e);
      message.error('恢复失败');
    } finally {
      setExportTextLoading(false);
    }
  };

  const saveExportTextOverrides = async () => {
    if (!exportTextContext?.reportId) return;
    setExportTextSaving(true);
    try {
      await reportService.putExportTextOverrides(exportTextContext.reportId, {
        contentRowIndex: exportTextPerContentRowEnabled ? exportTextContentRowIndex : undefined,
        detectionNarrativeBody: exportTextFields.detectionNarrativeBody,
        conclusionParagraph: exportTextFields.conclusionParagraph,
        overviewWorkContentLine: exportTextFields.overviewWorkContentLine,
        overviewDefectLine: exportTextFields.overviewDefectLine,
      });
      message.success('已保存导出文字覆盖');
      const { typeId, rowIndex } = exportTextContext;
      setReportRows((prev) => {
        const rows = prev[typeId];
        if (!rows?.[rowIndex]) return prev;
        const row = rows[rowIndex];
        const existing = (row.customFields?._exportTextOverrides || {}) as Record<string, unknown>;
        let overrides: Record<string, unknown>;
        if (exportTextPerContentRowEnabled) {
          const byContentRow = {
            ...((existing.byContentRow as Record<string, Record<string, string>>) || {}),
          };
          byContentRow[String(exportTextContentRowIndex)] = {
            detectionNarrativeBody: exportTextFields.detectionNarrativeBody,
            conclusionParagraph: exportTextFields.conclusionParagraph,
            overviewWorkContentLine: exportTextFields.overviewWorkContentLine,
            overviewDefectLine: exportTextFields.overviewDefectLine,
          };
          overrides = {
            ...existing,
            byContentRow,
          };
          delete overrides.detectionNarrativeBody;
          delete overrides.conclusionParagraph;
          delete overrides.overviewWorkContentLine;
          delete overrides.overviewDefectLine;
        } else {
          overrides = {
            ...existing,
            detectionNarrativeBody: exportTextFields.detectionNarrativeBody,
            conclusionParagraph: exportTextFields.conclusionParagraph,
            overviewWorkContentLine: exportTextFields.overviewWorkContentLine,
            overviewDefectLine: exportTextFields.overviewDefectLine,
          };
        }
        const next = [...rows];
        next[rowIndex] = {
          ...row,
          customFields: {
            ...(row.customFields || {}),
            _exportTextOverrides: overrides,
          },
        };
        return { ...prev, [typeId]: next };
      });
      setExportTextDrawerOpen(false);
      setExportTextContext(null);
      setExportTextPreview(null);
    } catch (e) {
      console.error(e);
      message.error('保存失败');
    } finally {
      setExportTextSaving(false);
    }
  };

  // 进入编辑模式
  const handleEnterEditMode = (typeId: number, rowIndex: number) => {
    const rows = reportRows[typeId];
    if (!rows) return;
    
    const newRows = [...rows];
    newRows[rowIndex] = { ...newRows[rowIndex], isEditing: true };
    setReportRows(prev => ({ ...prev, [typeId]: newRows }));
  };

  // 取消编辑
  const handleCancelEdit = (typeId: number, rowIndex: number) => {
    const rows = reportRows[typeId];
    if (!rows) return;
    
    const row = rows[rowIndex];
    if (row.isNew) {
      // 如果是新建的，直接删除
      handleDeleteReport(typeId, rowIndex);
    } else {
      // 如果是编辑的，恢复原始数据并退出编辑模式
      const newRows = [...rows];
      newRows[rowIndex] = { ...newRows[rowIndex], isEditing: false };
      setReportRows(prev => ({ ...prev, [typeId]: newRows }));
      // TODO: 从服务器重新加载原始数据
    }
  };

  /** 表格 rowKey 为行下标，将勾选的下标转为已保存报告的数据库 id */
  const resolveSelectedReportDbIds = useCallback((): number[] => {
    if (!currentActiveType) return [];
    const rows = reportRows[currentActiveType] ?? [];
    const dbIds: number[] = [];
    for (const key of selectedReportIds) {
      const idx = typeof key === 'number' ? key : Number(key);
      if (!Number.isFinite(idx) || idx < 0 || idx >= rows.length) {
        continue;
      }
      const row = rows[idx];
      if (row?.id != null && typeof row.id === 'number' && row.id > 0) {
        dbIds.push(row.id);
      }
    }
    return dbIds;
  }, [currentActiveType, reportRows, selectedReportIds]);

  // 批量操作函数
  const handleBatchDelete = async () => {
    if (selectedReportIds.length === 0) {
      message.warning('请选择要删除的报告');
      return;
    }

    const dbIds = resolveSelectedReportDbIds();
    if (dbIds.length !== selectedReportIds.length) {
      message.warning('所选包含未保存的行，请仅选择已保存的报告');
      return;
    }

    try {
      setBatchOperationLoading(true);
      await reportService.batchDelete(dbIds);
      message.success(`成功删除 ${dbIds.length} 个报告`);
      setSelectedReportIds([]);

      // 刷新数据
      queryClient.invalidateQueries({ queryKey: ['projectReports', id] });
      invalidateReportChangeLogQueries();
    } catch (error) {
      console.error('批量删除失败:', error);
      message.error('批量删除失败');
    } finally {
      setBatchOperationLoading(false);
    }
  };

  /** 合并生成正式单项 Word（与报告列表「合并生成」同一接口，无封面/概述） */
  const handleGenerateMergedFormalWord = async () => {
    if (selectedReportIds.length === 0) {
      message.warning('请选择要生成报告的行');
      return;
    }
    const dbIds = resolveSelectedReportDbIds();
    if (dbIds.length !== selectedReportIds.length) {
      message.warning('所选包含未保存的行，请先保存后再生成');
      return;
    }
    if (dbIds.length < 1) {
      message.warning('没有可生成的已保存报告');
      return;
    }

    try {
      setBatchOperationLoading(true);
      message.loading({
        content: '正在合并生成正式单项 Word...',
        key: 'word-merge-project',
        duration: 0,
      });
      const blob = await reportService.batchGenerateWordMerged(dbIds);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `合并单项报告_${dayjs().format('YYYYMMDDHHmmss')}.docx`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      message.success({ content: '合并 Word 生成成功！', key: 'word-merge-project' });
      setSelectedReportIds([]);
    } catch (error) {
      console.error('生成单项报告失败:', error);
      message.error({ content: '生成单项报告失败', key: 'word-merge-project' });
    } finally {
      setBatchOperationLoading(false);
    }
  };

  // 部件变化回调
  const handleComponentChange = () => {
    refetchComponents();
  };

  const handleInstrumentChange = () => {
    refetchInstruments();
  };

  // 获取当前激活类型的表格列
  const getCurrentTableColumns = () => {
    if (!currentActiveType) {
      console.log('No currentActiveType');
      return [];
    }

    const activeType = activeExperimentTypes.find(aet => aet.id === currentActiveType);
    if (!activeType || !activeType.experimentType) {
      console.log('No activeType or experimentType:', { activeType, currentActiveType });
      return [];
    }

    let reportFieldsSchema: ReportFieldsSchema;
    try {
      if (!activeType.experimentType.reportFieldsSchema) {
        console.log('No reportFieldsSchema for experiment type:', activeType.experimentType);
        // 降级方案：显示提示列
        return [{
          title: '提示',
          dataIndex: 'message',
          key: 'message',
          render: () => (
            <div style={{ color: '#ff4d4f', padding: '20px', textAlign: 'center' }}>
              检测类型配置缺失，请联系管理员或刷新页面重试（Ctrl+Shift+R）
            </div>
          ),
        }];
      }
      reportFieldsSchema = JSON.parse(activeType.experimentType.reportFieldsSchema);
      console.log('Parsed reportFieldsSchema:', reportFieldsSchema);
    } catch (error) {
      console.error('Invalid ReportFieldsSchema JSON:', error);
      // 降级方案：显示错误提示列
      return [{
        title: '错误',
        dataIndex: 'error',
        key: 'error',
        render: () => (
          <div style={{ color: '#ff4d4f', padding: '20px', textAlign: 'center' }}>
            检测类型配置格式错误，请联系管理员
          </div>
        ),
      }];
    }

    const columns: any[] = [];

    // 项目工作人员 + 当前表格已有检测人员（兼容历史数据）
    const staffOptions: string[] = (() => {
      const fromProject = parseStaff(project?.staff);
      const fromReports = (reportRows[currentActiveType] || []).flatMap((row) =>
        parseInspectorNames(row.inspector),
      );
      const merged = new Set<string>([...fromProject, ...fromReports]);
      return Array.from(merged).sort((a, b) => a.localeCompare(b, 'zh-CN'));
    })();

    /** 表格过滤后 Ant Design 传入的 index 可能不等于 reportRows 下标，统一用 record.key */
    const storeIdx = (record: ReportRow & { key?: number }, tableIndex: number): number =>
      typeof record.key === 'number' ? record.key : tableIndex;

    // 添加通用列（固定列）
    const commonColumns = [
      {
        title: '序号',
        dataIndex: 'serialNumber',
        key: 'serialNumber',
        width: 60,
        fixed: 'left' as const,
        render: (_: any, __: any, index: number) => index + 1,
      },
      {
        title: '部件名称',
        dataIndex: 'componentName',
        key: 'componentName',
        width: 200,
        fixed: 'left' as const,
        render: (value: any, record: ReportRow, index: number) => {
          const ri = storeIdx(record, index);
          if (!record.isEditing && !record.isNew) {
            const displayText =
              record.projectComponentId || (record.projectComponentIds && record.projectComponentIds.length > 0)
                ? formatMultiComponentDisplay(record.projectComponentIds, record.projectComponentId, components)
                : value || '-';
            return (
              <Tooltip title={displayText}>
                <span
                  style={{
                    display: 'block',
                    whiteSpace: 'normal',
                    wordBreak: 'break-word',
                    overflowWrap: 'anywhere',
                  }}
                >
                  {displayText}
                </span>
              </Tooltip>
            );
          }

          const componentOptions = components.map((comp) => ({
            label: formatComponentLabel(comp),
            value: comp.id,
          }));

          const selValue =
            record.projectComponentIds && record.projectComponentIds.length > 0
              ? record.projectComponentIds
              : record.projectComponentId
                ? [record.projectComponentId]
                : [];

          return (
            <Select
              mode="multiple"
              placeholder="可选多个同名称部件"
              showSearch
              autoClearSearchValue={false}
              optionFilterProp="label"
              value={selValue}
              onChange={(ids: number[]) => {
                if (!ids || ids.length === 0) {
                  updateReportRowPartial(currentActiveType!, ri, {
                    projectComponentId: undefined,
                    projectComponentIds: undefined,
                    componentName: '',
                    componentSpec: '',
                  });
                  return;
                }
                const selected = components.filter((c) => ids.includes(c.id));
                const nameSet = new Set(selected.map((c) => (c.componentName || '').trim()));
                if (nameSet.size > 1) {
                  message.error('所选部件名称必须一致');
                  return;
                }
                const specPreview = selected.map((c) => getComponentDisplaySpec(c)).filter(Boolean).join('/');
                const multiTargetRows = ids.length > 1 ? ids.length : null;

                if (multiTargetRows != null && currentActiveType != null) {
                  setReportRows((prev) => {
                    const rows = prev[currentActiveType];
                    if (!rows || ri >= rows.length) return prev;
                    const row = rows[ri];
                    let nextContent = row.detectionContent;
                    let nextData = row.detectionData;
                    if (
                      nextContent &&
                      typeof nextContent === 'object' &&
                      (nextContent.mode === 'table' || nextContent.mode === 'sod') &&
                      Array.isArray((nextContent as { rows?: unknown[] }).rows)
                    ) {
                      const resizedRows = sanitizeTableRowComponentIds(
                        resizeDetectionContentTableRows(
                          (nextContent as { rows: unknown[] }).rows,
                          multiTargetRows,
                        ),
                        ids,
                      );
                      nextContent =
                        nextContent.mode === 'sod'
                          ? ({ ...(nextContent as DetectionContentPayload), rows: resizedRows } as DetectionContentPayload)
                          : ({
                              ...(nextContent as DetectionContentTablePayload),
                              mode: 'table',
                              rows: resizedRows,
                            } as DetectionContentPayload);
                      nextData = buildTableDataPayload(
                        alignPerContentRowToContentRowCount(
                          extractPerContentRowBlocks(nextData as any),
                          multiTargetRows,
                        ),
                      );
                    }
                    const newRows = [...rows];
                    newRows[ri] = {
                      ...row,
                      projectComponentIds: ids.length > 1 ? ids : undefined,
                      projectComponentId: ids[0],
                      componentName: selected[0].componentName || '',
                      componentSpec: specPreview || '',
                      detectionContent: nextContent ?? row.detectionContent,
                      detectionData: nextData ?? row.detectionData,
                    };
                    return {
                      ...prev,
                      [currentActiveType!]: newRows,
                    };
                  });
                  return;
                }

                updateReportRowPartial(currentActiveType!, ri, {
                  projectComponentIds: ids.length > 1 ? ids : undefined,
                  projectComponentId: ids[0],
                  componentName: selected[0].componentName || '',
                  componentSpec: specPreview || '',
                });
              }}
              style={{ width: '100%' }}
              options={componentOptions}
              allowClear
              maxTagCount="responsive"
            />
          );
        },
      },
      {
        title: '仪器设备',
        dataIndex: 'projectInstrumentId',
        key: 'projectInstrumentId',
        width: 200,
        render: (value: any, record: ReportRow, index: number) => {
          const ri = storeIdx(record, index);
          if (!record.isEditing && !record.isNew) {
            const instrument = instruments.find(inst => inst.id === record.projectInstrumentId);
            if (instrument) {
              return `${instrument.instrumentName}${instrument.instrumentNumber ? ` (${instrument.instrumentNumber})` : ''}`;
            }
            return '-';
          }

          const instrumentOptions = instruments.map(inst => ({
            value: inst.id,
            label: `${inst.instrumentName}${inst.instrumentNumber ? ` (${inst.instrumentNumber})` : ''}`,
          }));

              return (
            <Select
              placeholder="请选择仪器设备"
                  value={value}
              onChange={(val) => {
                console.log('🔵 [前端] 选择仪器设备:', { instrumentId: val, instrumentIdType: typeof val, typeId: currentActiveType, rowIndex: ri });
                if (val === null || val === undefined) {
                  console.warn('⚠️ [前端] 仪器设备选择为null或undefined，清空关联');
                  updateReportField(currentActiveType, ri, 'projectInstrumentId', undefined);
                  updateReportField(currentActiveType, ri, 'instrumentModel', '');
                  updateReportField(currentActiveType, ri, 'instrumentNumber', '');
                  return;
                }
                const selectedInstrument = instruments.find(inst => inst.id === val);
                if (!selectedInstrument) {
                  console.warn('⚠️ [前端] 找不到ID为', val, '的仪器设备');
                  return;
                }
                updateReportField(currentActiveType, ri, 'projectInstrumentId', val);
                updateReportField(currentActiveType, ri, 'instrumentModel', selectedInstrument.instrumentModel || '');
                updateReportField(currentActiveType, ri, 'instrumentNumber', selectedInstrument.instrumentNumber || '');
                console.log('✅ [前端] 仪器设备选择后更新状态:', { 
                  projectInstrumentId: val, 
                  instrumentModel: selectedInstrument.instrumentModel,
                  instrumentNumber: selectedInstrument.instrumentNumber
                });
              }}
              allowClear
              style={{ width: '100%' }}
            >
              {instrumentOptions.map(opt => (
                <Option key={opt.value} value={opt.value}>
                  {opt.label}
                </Option>
              ))}
            </Select>
          );
        },
      },
      {
        title: '检测人员',
        dataIndex: 'inspector',
        key: 'inspector',
        width: 100,
        render: (value: any, record: ReportRow, index: number) => {
          const ri = storeIdx(record, index);
          const selectedNames = parseInspectorNames(typeof value === 'string' ? value : undefined);

          if (!record.isEditing && !record.isNew) {
            if (!value || value === '/') return '-';
            return value;
          }

          return (
            <Select
              mode="multiple"
              placeholder={staffOptions.length === 0 ? '请先在项目中添加工作人员' : '选择检测人员'}
              value={selectedNames}
              onChange={(names: string[]) => {
                const joined = names && names.length > 0 ? names.join('、') : '';
                updateReportField(currentActiveType, ri, 'inspector', joined);
              }}
              options={staffOptions.map((name) => ({ label: name, value: name }))}
              style={{ width: '100%' }}
              allowClear
              maxTagCount="responsive"
            />
          );
        },
      },
      {
        title: '检测时间',
        dataIndex: 'testDate',
        key: 'testDate',
      width: 120,
        render: (value: any, record: ReportRow, index: number) => {
        const ri = storeIdx(record, index);
        if (!record.isEditing && !record.isNew) {
            return value ? dayjs(value).format('YYYY-MM-DD') : '-';
          }
          
        return (
            <DatePicker
              placeholder="检测时间"
              value={value ? dayjs(value) : null}
              onChange={(date) => updateReportField(currentActiveType, ri, 'testDate', date?.format('YYYY-MM-DD') || '')}
              style={{ width: '100%' }}
          />
        );
      },
      },
      {
      title: '是否存在缺陷',
      dataIndex: 'hasDefect',
      key: 'hasDefect',
      width: 100,
      render: (value: string, record: ReportRow, index: number) => {
        const ri = storeIdx(record, index);
        const defectCode = activeType?.experimentType?.code;
        const isManualSelection =
          defectCode != null && MANUAL_DEFECT_TYPE_CODES.has(defectCode);
        
        if (isManualSelection) {
          // SOD和MET：用户手动选择，在编辑模式下显示单选按钮
          if (record.isEditing || record.isNew) {
            return (
              <div style={{ display: 'flex', gap: 8 }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: 4, fontSize: 12 }}>
                  <input
                    type="radio"
                    name={`hasDefect_${ri}`}
                    value="是"
                    checked={value === '是'}
                    onChange={(e) =>
                      updateHasDefectWithOverviewSync(currentActiveType!, ri, e.target.value)
                    }
                  />
                  是
                </label>
                <label style={{ display: 'flex', alignItems: 'center', gap: 4, fontSize: 12 }}>
                  <input
                    type="radio"
                    name={`hasDefect_${ri}`}
                    value="否"
                    checked={value === '否'}
                    onChange={(e) =>
                      updateHasDefectWithOverviewSync(currentActiveType!, ri, e.target.value)
                    }
                  />
                  否
                </label>
              </div>
            );
          } else {
            // 非编辑模式：显示文本
            return <span>{value || '-'}</span>;
          }
        } else {
          // 自动判断的检测类型：无论是否编辑，都显示图标（保存后自动判断）
          if (value === '否') {
            return <CheckCircleOutlined style={{ color: '#52c41a', fontSize: 16 }} />;
          } else if (value === '是') {
            return <CloseCircleOutlined style={{ color: '#ff4d4f', fontSize: 16 }} />;
          } else {
            // 未设置或空值：显示文本（保存后会自动判断）
            return <span>{value || '-'}</span>;
          }
        }
      },
      },
    ];

    // 添加通用列到表格
    columns.push(...commonColumns);

    // 注释掉：动态添加 reportFieldsSchema 中的列（这些列与 commonColumns 重复或不需要）
    // 原来的逻辑会添加：序号、设备类别、设备名称、部件规格、仪器型号、检测人员、检测地点、检测日期等
    // 这些字段已在 commonColumns 中定义，或者不需要显示
    /*
    console.log('reportFieldsSchema.fields:', reportFieldsSchema.fields);
    console.log('Number of fields:', reportFieldsSchema.fields?.length || 0);
    
    if (!reportFieldsSchema.fields || reportFieldsSchema.fields.length === 0) {
      console.error('No fields found in reportFieldsSchema!');
      // 降级方案：显示提示列
      return [{
        title: '提示',
        dataIndex: 'message',
        key: 'message',
        render: () => (
          <div style={{ color: '#ff4d4f', padding: '20px', textAlign: 'center' }}>
            检测类型字段配置为空，请刷新页面重试（Ctrl+Shift+R）
          </div>
        ),
      }];
    }
    
    reportFieldsSchema.fields.forEach(field => {
      columns.push({
        title: field.label,
        dataIndex: field.name,
        key: field.name,
        width: field.type === 'button' ? 100 : field.type === 'radio' ? 120 : 120,
        render: (value: any, record: ReportRow, index: number) => {
          // ... 完整的 render 逻辑
      },
    });
    });
    */

    const isVisualInspectionType = activeType?.experimentType?.code === 'VIS';

    // 检测内容、附图与检测数据（合并为一列入口）
    columns.push({
      title: '检测内容与数据',
      key: 'detectionContentAndImages',
      width: 120,
      render: (_: any, record: ReportRow, index: number) => {
        const ri = storeIdx(record, index);
        const isExpanded = isVisualInspectionType
          ? !!record.detectionContentExpanded
          : (record.detectionContentExpanded || record.imageAttachmentsExpanded);
        return (
          <Tooltip title={isExpanded ? '收起检测内容与数据详情' : '展开检测内容与数据'}>
            <Button
              size="small"
              type={isExpanded ? 'primary' : 'default'}
              icon={isExpanded ? <UpOutlined /> : <DownOutlined />}
              onClick={() => {
                const newExpanded = !isExpanded;
                const rows = reportRows[currentActiveType!];
                if (!rows) return;
                const newRows = [...rows];
                newRows[ri] = {
                  ...newRows[ri],
                  detectionContentExpanded: newExpanded,
                  imageAttachmentsExpanded: isVisualInspectionType ? false : newExpanded,
                };
                setReportRows(prev => ({ ...prev, [currentActiveType!]: newRows }));
              }}
              style={
                {...(isExpanded
                  ? undefined
                  : { borderColor: '#d3adf7', color: '#531dab' }),
                  width: 80
              }}
            >
              {isExpanded ? '收起' : '展开'} 
            </Button>
          </Tooltip>
        );
      },
    });

    // 已删除：重复的"是否存在缺陷"列（已在 commonColumns 中定义）

    columns.push({
      title: '操作',
      key: 'action',
      width: 200,
      render: (_: any, record: ReportRow, index: number) => {
        const ri = storeIdx(record, index);
        return (
        <Space size="small">
          {record.isNew || record.isEditing ? (
            <>
              <Button
                type="link"
                size="small"
                icon={<SaveOutlined />}
                onClick={() => {
                  // ✅ 使用函数式更新来获取最新状态，确保读取到最新的 projectComponentId 和 projectInstrumentId
                  // 通过 setReportRows 的函数式更新来"读取"最新值，但不实际更新状态
                  setReportRows(prev => {
                    const latestRows = prev[currentActiveType];
                    const latestRow = latestRows && latestRows[ri] ? latestRows[ri] : record;
                    
                    console.log('🔘 [前端] 点击保存按钮 (函数式更新获取最新状态):', { 
                      currentActiveType, 
                      index: ri, 
                      recordId: record.id,
                      recordProjectComponentId: record.projectComponentId,
                      recordProjectInstrumentId: record.projectInstrumentId,
                      latestProjectComponentId: latestRow.projectComponentId,
                      latestProjectInstrumentId: latestRow.projectInstrumentId,
                      latestRowData: latestRow,
                    });
                    
                    // ✅ 使用从最新状态读取的数据（通过函数式更新获取）
                    saveReportWithLeebCategoryFlow(currentActiveType, ri, latestRow);
                    
                    // 返回原始状态，不实际更新
                    return prev;
                  });
                }}
              >
                保存
              </Button>
              <Button
                type="link"
                size="small"
                onClick={() => handleCancelEdit(currentActiveType, ri)}
              >
                取消
              </Button>
            </>
          ) : (
            <>
              <Button
                type="link"
                size="small"
                icon={<EditOutlined />}
                onClick={() => handleEnterEditMode(currentActiveType, ri)}
              >
                编辑
              </Button>
              <Popconfirm
                title="确认删除"
                description="确定要删除这个报告吗？"
                onConfirm={() => handleDeleteReport(currentActiveType, ri)}
                okText="确定"
                cancelText="取消"
              >
                <Button type="link" size="small" danger icon={<DeleteOutlined />}>
                  删除
                </Button>
              </Popconfirm>
            </>
          )}
        </Space>
        );
      },
    });

    // 已删除：重复的"操作"列（第二个，保留第一个操作列定义）

    return columns;
  };

  if (isLoading || !experimentTypes) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <div>加载中...</div>
      </div>
    );
  }

  if (experimentTypesError) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <div style={{ color: '#ff4d4f', marginBottom: '16px' }}>加载检测类型失败</div>
        <Button onClick={() => window.location.reload()}>
          刷新页面
        </Button>
      </div>
    );
  }

  if (projectError) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <div style={{ color: '#ff4d4f', marginBottom: '16px' }}>
          {(projectError as any)?.response?.status === 404 ? '项目不存在或已被删除' : '加载项目详情失败'}
        </div>
        <Button onClick={() => navigate('/projects')}>
          返回项目列表
        </Button>
      </div>
    );
  }

  if (projectReportsError) {
    // 项目报告加载失败不应该阻止页面显示，只显示警告
    console.warn('项目报告加载失败:', projectReportsError);
  }

  if (!project) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <div style={{ color: '#ff4d4f', marginBottom: '16px' }}>项目不存在</div>
        <Button onClick={() => navigate('/projects')}>
          返回项目列表
        </Button>
      </div>
    );
  }

  return (
    <ErrorBoundary>
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/projects')}>
          返回项目列表
        </Button>
      </Space>

      <Title level={2}>项目详情</Title>

      <Card 
        title={
          <span>
            项目信息{'  '}
            <Tag color={project.status === 'Completed' ? 'success' : 'processing'} style={{ marginLeft: 18 }}>
              {project.status === 'Completed' ? '已完成' : '进行中'}
            </Tag>
          </span>
        } 
        style={{ marginBottom: 16,borderRadius: 12, boxShadow: '0 6px 20px rgba(0,0,0,0.06)',
          border: '1px solid #f0f0f0',
          overflow: 'hidden' }}
      >
        <Descriptions column={2} bordered className="project-detail-info-descriptions">
          <Descriptions.Item label="项目编号">
            {project.projectNumber}
          </Descriptions.Item>
          <Descriptions.Item label="项目名称">
            {project.projectName}
          </Descriptions.Item>
          
          <Descriptions.Item label="项目编号（第三方）">
            {project.thirdPartyProjectNumber?.trim() || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="第三方名称">
            {project.thirdPartyName?.trim() || '-'}
          </Descriptions.Item>    
          <Descriptions.Item label="客户方">
            {project.customer || '未设置'}
          </Descriptions.Item>
          <Descriptions.Item label="机组号">
            {project.unitNumber || '未设置'}
          </Descriptions.Item>
          <Descriptions.Item label="客户方人员">
            {project.customerContact || '未设置'}
          </Descriptions.Item>

          <Descriptions.Item label="项目类型">
            {project.projectType?.trim() || '未设置'}
          </Descriptions.Item>
          <Descriptions.Item label="开始日期">
            {dayjs(project.startDate).format('YYYY-MM-DD')}
          </Descriptions.Item>
          <Descriptions.Item label="结束日期">
            {project.endDate ? dayjs(project.endDate).format('YYYY-MM-DD') : '未设置'}
          </Descriptions.Item>
          <Descriptions.Item label="项目负责人">
            {formatOptionalText(project.responsiblePerson)}
          </Descriptions.Item>
          <Descriptions.Item label="工作人员">
            {(() => {
              const list = parseStaff(project.staff);
              return list.length > 0 ? list.join('、') : '-';
            })()}
          </Descriptions.Item>
          <Descriptions.Item label="创建时间">
            {dayjs(project.createdAt).format('YYYY-MM-DD HH:mm:ss')}
          </Descriptions.Item>
          {project.description && (
            <Descriptions.Item label="项目描述" span={2}>
              {project.description}
            </Descriptions.Item>
          )}
        </Descriptions>

        <div style={{ marginTop: 16, width: '100%' }}>
          <div
            style={{
              width: '100%',
              marginBottom: 16,
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              gap: 12,
              flexWrap: 'wrap',
            }}
          >
            <Space wrap size={[12, 12]}>
              <Button
                icon={<EditOutlined />}
                onClick={() => navigate(`/projects/${id}/edit`)}
                style={PROMINENT_ACTION_BTN_SHADOW}
              >
                编辑项目
              </Button>
              <Tooltip title="按检测类型维护编写/审核/批准及日期，用于第三方单项 Word 签批区">
                <Button onClick={openThirdPartyApprovalModal} style={PROMINENT_ACTION_BTN_SHADOW}>
                  第三方审批信息
                </Button>
              </Tooltip>
              <Tooltip title="仅导出第三方样式单项正文，无封面与概述">
                <Space size={8}>
                  <Button
                    icon={<FileWordOutlined />}
                    onClick={handleGenerateThirdPartyWord}
                    loading={thirdPartyWordGenerating}
                    disabled={thirdPartyWordGenerating || isWordJobRunning(thirdPartyWordJob)}
                    style={PROMINENT_ACTION_BTN_SHADOW}
                  >
                    预生成第三方报告
                  </Button>
                  <Button
                    onClick={downloadThirdPartyWord}
                    disabled={thirdPartyWordJob?.status !== 'SUCCEEDED'}
                  >
                    下载
                  </Button>
                  <Tag color={
                    thirdPartyWordGenerating || isWordJobRunning(thirdPartyWordJob) ? 'processing' :
                      thirdPartyWordJob?.status === 'SUCCEEDED' ? 'success' :
                        thirdPartyWordJob?.status === 'FAILED' ? 'error' : 'default'
                  }>
                    {thirdPartyWordGenerating || isWordJobRunning(thirdPartyWordJob)
                      ? '后台生成中'
                      : thirdPartyWordJob?.status === 'SUCCEEDED'
                        ? '已生成可下载'
                        : thirdPartyWordJob?.status === 'FAILED'
                          ? (thirdPartyWordJob.errorMessage || '生成失败')
                          : '未生成'}
                  </Tag>
                </Space>
              </Tooltip>
            </Space>

            <Tooltip title="总报告将保留系统生成的封面、项目信息、概述与润电单项正文">
              <Space size={8}>
                <Tooltip title="与检测日志「总日志」中调整顺序相同，影响概述与总报告条目顺序">
                  <Button
                    onClick={() => setReportOrderModalOpen(true)}
                    disabled={(projectReports?.length ?? 0) === 0}
                    style={PROMINENT_ACTION_BTN_SHADOW}
                  >
                    调整顺序
                  </Button>
                </Tooltip>
                <Tooltip title="按当前概述逻辑预览摘要与第 1～3 章（只读，不生成 Word）">
                  <Button
                    onClick={openOverviewPreview}
                    loading={overviewPreviewLoading}
                    disabled={overviewPreviewLoading}
                    style={PROMINENT_ACTION_BTN_SHADOW}
                  >
                    概述预览
                  </Button>
                </Tooltip>
                <Button
                  type="primary"
                  icon={<FileWordOutlined />}
                  onClick={prepareSummaryWord}
                  loading={summaryWordGenerating}
                  disabled={summaryWordGenerating || isWordJobRunning(summaryWordJob)}
                >
                  预生成总报告
                </Button>
                <Button
                  onClick={downloadSummaryWord}
                  disabled={summaryWordJob?.status !== 'SUCCEEDED'}
                >
                  下载
                </Button>
                <Tag color={
                  summaryWordGenerating || isWordJobRunning(summaryWordJob) ? 'processing' :
                    summaryWordJob?.status === 'SUCCEEDED' ? 'success' :
                      summaryWordJob?.status === 'FAILED' ? 'error' : 'default'
                }>
                  {summaryWordGenerating || isWordJobRunning(summaryWordJob)
                    ? '后台生成中'
                    : summaryWordJob?.status === 'SUCCEEDED'
                      ? '已生成可下载'
                      : summaryWordJob?.status === 'FAILED'
                        ? (summaryWordJob.errorMessage || '生成失败')
                        : '未生成'}
                </Tag>
              </Space>
            </Tooltip>
          </div>

          <Row gutter={[16, 16]}>
            <Col xs={24} md={12}>
              <Card
                size="small"
                styles={{ body: { padding: 14 } }}
                style={{ borderRadius: 28 }}
              >
                <Space direction="vertical" size={10} style={{ width: '100%' }}>
                  <Upload
                    accept=".docx,.pdf"
                    showUploadList={false}
                    beforeUpload={(file) => {
                      const err = validateSummaryNotificationFile(file as File);
                      if (err) {
                        message.error(err);
                        return Upload.LIST_IGNORE;
                      }
                      uploadSummaryNotificationSignedMutation.mutate(file as File);
                      return Upload.LIST_IGNORE;
                    }}
                    disabled={uploadSummaryNotificationSignedMutation.isPending}
                  >
                    <Button
                      icon={<UploadOutlined />}
                      loading={uploadSummaryNotificationSignedMutation.isPending}
                      style={{
                        height: 32,
                        paddingInline: 14,
                        borderRadius: 28,
                        ...PROMINENT_ACTION_BTN_SHADOW,
                      }}
                    >
                      上传通知单签字版PDF
                    </Button>
                  </Upload>
                  {project?.summaryNotificationSignedOriginalName ? (
                    <Space wrap size={[8, 8]}>
                      <Tag color="blue" style={{ borderRadius: 18 }}>{project.summaryNotificationSignedOriginalName}</Tag>
                      <Popconfirm
                        title="确定删除通知单签字版吗？"
                        onConfirm={() => deleteSummaryNotificationSignedMutation.mutate()}
                        
                      >
                        <Button
                          icon={<DeleteOutlined />}
                          loading={deleteSummaryNotificationSignedMutation.isPending}
                          style={{ borderRadius: 18 }}
                          size='small'
                        >
                          删除
                        </Button>
                      </Popconfirm>
                    </Space>
                  ) : (
                    <Tag>未上传</Tag>
                  )}
                </Space>
              </Card>
            </Col>

            <Col xs={24} md={12}>
              <Card
                size="small"
                styles={{ body: { padding: 14 } }}
                style={{ borderRadius: 28 }}
              >
                <Space direction="vertical" size={10} style={{ width: '100%' }}>
                  <Upload
                    accept=".pdf"
                    showUploadList={false}
                    beforeUpload={(file) => {
                      const err = validateThirdPartyFullFile(file as File);
                      if (err) {
                        message.error(err);
                        return Upload.LIST_IGNORE;
                      }
                      uploadSummaryThirdPartyFullMutation.mutate(file as File);
                      return Upload.LIST_IGNORE;
                    }}
                    disabled={uploadSummaryThirdPartyFullMutation.isPending}
                  >
                    <Button
                      icon={<UploadOutlined />}
                      loading={uploadSummaryThirdPartyFullMutation.isPending}
                      style={{
                        height: 32,
                        paddingInline: 14,
                        borderRadius: 28,
                        ...PROMINENT_ACTION_BTN_SHADOW,
                      }}
                    >
                      上传第三方报告完整版PDF
                    </Button>
                  </Upload>
                  {project?.summaryThirdPartyFullOriginalName ? (
                    <Space wrap size={[8, 8]}>
                      <Tag color="purple" style={{ borderRadius: 18 }}>{project.summaryThirdPartyFullOriginalName}</Tag>
                      <Popconfirm
                        title="确定删除第三方报告完整版吗？"
                        onConfirm={() => deleteSummaryThirdPartyFullMutation.mutate()}
                      >
                        <Button
                          icon={<DeleteOutlined />}
                          loading={deleteSummaryThirdPartyFullMutation.isPending}
                          style={{ borderRadius: 18 }}
                          size='small'
                        >
                          删除
                        </Button>
                      </Popconfirm>
                    </Space>
                  ) : (
                    <Tag>未上传 PDF</Tag>
                  )}
                </Space>
              </Card>
            </Col>
          </Row>
        </div>
      </Card>

      {/* 项目统计信息 */}
      {projectStats && (
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={6}>
            <Card className="project-section-card">
              <Statistic
                title="报告总数"
                value={projectStats.totalReports}
                prefix={<FileTextOutlined />}
                suffix="个"
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card className="project-section-card">
              <Statistic
                title="缺陷总数"
                value={projectStats.defectCount}
                prefix={<CloseCircleOutlined />}
                suffix="个缺陷"
                valueStyle={{ color: '#cf1322' }}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card className="project-section-card">
              <Statistic
                title="工作总数"
                value={projectStats.workDays}
                prefix={<ClockCircleOutlined />}
                suffix="天"
                valueStyle={{ color: '#3f8600' }}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card className="project-section-card">
              <Statistic
                title="检测类型总计"
                value={projectStats.detectionTypeTotal}
                prefix={<BarChartOutlined />}
                suffix="种"
              />
            </Card>
          </Col>
        </Row>
      )}

      {(projectReports?.length ?? 0) > 0 && (
        <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'flex-end' }}>
          <Button type="default" onClick={() => setReportOrderModalOpen(true)}>
            调整报告顺序
          </Button>
        </div>
      )}

      <Tabs
        className="project-detail-section-tabs"
        activeKey={detailSectionTab}
        onChange={(k) => setDetailSectionTab(k as DetailSectionTabKey)}
        destroyInactiveTabPane={false}
        size="large"
        items={[
          {
            key: 'detectionLog',
            label: '检测日志',
            children: (
              <DetectionLog
                reports={projectReports || []}
                components={projectComponents || []}
                projectId={Number(id)}
                projectName={project?.projectName}
                aggregateDetectionLogOrderJson={project?.aggregateDetectionLogOrder}
                onOpenReportOrderModal={() => setReportOrderModalOpen(true)}
                logScrollMaxHeight="min(78vh, 920px)"
              />
            ),
          },
          {
            key: 'components',
            label: '检测部件列表',
            children: (
              <Card className="project-section-card project-inline-list-card">
                <ProjectComponentsTable
                  projectId={Number(id)}
                  reports={projectReports ?? []}
                  onComponentChange={handleComponentChange}
                />
              </Card>
            ),
          },
          {
            key: 'instruments',
            label: '仪器设备列表',
            children: (
              <Card className="project-section-card project-inline-list-card">
                <ProjectInstrumentsTable
                  projectId={Number(id)}
                  onInstrumentChange={handleInstrumentChange}
                />
              </Card>
            ),
          },
          {
            key: 'reports',
            label: '项目报告列表',
            children: (
      <Card className="project-section-card"
        title={
          <Space size={36} align="center" wrap>
            <span style={{ fontSize: 18, fontWeight: 600 }}>项目报告列表</span>
            <Input.Search
              allowClear
              placeholder="搜索部件名称、检测人员…"
              style={{ width: 260 }}
              value={reportSearchKeyword}
              onChange={(e) => setReportSearchKeyword(e.target.value)}
            />
            <Button
              type="primary"
              size="small"
              icon={<PictureOutlined />}
              onClick={() => void openReportFiguresModal()}
            >
              报告附图
            </Button>
            <Button
              type="primary"
              size="small"
              icon={<SortAscendingOutlined />}
              onClick={handleSortReportsByComponentOrder}
            >
              报告自动排序
            </Button>
            <Tooltip title="按报告创建时间（当天）筛选；再次点击恢复显示全部检测类型下的报告">
              <Button
                type={showTodayCreatedReportsOnly ? 'primary' : 'default'}
                size="small"
                icon={<FilterOutlined />}
                onClick={() => setShowTodayCreatedReportsOnly((v) => !v)}
                style={PROMINENT_ACTION_BTN_SHADOW}
              >
                {showTodayCreatedReportsOnly ? '显示全部' : '仅看今日新建'}
              </Button>
            </Tooltip>
          </Space>
        }
        extra={
          <Space>
            {selectedReportIds.length > 0 && (
              <>
                <Popconfirm
                  title="确认删除"
                  description={`确定要删除选中的 ${selectedReportIds.length} 个报告吗？`}
                  onConfirm={() => void handleBatchDelete()}
                  okText="确定"
                  cancelText="取消"
                >
                  <Button
                    danger
                    icon={<DeleteOutlined />}
                    loading={batchOperationLoading}
                  >
                    批量删除 ({selectedReportIds.length})
                  </Button>
                </Popconfirm>
                <Button
                  type="primary"
                  icon={<FileWordOutlined />}
                  onClick={() => void handleGenerateMergedFormalWord()}
                  loading={batchOperationLoading}
                >
                  生成单项报告 ({selectedReportIds.length})
                </Button>
              </>
            )}
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={handleAddExperimentType}
          >
            添加检测类型
          </Button>
          </Space>
        }
      >
        {/* 检测类型按钮行 */}
        <div style={{ marginBottom: 16 }}>
          <Space wrap>
            {activeExperimentTypes.map(type => (
              <Button
                key={type.id}
                type={currentActiveType === type.id ? 'primary' : 'default'}
                onClick={() => handleSwitchActiveType(type.id)}
              >
                {type.name}
              </Button>
            ))}
            {activeExperimentTypes.length === 0 && (
              <div style={{ color: '#999' }}>暂无检测类型，请点击"添加检测类型"</div>
            )}
          </Space>
        </div>


        {/* 报告数据表格 */}
        {currentActiveType && (
          <div>
            <Table
              className="report-detail-table"
              columns={getCurrentTableColumns()}
              dataSource={reportSearchFilteredRows.map(({ row, storeIndex }) => ({
                ...row,
                key: storeIndex,
              }))}
              locale={{
                emptyText:
                  reportSearchKeyword.trim() && reportSearchFilteredRows.length === 0
                    ? '无匹配报告'
                    : showTodayCreatedReportsOnly && reportSearchFilteredRows.length === 0
                      ? '今日暂无新建报告（未保存的新行仍会显示）'
                      : undefined,
              }}
              rowKey="key"
              pagination={false}
              size="small"
              tableLayout="fixed"
              scroll={{ x: 1320 }}
              rowSelection={{
                selectedRowKeys: selectedReportIds,
                onChange: (selectedRowKeys) => {
                  setSelectedReportIds(selectedRowKeys as number[]);
                },
                getCheckboxProps: (record: any) => ({
                  disabled: record.isNew || record.isEditing, // 新建或编辑中的行不能选择
                }),
              }}
              rowClassName={getRowClassName}
              onRow={() => ({
                style: {
                  transition: 'all 0.3s ease'
                },
              })}
              expandable={{
                expandedRowRender: (record, index) => {
              const rowIndex = reportStoreIndex(record as ReportRowWithKey, index);
              const activeType = activeExperimentTypes.find(aet => aet.id === currentActiveType);
              if (!activeType) return null;
              const detectionContentConfig = getDetectionContentConfigByName(activeType.experimentType?.name || activeType.name);
              const isVisualInspectionType = activeType.experimentType?.code === 'VIS';
              const detectionContentValue = record.detectionContent || createDefaultDetectionContent(detectionContentConfig);
              const tableRowsForSummary =
                (detectionContentValue.mode === 'table' || detectionContentValue.mode === 'sod') && Array.isArray(detectionContentValue.rows)
                  ? detectionContentValue.rows
                  : [];
              const multiCompIds = record.projectComponentIds;
              const multiRowComponentSelect =
                multiCompIds &&
                multiCompIds.length > 1 &&
                tableRowsForSummary.length > 0 &&
                (detectionContentConfig.mode === 'table' || detectionContentConfig.mode === 'sod')
                  ? {
                      selectedIds: multiCompIds,
                      options: multiCompIds
                        .map((cid) => components.find((c) => c.id === cid))
                        .filter((c): c is ProjectComponent => c != null)
                        .map((c) => ({
                          value: c.id,
                          label: formatComponentLabel(c),
                        })),
                    }
                  : undefined;

              return (
                    <div className="report-expanded-panel">
                      {(record.detectionContentExpanded || record.imageAttachmentsExpanded) && (
                        <div
                          style={{
                            height: 4,
                            background: highlightBorderColor,
                            borderRadius: '0 0 6px 6px'
                          }}
                        />
                      )}
                      <div className="report-subcard">
                        <div className="report-subcard-content">
                        {/* 检测内容（在上方） */}
                        {record.detectionContentExpanded && (
                          <div className="report-subcard-section">
                            {/* 5 种类型：上方只读展示 检测内容、位置编号、总计（与后端自动填充一致） */}
                            {AUTO_FILL_READONLY_TYPE_NAMES.includes(activeType.experimentType?.name || activeType.name || '') && (
                              <div
                                className="report-subcard-section"
                                style={{
                                  marginBottom: 16,
                                  padding: '12px 12px',
                                  background: 'var(--ant-color-fill-quaternary, #f5f5f5)',
                                  borderRadius: 6,
                                }}
                              >
                                <div style={{ marginBottom: 8, fontWeight: 600, fontSize: 13 }}>位置编号、总计由检测数据自动导入，不可修改</div>
                                {(record.detectionContent?.mode === 'table' || record.detectionContent?.mode === 'sod') &&
                                Array.isArray((record.detectionContent as { rows?: unknown[] }).rows) &&
                                (record.detectionContent as { rows: unknown[] }).rows.length > 1 ? (
                                  (record.detectionContent as { rows: Array<{ type?: string; locationNumber?: string; total?: string }> }).rows.map((contentRow, ci) => {
                                    const af = computeAutoFillFromTableData(
                                      activeType.experimentType?.name || activeType.name || '',
                                      activeType.experimentType?.code || '',
                                      record.detectionData,
                                      ci,
                                    );
                                    const contentRows =
                                      (record.detectionContent as { rows?: DetectionContentTableRow[] }).rows ?? [];
                                    const rowCompId = resolveDetectionContentRowComponentId(
                                      contentRows[ci],
                                      ci,
                                      record.projectComponentIds,
                                    );
                                    const rowComp =
                                      rowCompId != null ? components.find((c) => c.id === rowCompId) : undefined;
                                    const rowSummary = buildComponentSummaryLine(rowComp);
                                    return (
                                      <Descriptions key={ci} size="small" column={1} bordered style={{ marginBottom: 12 }}>
                                        {record.projectComponentIds && record.projectComponentIds.length > 1 && (
                                          <Descriptions.Item label="部件信息">
                                            {rowSummary || '—'}
                                          </Descriptions.Item>
                                        )}
                                        <Descriptions.Item label={`类型（第 ${ci + 1} 条）`}>
                                          {contentRow?.type || af.type || '-'}
                                        </Descriptions.Item>
                                        <Descriptions.Item label="附图中位置编号">
                                          {contentRow?.locationNumber || af.locationNumber || '-'}
                                        </Descriptions.Item>
                                        <Descriptions.Item label="总计">
                                          {contentRow?.total || af.total || '-'}
                                        </Descriptions.Item>
                                      </Descriptions>
                                    );
                                  })
                                ) : (
                                <Descriptions size="small" column={1} bordered>
                                  {record.projectComponentIds && record.projectComponentIds.length > 1 && (
                                    <Descriptions.Item label="部件信息">
                                      {record.projectComponentIds
                                        .map((cid) => buildComponentSummaryLine(components.find((c) => c.id === cid)))
                                        .filter((s) => s.trim() !== '')
                                        .join('；') || '—'}
                                    </Descriptions.Item>
                                  )}
                                  <Descriptions.Item label="类型">
                                    {((record.detectionContent?.mode === 'table' || record.detectionContent?.mode === 'sod') &&
                                      Array.isArray(record.detectionContent?.rows) &&
                                      record.detectionContent.rows[0]?.type) ||
                                      computeAutoFillFromTableData(
                                        activeType.experimentType?.name || activeType.name || '',
                                        activeType.experimentType?.code || '',
                                        record.detectionData,
                                        0,
                                      ).type ||
                                      '-'}
                                  </Descriptions.Item>
                                  <Descriptions.Item label="附图中位置编号">
                                    {((record.detectionContent?.mode === 'table' || record.detectionContent?.mode === 'sod') &&
                                      Array.isArray(record.detectionContent?.rows) &&
                                      record.detectionContent.rows[0]?.locationNumber) ||
                                      computeAutoFillFromTableData(
                                        activeType.experimentType?.name || activeType.name || '',
                                        activeType.experimentType?.code || '',
                                        record.detectionData,
                                        0,
                                      ).locationNumber ||
                                      '-'}
                                  </Descriptions.Item>
                                  <Descriptions.Item label="总计">
                                    {((record.detectionContent?.mode === 'table' || record.detectionContent?.mode === 'sod') &&
                                      Array.isArray(record.detectionContent?.rows) &&
                                      record.detectionContent.rows[0]?.total) ||
                                      computeAutoFillFromTableData(
                                        activeType.experimentType?.name || activeType.name || '',
                                        activeType.experimentType?.code || '',
                                        record.detectionData,
                                        0,
                                      ).total ||
                                      '-'}
                                  </Descriptions.Item>
                                </Descriptions>
                                )}
                              </div>
                            )}
                            <DetectionContentEditor
                              config={detectionContentConfig}
                              value={detectionContentValue}
                              onChange={(value) => updateDetectionContent(currentActiveType, rowIndex, value)}
                              onSave={(value) => {
                                const normalized = normalizeDetectionContentValue(
                                  value,
                                  detectionContentConfig,
                                );
                                const row = reportRows[currentActiveType!]?.[rowIndex];
                                const nextDetectionData = row
                                  ? alignDetectionDataToContent(row.detectionData, normalized)
                                  : undefined;
                                if (nextDetectionData) {
                                  setReportRows((prev) => {
                                    const rows = prev[currentActiveType!];
                                    if (!rows || rowIndex >= rows.length) return prev;
                                    const newRows = [...rows];
                                    newRows[rowIndex] = {
                                      ...newRows[rowIndex],
                                      detectionContent: normalized,
                                      detectionData: nextDetectionData,
                                    };
                                    return { ...prev, [currentActiveType!]: newRows };
                                  });
                                } else {
                                  updateDetectionContent(currentActiveType, rowIndex, value);
                                }
                                saveReportWithLeebCategoryFlow(
                                  currentActiveType,
                                  rowIndex,
                                  undefined,
                                  nextDetectionData,
                                  normalized,
                                );
                              }}
                              onTextPreview={() =>
                                openExportTextDrawer(record.id ?? null, currentActiveType!, rowIndex)
                              }
                              detectionDataButton={
                                !isVisualInspectionType && (detectionContentConfig.mode === 'table' || detectionContentConfig.mode === 'sod')
                                  ? {
                                      onOpen: (contentRowIndex) => {
                                        setDetectionDataModal({
                                          open: true,
                                          typeId: currentActiveType!,
                                          rowIndex,
                                          contentRowIndex,
                                        });
                                      },
                                    }
                                  : undefined
                              }
                              multiRowComponentSelect={multiRowComponentSelect}
                              autoFillPreview={
                                detectionContentConfig.readOnlyFields?.includes('locationNumber')
                                  ? (contentRowIndex) => {
                                      const af = computeAutoFillFromTableData(
                                        activeType.experimentType?.name || activeType.name || '',
                                        activeType.experimentType?.code || '',
                                        record.detectionData,
                                        contentRowIndex,
                                      );
                                      return {
                                        locationNumber: af.locationNumber,
                                        total: af.total,
                                      };
                                    }
                                  : undefined
                              }
                            />
                          </div>
                        )}
                        
                        {/* 附图表格（在下方） */}
                        {record.imageAttachmentsExpanded && !isVisualInspectionType && (
                          <div className="report-subcard-section">
                            <ImageAttachmentsTable
                              value={record.imageAttachments}
                              experimentTypeCode={activeType.experimentType?.code}
                              onChange={(attachments) => {
                                updateReportField(currentActiveType, rowIndex, 'imageAttachments', attachments);
                              }}
                              onSave={(attachments) => {
                                // 1. 更新本地状态
                                updateReportField(currentActiveType, rowIndex, 'imageAttachments', attachments);
                                // 2. 自动触发主报告保存
                                handleSaveReport(currentActiveType, rowIndex);
                              }}
                            />
                          </div>
                        )}
                        </div>
                      </div>
                    </div>
              );
                },
                expandedRowKeys: reportSearchFilteredRows
                  .filter(({ row }) => row.detectionContentExpanded || row.imageAttachmentsExpanded)
                  .map(({ storeIndex }) => storeIndex),
                onExpand: (expanded, record) => {
                  const rowIndex =
                    typeof record.key === 'number' ? record.key : Number(record.key);
                  if (!Number.isFinite(rowIndex) || rowIndex < 0) return;
                  const activeType = activeExperimentTypes.find((aet) => aet.id === currentActiveType);
                  const isVisualInspectionType = activeType?.experimentType?.code === 'VIS';
                  setReportRows((prev) => {
                    const tid = currentActiveType!;
                    const rows = prev[tid];
                    if (!rows || rowIndex >= rows.length) return prev;
                    const newRows = [...rows];
                    newRows[rowIndex] = {
                      ...newRows[rowIndex],
                      detectionContentExpanded: expanded,
                      imageAttachmentsExpanded: isVisualInspectionType ? false : expanded,
                    };
                    return { ...prev, [tid]: newRows };
                  });
                },
              }}
              footer={() => (
                <Button
                  type="dashed"
                  icon={<PlusOutlined />}
                  onClick={handleAddReportRow}
                  block
                >
                  添加报告
                </Button>
              )}
            />
          </div>
        )}
      </Card>
            ),
          },
          {
            key: 'approval',
            label: '审批流程',
            children: (
              <ApprovalFlowCard
                project={project}
                fullName={fullName}
                isProjectTypeMissing={isProjectTypeMissing}
                canRollbackApproval={canRollbackApproval}
                submitApprovalMutation={submitApprovalMutation}
                approvalPassMutation={approvalPassMutation}
                approvalRejectMutation={approvalRejectMutation}
                approvalRollbackMutation={approvalRollbackMutation}
                onRefresh={() => {
                  queryClient.invalidateQueries({ queryKey: ['project', id] });
                  queryClient.invalidateQueries({ queryKey: ['projects'] });
                }}
                projectId={Number(id)}
              />
            ),
          },
          {
            key: 'reportChanges',
            label: '报告变更记录',
            children: (
              <ProjectReportChangeLogPanel
                projectId={Number(id)}
                active={detailSectionTab === 'reportChanges'}
              />
            ),
          },
        ]}
      />

      {/* 检测数据弹窗 */}
      {detectionDataModal.open &&
        detectionDataModal.typeId !== null &&
        detectionDataModal.rowIndex !== null &&
        detectionDataModal.contentRowIndex != null && (
        <Modal
          title={null}
          open={detectionDataModal.open}
          onCancel={() =>
            setDetectionDataModal({ open: false, typeId: null, rowIndex: null, contentRowIndex: null })
          }
          footer={null}
          width={1200}
          style={{ top: 20 }}
          styles={{
            body: {
              padding: '24px',
              background: '#f3ecff',
            },
          }}
          className="detection-data-modal"
        >
          {(() => {
            const activeType = activeExperimentTypes.find(aet => aet.id === detectionDataModal.typeId);
            const row = reportRows[detectionDataModal.typeId!]?.[detectionDataModal.rowIndex!];
            const ci = detectionDataModal.contentRowIndex!;
            if (!activeType || !row) return null;
            const blockRows = getDetectionDataRowsForContentBlock(row.detectionData, ci);
            const contentTableRowCount = detectionContentTableRowCount(
              row.detectionContent as DetectionContentPayload,
            );
            const lastContentIdx = Math.max(0, contentTableRowCount - 1);
            const tid = detectionDataModal.typeId!;
            const ri = detectionDataModal.rowIndex!;
            const persistBlockRows = (rows: Record<string, unknown>[], saveToServer: boolean) => {
              setReportRows((prev) => {
                const currentRow = prev[tid]?.[ri];
                if (!currentRow) return prev;
                const contentN = detectionContentTableRowCount(
                  currentRow.detectionContent as DetectionContentPayload,
                );
                const full = mergeBlockIntoTableData(
                  currentRow.detectionData,
                  ci,
                  rows,
                  contentN,
                );
                const newRows = [...(prev[tid] || [])];
                newRows[ri] = { ...currentRow, detectionData: full };
                if (saveToServer) {
                  queueMicrotask(() => saveReportWithLeebCategoryFlow(tid, ri, undefined, full));
                }
                return { ...prev, [tid]: newRows };
              });
            };
            return (
              <DynamicDetectionDataTable
                key={`detection-grid-${detectionDataModal.typeId}-${detectionDataModal.rowIndex}-${ci}`}
                tableSchema={activeType.experimentType.tableSchema}
                appendTrailingSlashPlaceholderRow={ci === lastContentIdx}
                value={{ rows: blockRows }}
                {...({ nonComplianceRecords: row.nonComplianceRecords } as any)}
                experimentTypeName={activeType.experimentType?.name || activeType.name || '检测数据'}
                onChange={(detectionData) => {
                  persistBlockRows(detectionData.rows as Record<string, unknown>[], false);
                }}
                onSave={(detectionData) => {
                  persistBlockRows(detectionData.rows as Record<string, unknown>[], true);
                }}
              />
            );
          })()}
        </Modal>
      )}

      <LeebHardnessCategorySaveModal
        open={leebCategoryModalState.open}
        sourceRows={leebCategoryModalState.sourceRows}
        onCancel={() =>
          setLeebCategoryModalState({
            open: false,
            typeId: null,
            rowIndex: null,
            sourceRows: [],
            mapping: [],
            detectionData: null,
            rowData: undefined,
            detectionContentOverride: undefined,
          })
        }
        onConfirm={(mergedRows) => {
          const { typeId, rowIndex, mapping, detectionData, rowData, detectionContentOverride } = leebCategoryModalState;
          setLeebCategoryModalState({
            open: false,
            typeId: null,
            rowIndex: null,
            sourceRows: [],
            mapping: [],
            detectionData: null,
            rowData: undefined,
            detectionContentOverride: undefined,
          });
          if (typeId == null || rowIndex == null) return;
          const blocks = extractPerContentRowBlocks(detectionData);
          mapping.forEach((m, idx) => {
            if (!blocks[m.blockIndex] || !blocks[m.blockIndex].rows?.[m.rowIndex]) return;
            blocks[m.blockIndex].rows[m.rowIndex] = {
              ...blocks[m.blockIndex].rows[m.rowIndex],
              ...(mergedRows[idx] || {}),
            };
          });
          const nextDetectionData = buildTableDataPayload(blocks);
          void handleSaveReport(typeId, rowIndex, rowData, nextDetectionData, detectionContentOverride);
        }}
      />

      <Modal
        title="报告附图"
        open={reportFiguresModalOpen}
        onCancel={() => setReportFiguresModalOpen(false)}
        width={980}
        destroyOnClose
        styles={{ body: { maxHeight: '70vh', overflowY: 'auto' } }}
        footer={[
          <Button key="cancel" onClick={() => setReportFiguresModalOpen(false)}>
            取消
          </Button>,
          <Button
            key="save"
            type="primary"
            loading={reportFiguresSaving}
            onClick={() => void saveProjectReportFigures()}
          >
            保存
          </Button>,
        ]}
      >
        <ImageAttachmentsTable
          value={projectReportFigures}
          onChange={setProjectReportFigures}
          disabled={reportFiguresSaving}
        />
      </Modal>

      {/* 添加检测类型模态框 */}
      <Modal
        title="添加检测类型"
        open={showAddTypeModal}
        onOk={handleConfirmAddType}
        onCancel={() => {
          setShowAddTypeModal(false);
          setSelectedTypeToAdd(null);
        }}
        okText="添加"
        cancelText="取消"
      >
        <div style={{ marginBottom: 16 }}>
          <label>选择检测类型：</label>
        </div>
        <Select
          style={{ width: '100%' }}
          placeholder="请选择检测类型"
          value={selectedTypeToAdd}
          onChange={setSelectedTypeToAdd}
          showSearch
          optionFilterProp="children"
          filterOption={(input, option) => {
            const children = option?.children as any;
            if (typeof children === 'string') {
              return children.toLowerCase().includes(input.toLowerCase());
            }
            if (Array.isArray(children)) {
              return children.some((child: any) => {
                if (typeof child === 'string') {
                  return child.toLowerCase().includes(input.toLowerCase());
                }
                return false;
              });
            }
            return false;
          }}
        >
          {experimentTypes?.map(et => (
            <Option key={et.id} value={et.id}>
              {et.name}
            </Option>
          ))}
        </Select>
      </Modal>

      <Modal
        title="第三方审批信息（按检测类型）"
        open={thirdPartyApprovalModalOpen}
        onCancel={() => setThirdPartyApprovalModalOpen(false)}
        width={920}
        styles={{ body: { maxHeight: '72vh', overflowY: 'auto' } }}
        destroyOnClose
        footer={[
          <Button key="cancel" onClick={() => setThirdPartyApprovalModalOpen(false)}>
            取消
          </Button>,
          <Button
            key="save"
            type="primary"
            loading={thirdPartyApprovalSaving}
            onClick={() => void saveThirdPartyApproval()}
          >
            保存
          </Button>,
        ]}
      >
        {(project.selectedExperimentTypeIds ?? []).length === 0 ? (
          <div style={{ color: '#999' }}>请先在项目中添加检测类型。</div>
        ) : (
          <Collapse
            bordered={false}
            items={(project.selectedExperimentTypeIds ?? []).map((tid) => {
              const key = String(tid);
              const et = experimentTypes.find((e) => e.id === tid);
              const d = thirdPartyApprovalDraft[key] ?? emptyThirdPartyApprovalFormRow();
              const showLevel = experimentTypeSupportsNdtLevelCode(et?.code);
              return {
                key,
                label: et ? `${et.name}（${et.code}）` : `检测类型 #${tid}`,
                children: (
                  <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                    <Row gutter={[12, 8]} align="middle">
                      <Col xs={24} sm={5}>
                        编写人
                      </Col>
                      <Col xs={24} sm={9}>
                        <Input
                          value={d.writer}
                          onChange={(e) => patchThirdPartyDraft(key, { writer: e.target.value })}
                          placeholder="姓名"
                          allowClear
                        />
                      </Col>
                      <Col xs={24} sm={10}>
                        <DatePicker
                          style={{ width: '100%' }}
                          value={d.writerDate}
                          onChange={(v) => patchThirdPartyDraft(key, { writerDate: v })}
                          placeholder="日期"
                        />
                      </Col>
                    </Row>
                    {showLevel && (
                      <Row gutter={[12, 8]} align="middle">
                        <Col xs={24} sm={5}>
                          编写级别
                        </Col>
                        <Col xs={24} sm={19}>
                          <Select
                            style={{ width: '100%' }}
                            placeholder="请选择级别"
                            allowClear
                            value={d.writerLevel || undefined}
                            onChange={(v) => patchThirdPartyDraft(key, { writerLevel: v ?? '' })}
                            options={[
                              { value: 'Ⅱ', label: 'Ⅱ级' },
                              { value: 'Ⅲ', label: 'Ⅲ级' },
                            ]}
                          />
                        </Col>
                      </Row>
                    )}
                    <Row gutter={[12, 8]} align="middle">
                      <Col xs={24} sm={5}>
                        审核人
                      </Col>
                      <Col xs={24} sm={9}>
                        <Input
                          value={d.reviewer}
                          onChange={(e) => patchThirdPartyDraft(key, { reviewer: e.target.value })}
                          placeholder="姓名"
                          allowClear
                        />
                      </Col>
                      <Col xs={24} sm={10}>
                        <DatePicker
                          style={{ width: '100%' }}
                          value={d.reviewDate}
                          onChange={(v) => patchThirdPartyDraft(key, { reviewDate: v })}
                          placeholder="日期"
                        />
                      </Col>
                    </Row>
                    {showLevel && (
                      <Row gutter={[12, 8]} align="middle">
                        <Col xs={24} sm={5}>
                          审核级别
                        </Col>
                        <Col xs={24} sm={19}>
                          <Select
                            style={{ width: '100%' }}
                            placeholder="请选择级别"
                            allowClear
                            value={d.reviewerLevel || undefined}
                            onChange={(v) => patchThirdPartyDraft(key, { reviewerLevel: v ?? '' })}
                            options={[
                              { value: 'Ⅱ', label: 'Ⅱ级' },
                              { value: 'Ⅲ', label: 'Ⅲ级' },
                            ]}
                          />
                        </Col>
                      </Row>
                    )}
                    <Row gutter={[12, 8]} align="middle">
                      <Col xs={24} sm={5}>
                        批准人
                      </Col>
                      <Col xs={24} sm={9}>
                        <Input
                          value={d.approver}
                          onChange={(e) => patchThirdPartyDraft(key, { approver: e.target.value })}
                          placeholder="姓名"
                          allowClear
                        />
                      </Col>
                      <Col xs={24} sm={10}>
                        <DatePicker
                          style={{ width: '100%' }}
                          value={d.approvalDate}
                          onChange={(v) => patchThirdPartyDraft(key, { approvalDate: v })}
                          placeholder="日期"
                        />
                      </Col>
                    </Row>
                  </Space>
                ),
              };
            })}
          />
        )}
      </Modal>

      <Drawer
        title="报告文本"
        width={720}
        open={exportTextDrawerOpen}
        onClose={() => {
          setExportTextDrawerOpen(false);
          setExportTextContext(null);
          setExportTextPreview(null);
          setExportTextContentRowIndex(0);
        }}
        destroyOnClose
        footer={
          <Space style={{ justifyContent: 'flex-end', width: '100%' }}>
            <Button
              onClick={() => void applyExportTextDefaults()}
              disabled={!exportTextPreview || exportTextContext?.reportId == null || exportTextLoading}
            >
              恢复自动生成
            </Button>
            <Button
              type="primary"
              loading={exportTextSaving}
              onClick={() => void saveExportTextOverrides()}
              disabled={exportTextContext?.reportId == null}
            >
              保存覆盖
            </Button>
          </Space>
        }
      >
        {!exportTextContext?.reportId && (
          <Typography.Paragraph type="secondary" style={{ marginBottom: 16 }}>
            保存报告后可从服务端加载自动生成文案。
          </Typography.Paragraph>
        )}
        {exportTextPerContentRowEnabled && exportTextSegmentOptions.length > 1 && (
          <div style={{ marginBottom: 12 }}>
            <Segmented
              block
              options={exportTextSegmentOptions}
              value={exportTextContentRowIndex}
              onChange={(v) => handleExportTextContentRowChange(Number(v))}
              disabled={exportTextLoading}
            />
            {exportTextPreview?.contentRowType && (
              <Typography.Text type="secondary" style={{ display: 'block', marginTop: 6 }}>
                当前检测类型：{exportTextPreview.contentRowType}
              </Typography.Text>
            )}
          </div>
        )}
        {exportTextLoading && <Typography.Text type="secondary">加载中…</Typography.Text>}
        {!exportTextLoading && (
          <Space direction="vertical" size={10} style={{ width: '100%' }}>
            <Card
              title="单项报告"
              bordered
              style={{
                borderColor: '#d3adf7',
                borderWidth: 2,
                background: '#f9f0ff',
              }}
              styles={{
                header: {
                  padding: '6px 12px',
                  minHeight: 36,
                  fontWeight: 600,
                  //borderBottom: 'none',
                  borderBottom: '1px solid #d3adf7',
                  background: '#efdbff',
                },
                body: { padding: '8px 12px' },
              }}
            >
              <div>
                <Typography.Text strong>检测内容</Typography.Text>
                <Input.TextArea
                  rows={4}
                  value={exportTextFields.detectionNarrativeBody}
                  onChange={(e) =>
                    setExportTextFields((f) => ({ ...f, detectionNarrativeBody: e.target.value }))
                  }
                  style={{ marginTop: 6 }}
                />
              </div>
              <div style={{ marginTop: 12 }}>
                <Typography.Text strong>{exportTextConclusionBlockTitle}</Typography.Text>
                <Input.TextArea
                  rows={4}
                  value={exportTextFields.conclusionParagraph}
                  onChange={(e) =>
                    setExportTextFields((f) => ({ ...f, conclusionParagraph: e.target.value }))
                  }
                  style={{ marginTop: 6 }}
                />
              </div>
            </Card>
            <Card
              title="概述"
              bordered
              style={{
                borderColor: '#d3adf7',
                borderWidth: 2,
                background: '#f9f0ff',
              }}
              styles={{
                header: {
                  padding: '6px 12px',
                  minHeight: 36,
                  fontWeight: 600,
                  borderBottom: '1px solid #d3adf7',
                  background: '#efdbff',
                },
                body: { padding: '8px 12px' },
              }}
            >
              {exportTextPreview?.overviewMultiSegment && (
                <Typography.Paragraph type="secondary" style={{ marginBottom: 10, fontSize: 12 }}>
                  概述按当前选中部件分别编辑；自动生成时每段对应递增的单项报告编号（与总报告第 3 章一致）。
                </Typography.Paragraph>
              )}
              <div>
                <Typography.Text strong>工作内容</Typography.Text>
                <div style={{ marginTop: 2 }}>
                  <Typography.Text type="secondary">
                    （单项报告编号会自动添加，无需调整）
                  </Typography.Text>
                </div>
                <Input.TextArea
                  rows={3}
                  value={exportTextFields.overviewWorkContentLine}
                  onChange={(e) =>
                    setExportTextFields((f) => ({ ...f, overviewWorkContentLine: e.target.value }))
                  }
                  style={{ marginTop: 6 }}
                />
              </div>
              <div style={{ marginTop: 12 }}>
                <Typography.Text strong>发现问题及处理情况</Typography.Text>
                {!exportTextShowDefectSection && (
                  <div style={{ marginTop: 2 }}>
                    <Typography.Text type="secondary">（当前无缺陷可不填）</Typography.Text>
                  </div>
                )}
                <Input.TextArea
                  rows={3}
                  value={exportTextFields.overviewDefectLine}
                  onChange={(e) =>
                    setExportTextFields((f) => ({ ...f, overviewDefectLine: e.target.value }))
                  }
                  disabled={exportTextPreview != null && !exportTextShowDefectSection}
                  placeholder={
                    exportTextPreview != null && !exportTextShowDefectSection
                      ? '当前报告判定为无缺陷'
                      : undefined
                  }
                  style={{ marginTop: 6 }}
                />
              </div>
            </Card>
          </Space>
        )}
      </Drawer>
      <OverviewPreviewModal
        open={overviewPreviewOpen}
        loading={overviewPreviewLoading}
        data={overviewPreviewData}
        onClose={() => setOverviewPreviewOpen(false)}
      />
      <ReportOverviewOrderModal
        open={reportOrderModalOpen}
        onClose={() => setReportOrderModalOpen(false)}
        projectId={Number(id)}
        reports={projectReports || []}
        components={projectComponents || []}
        aggregateDetectionLogOrderJson={project?.aggregateDetectionLogOrder}
        onSaved={() => queryClient.invalidateQueries({ queryKey: ['project', id] })}
      />
    </div>
    </ErrorBoundary>
  );
};

export default ProjectDetailPage;