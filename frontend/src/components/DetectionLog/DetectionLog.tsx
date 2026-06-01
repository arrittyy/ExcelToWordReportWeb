import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Card,
  Button,
  Typography,
  Timeline,
  Space,
  message,
  Checkbox,
  Modal,
  Segmented,
  Tag,
} from 'antd';
import {
  DownloadOutlined,
  FileTextOutlined,
} from '@ant-design/icons';
import type {
  ReportList,
  DetectionContentPayload,
  DetectionContentTablePayload,
  DetectionContentDualTextareaPayload,
  DetectionContentTextareaPayload,
  DetectionContentSingleFieldPayload,
} from '@/types';
import type { ProjectComponent } from '@/services/componentService';
import dayjs from 'dayjs';
import { projectService, type WordExportJob } from '@/services/projectService';
import { formatMultiComponentDisplay } from '@/utils/reportComponentDisplay';
import { stripTypeParentheticalForConcat } from '@/utils/detectionTypeLabel';
import { waitForWordExportJob } from '@/utils/wordExportJob';
import {
  mergeSavedOrder,
  getRepresentativeReport,
} from '@/utils/aggregateDetectionLogOrder';
import { extractPerContentRowBlocks, flattenPerContentRowRows } from '@/utils/detectionTableData';

const { Text } = Typography;

type ViewMode = 'byDate' | 'aggregate';

interface DetectionLogProps {
  reports: ReportList[];
  components?: ProjectComponent[];
  projectId: number;
  projectName?: string;
  aggregateDetectionLogOrderJson?: string | null;
  /** 打开「调整报告顺序」弹窗（由项目详情统一挂载 Modal） */
  onOpenReportOrderModal?: () => void;
  /** 日志滚动区域 max-height，默认 350px；详情 Tab 内可加大展示区域 */
  logScrollMaxHeight?: string;
}

interface LogEntry {
  date: string;
  componentName: string;
  testMethod: string;
  detectionContent: string;
  hasDefect: string;
  inspector: string;
}

const LOCATION_NUMBER_KEYS = ['编号', '序号', '缺陷编号', '测点编号', '位置编号'] as const;

function extractFallbackLocationNumberFromRows(rows: Record<string, unknown>[]): string {
  const values: string[] = [];
  for (const row of rows) {
    for (const key of LOCATION_NUMBER_KEYS) {
      const raw = row[key];
      if (raw != null && String(raw).trim() !== '') {
        values.push(String(raw).trim());
        break;
      }
    }
  }
  if (values.length === 0) return '';
  return Array.from(new Set(values)).join('、');
}

function groupReportsByDateAndComponent(
  reports: ReportList[],
  getComponentName: (report: ReportList) => string,
): Record<string, Record<string, ReportList[]>> {
  const dateGroups: Record<string, Record<string, ReportList[]>> = {};
  reports.forEach((report) => {
    if (!report.testDate) return;
    const date = dayjs(report.testDate).format('YYYY-MM-DD');
    const componentName = getComponentName(report);
    if (!dateGroups[date]) {
      dateGroups[date] = {};
    }
    if (!dateGroups[date][componentName]) {
      dateGroups[date][componentName] = [];
    }
    dateGroups[date][componentName].push(report);
  });
  return dateGroups;
}

const DetectionLog: React.FC<DetectionLogProps> = ({
  reports,
  components = [],
  projectId,
  projectName,
  aggregateDetectionLogOrderJson,
  onOpenReportOrderModal,
  logScrollMaxHeight = '350px',
}) => {
  const [viewMode, setViewMode] = useState<ViewMode>('byDate');
  const [notificationModalOpen, setNotificationModalOpen] = useState(false);
  const [selectedReportIds, setSelectedReportIds] = useState<number[]>([]);
  const [generatingNotification, setGeneratingNotification] = useState(false);
  const [notificationJob, setNotificationJob] = useState<WordExportJob | null>(null);
  const isNotificationRunning = notificationJob?.status === 'PENDING' || notificationJob?.status === 'RUNNING';

  const getComponentName = (report: ReportList): string => {
    if (report.componentName?.trim()) {
      return report.componentName.trim();
    }
    if (report.projectComponentId && components.length > 0) {
      const component = components.find((c) => c.id === report.projectComponentId);
      if (component?.componentName) {
        return component.componentName;
      }
    }
    return '未指定部件';
  };

  const formatDetectionContent = (
    detectionContent: DetectionContentPayload | null | undefined,
    reportItems?: any[],
  ): string => {
    const firstItem = reportItems && reportItems.length > 0 ? reportItems[0] : null;
    let parsedTableData: any = null;
    if (firstItem?.tableData) {
      try {
        parsedTableData =
          typeof firstItem.tableData === 'string' ? JSON.parse(firstItem.tableData) : firstItem.tableData;
      } catch (error) {
        console.error('Error parsing reportItems tableData:', error);
      }
    }

    if (detectionContent) {
      try {
        switch (detectionContent.mode) {
          case 'table': {
            const rows = (detectionContent as DetectionContentTablePayload).rows || [];
            if (rows.length === 0) return '无检测数据';
            const summary = rows
              .slice(0, 3)
              .map((row, rowIndex) => {
                const type = stripTypeParentheticalForConcat(row.type || '');
                const locationDesc = row.locationDesc || '';
                const fallbackRows =
                  parsedTableData && typeof parsedTableData === 'object'
                    ? extractPerContentRowBlocks(parsedTableData)[rowIndex]?.rows ||
                      (rowIndex === 0 && Array.isArray(parsedTableData.rows) ? parsedTableData.rows : [])
                    : [];
                const fallbackLocationNumber = extractFallbackLocationNumberFromRows(
                  Array.isArray(fallbackRows) ? fallbackRows : [],
                );
                const locationNumber = row.locationNumber || fallbackLocationNumber || '';
                const total = row.total || '';
                const parts = [];
                if (type) parts.push(type);
                if (total) parts.push(`总计数量：${total}`);
                if (locationDesc) parts.push(`具体位置：${locationDesc}`);
                if (locationNumber) parts.push(`编号：${locationNumber}`);
                return parts.join('，');
              })
              .filter(Boolean)
              .join('；');
            const moreCount = rows.length > 3 ? `等${rows.length}项` : '';
            return summary ? `${summary}${moreCount}` : `检测${rows.length}项`;
          }
          case 'dual-textarea': {
            const { position, conclusion } = detectionContent as DetectionContentDualTextareaPayload;
            const parts = [];
            if (position) parts.push(position);
            if (conclusion) parts.push(conclusion);
            return parts.join('，') || '无';
          }
          case 'textarea':
            return (detectionContent as DetectionContentTextareaPayload).conclusion || '无';
          case 'single':
            return (detectionContent as DetectionContentSingleFieldPayload).value || '无';
          default:
            return '无';
        }
      } catch (error) {
        console.error('Error formatting detection content:', error);
      }
    }

    if (reportItems && reportItems.length > 0) {
      try {
        if (parsedTableData) {
          const tableData = parsedTableData;
          if (tableData && typeof tableData === 'object') {
            const mergedLen = flattenPerContentRowRows(extractPerContentRowBlocks(tableData)).length;
            if (mergedLen > 0) {
              return `检测${mergedLen}项`;
            }
            if (tableData.rows && Array.isArray(tableData.rows)) {
              return `检测${tableData.rows.length}项`;
            }
          }
        }
      } catch (error) {
        console.error('Error reading reportItems tableData:', error);
      }
    }

    return '无';
  };

  const detectionContentTextForReport = (report: ReportList): string => {
    const n = report.detectionContentNarrative?.trim();
    if (n) return n;
    return formatDetectionContent(report.detectionContent || null, report.reportItems);
  };

  const mergedAggregateOrder = useMemo(
    () => mergeSavedOrder(reports || [], aggregateDetectionLogOrderJson, components),
    [reports, aggregateDetectionLogOrderJson, components],
  );

  const effectiveAggregateOrder = mergedAggregateOrder;

  const reportById = useMemo(() => {
    const m = new Map<number, ReportList>();
    (reports || []).forEach((r) => {
      if (r.id != null) m.set(r.id, r);
    });
    return m;
  }, [reports]);

  const logEntries = useMemo(() => {
    if (!reports || reports.length === 0) return [];

    const entries: LogEntry[] = reports
      .filter((report) => report.testDate)
      .map((report) => ({
        date: dayjs(report.testDate).format('YYYY-MM-DD'),
        componentName: getComponentName(report),
        testMethod: report.testMethod || '未指定检测方法',
        detectionContent: detectionContentTextForReport(report),
        hasDefect: report.hasDefect || '未设置',
        inspector: report.inspector || '未指定',
      }))
      .sort((a, b) => dayjs(b.date).valueOf() - dayjs(a.date).valueOf());

    return entries;
  }, [reports, components]);

  const groupedEntries = useMemo(() => {
    const dateGroups: Record<string, Record<string, LogEntry[]>> = {};
    logEntries.forEach((entry) => {
      if (!dateGroups[entry.date]) {
        dateGroups[entry.date] = {};
      }
      if (!dateGroups[entry.date][entry.componentName]) {
        dateGroups[entry.date][entry.componentName] = [];
      }
      dateGroups[entry.date][entry.componentName].push(entry);
    });
    return dateGroups;
  }, [logEntries]);

  const groupedDefectReports = useMemo(() => {
    const defectList = (reports || []).filter((r) => r.testDate && r.hasDefect === '是');
    defectList.sort((a, b) => dayjs(b.testDate!).valueOf() - dayjs(a.testDate!).valueOf());
    return groupReportsByDateAndComponent(defectList, getComponentName);
  }, [reports, components]);

  const allDefectReportIds = useMemo(
    () =>
      (reports || [])
        .filter((r) => r.testDate && r.hasDefect === '是' && r.id != null)
        .map((r) => r.id as number),
    [reports],
  );

  useEffect(() => {
    if (!notificationModalOpen) return;
    setSelectedReportIds([...allDefectReportIds]);
  }, [notificationModalOpen, allDefectReportIds]);

  useEffect(() => {
    if (!notificationModalOpen) return;
    let cancelled = false;
    (async () => {
      try {
        const latest = await projectService.getLatestWordExportJob(projectId, 'DETECTION_NOTIFICATION');
        if (!cancelled) setNotificationJob(latest);
      } catch {
        if (!cancelled) setNotificationJob(null);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [notificationModalOpen, projectId]);

  const toggleReportSelection = (reportId: number) => {
    setSelectedReportIds((prev) =>
      prev.includes(reportId) ? prev.filter((id) => id !== reportId) : [...prev, reportId],
    );
  };

  const selectAllDefectReports = (checked: boolean) => {
    if (checked) {
      setSelectedReportIds([...allDefectReportIds]);
    } else {
      setSelectedReportIds([]);
    }
  };

  const allDefectSelected =
    allDefectReportIds.length > 0 && allDefectReportIds.every((id) => selectedReportIds.includes(id));
  const selectAllIndeterminate = selectedReportIds.length > 0 && !allDefectSelected;

  const groupDisplayTitle = useCallback(
    (groupKey: string): string => {
      const rep = getRepresentativeReport(
        reports || [],
        groupKey,
        mergedAggregateOrder.experimentTypeOrder,
      );
      if (!rep) return groupKey === 'none' ? '未指定部件' : groupKey;
      if (
        !rep.projectComponentIds?.length &&
        rep.projectComponentId == null
      ) {
        return '未指定部件';
      }
      return formatMultiComponentDisplay(
        rep.projectComponentIds,
        rep.projectComponentId,
        components,
      );
    },
    [reports, components, mergedAggregateOrder.experimentTypeOrder],
  );

  const handleExport = () => {
    if (viewMode === 'aggregate') {
      const headers = ['部件（与报告表一致）', '检测日期', '检测方法', '检测内容', '是否存在缺陷', '检测人员'];
      const rows: string[][] = [];
      for (const key of effectiveAggregateOrder.componentKeys) {
        const title = groupDisplayTitle(key);
        const ids = effectiveAggregateOrder.reportIdsByComponent[key] || [];
        for (const rid of ids) {
          const report = reportById.get(rid);
          if (!report) continue;
          const dateStr = report.testDate
            ? dayjs(report.testDate).format('YYYY-MM-DD')
            : '未填日期';
          rows.push([
            title,
            dateStr,
            report.testMethod || '未指定检测方法',
            detectionContentTextForReport(report).replace(/"/g, '""'),
            report.hasDefect || '未设置',
            report.inspector || '未指定',
          ]);
        }
      }
      if (rows.length === 0) {
        message.warning('没有可导出的记录');
        return;
      }
      const csvContent = [
        headers.map((h) => `"${h}"`).join(','),
        ...rows.map((row) => row.map((cell) => `"${cell}"`).join(',')),
      ].join('\n');
      const BOM = '\uFEFF';
      const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `总检测日志_${dayjs().format('YYYY-MM-DD')}.csv`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      return;
    }

    if (logEntries.length === 0) return;
    const headers = ['日期', '部件名称', '检测方法', '检测内容', '是否存在缺陷', '检测人员'];
    const rows = logEntries.map((entry) => [
      entry.date,
      entry.componentName,
      entry.testMethod,
      entry.detectionContent.replace(/"/g, '""'),
      entry.hasDefect,
      entry.inspector,
    ]);
    const csvContent = [
      headers.map((h) => `"${h}"`).join(','),
      ...rows.map((row) => row.map((cell) => `"${cell}"`).join(',')),
    ].join('\n');
    const BOM = '\uFEFF';
    const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `检测日志_${dayjs().format('YYYY-MM-DD')}.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  };

  const handlePrepareNotification = async () => {
    if (selectedReportIds.length === 0) {
      message.warning('请至少勾选一条缺陷报告');
      return Promise.reject(new Error('no selection'));
    }
    setGeneratingNotification(true);
    try {
      const created = await projectService.createWordExportJob(projectId, {
        type: 'DETECTION_NOTIFICATION',
        reportIds: selectedReportIds,
      });
      setNotificationJob(created);
      message.info('后台正在生成检测通知单，请稍候...');
      const finalJob = await waitForWordExportJob(projectId, created.jobId);
      setNotificationJob(finalJob);
      if (finalJob.status === 'SUCCEEDED') {
        message.success('检测通知单已生成，可点击下载');
      } else {
        throw new Error(finalJob.errorMessage || '检测通知单生成失败');
      }
    } catch (error: any) {
      let errMsg = error?.message || '检测通知单生成失败';
      const data = error?.response?.data;
      if (data instanceof Blob) {
        try {
          const text = await data.text();
          const j = JSON.parse(text);
          if (j?.message) errMsg = j.message;
        } catch {
          /* ignore */
        }
      } else if (error?.response?.data?.message) {
        errMsg = error.response.data.message;
      }
      message.error(errMsg);
      throw error;
    } finally {
      setGeneratingNotification(false);
    }
  };

  const handleDownloadNotification = async () => {
    if (!notificationJob || notificationJob.status !== 'SUCCEEDED') {
      message.warning('请先预生成并等待完成');
      return;
    }
    try {
      const blob = await projectService.downloadWordExportJob(projectId, notificationJob.jobId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = notificationJob.suggestedFileName || `${projectName || '项目'}_检测通知单.docx`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      message.success('检测通知单下载成功');
    } catch (error: any) {
      let errMsg = error?.message || '检测通知单生成失败';
      const data = error?.response?.data;
      if (data instanceof Blob) {
        try {
          const text = await data.text();
          const j = JSON.parse(text);
          if (j?.message) errMsg = j.message;
        } catch {
          /* ignore */
        }
      } else if (error?.response?.data?.message) {
        errMsg = error.response.data.message;
      }
      message.error(errMsg);
      throw error;
    }
  };

  const defectDateKeys = Object.keys(groupedDefectReports).sort(
    (a, b) => dayjs(b).valueOf() - dayjs(a).valueOf(),
  );

  const aggregateRowCount = useMemo(() => {
    let n = 0;
    for (const key of effectiveAggregateOrder.componentKeys) {
      n += (effectiveAggregateOrder.reportIdsByComponent[key] || []).length;
    }
    return n;
  }, [effectiveAggregateOrder]);

  const canExport =
    viewMode === 'aggregate' ? aggregateRowCount > 0 : logEntries.length > 0;

  const cardExtra = (
    <Space wrap>
      <Segmented
        value={viewMode}
        onChange={(v) => {
          const m = v as ViewMode;
          setViewMode(m);
        }}
        options={[
          { label: '按日期', value: 'byDate' },
          { label: '总日志', value: 'aggregate' },
        ]}
      />
      {viewMode === 'aggregate' && (
        <Button
          size="small"
          disabled={!onOpenReportOrderModal}
          onClick={() => onOpenReportOrderModal?.()}
        >
          调整顺序
        </Button>
      )}
      <Button
        type="primary"
        ghost
        icon={<FileTextOutlined />}
        onClick={() => setNotificationModalOpen(true)}
      >
        生成检测通知单
      </Button>
      <Button icon={<DownloadOutlined />} disabled={!canExport} onClick={handleExport}>
        导出日志
      </Button>
    </Space>
  );

  const scrollBoxStyle: React.CSSProperties = {
    maxHeight: logScrollMaxHeight,
    overflowY: 'auto',
    paddingRight: '8px',
  };

  const scrollbarCss = `
          .detection-log-scroll::-webkit-scrollbar {
            width: 6px;
          }
          .detection-log-scroll::-webkit-scrollbar-track {
            background: #f1f1f1;
            border-radius: 3px;
          }
          .detection-log-scroll::-webkit-scrollbar-thumb {
            background: #c1c1c1;
            border-radius: 3px;
          }
          .detection-log-scroll::-webkit-scrollbar-thumb:hover {
            background: #a8a8a8;
          }
        `;

  const renderAggregateBody = () => {
    if (aggregateRowCount === 0) {
      return (
        <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>暂无检测记录（需有报告编号）</div>
      );
    }

    return (
      <div style={{ paddingTop: 8 }}>
        {effectiveAggregateOrder.componentKeys.map((groupKey) => {
          const title = groupDisplayTitle(groupKey);
          const ids = effectiveAggregateOrder.reportIdsByComponent[groupKey] || [];
          return (
            <div key={groupKey} style={{ marginBottom: 20 }}>
              <div
                style={{
                  marginBottom: 8,
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                  flexWrap: 'wrap',
                }}
              >
                <Text strong style={{ fontSize: 14, color: '#262626' }}>
                  {title}
                </Text>
              </div>
              <div style={{ marginLeft: 16 }}>
                {ids.map((rid) => {
                  const report = reportById.get(rid);
                  if (!report) return null;
                  const content = detectionContentTextForReport(report);
                  const method = report.testMethod || '未指定检测方法';
                  const inspector = report.inspector || '未指定';
                  const dateLine = report.testDate
                    ? dayjs(report.testDate).format('YYYY-MM-DD')
                    : '检测日期未填';
                  return (
                    <div
                      key={rid}
                      style={{
                        marginBottom: 8,
                        padding: '8px 12px',
                        backgroundColor: '#fafafa',
                        borderRadius: 6,
                        borderLeft: '3px solid #722ed1',
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: 8,
                      }}
                    >
                      <div style={{ flex: 1, fontSize: 13, color: '#595959', lineHeight: 1.7 }}>
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          {dateLine}
                        </Text>
                        <div>
                          <Text style={{ color: '#722ed1', fontWeight: 500 }}>{method}</Text>
                          {content && content !== '无' && <Text>：{content}</Text>}
                          {report.hasDefect && report.hasDefect !== '未设置' && (
                            <Text
                              style={{
                                color:
                                  report.hasDefect === '是'
                                    ? '#ff4d4f'
                                    : report.hasDefect === '否'
                                      ? '#52c41a'
                                      : '#595959',
                                fontWeight: 500,
                                marginLeft: 4,
                              }}
                            >
                              ，
                              {report.hasDefect === '是'
                                ? '存在缺陷'
                                : report.hasDefect === '否'
                                  ? '未见缺陷'
                                  : report.hasDefect}
                            </Text>
                          )}
                          {inspector !== '未指定' && (
                            <Text style={{ marginLeft: 4 }}>
                              ，检测人员：<Text style={{ color: '#1890ff' }}>{inspector}</Text>
                            </Text>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
    );
  };

  return (
    <>
      <Card
        title="检测日志"
        style={{
          marginBottom: 16,
          borderRadius: 12,
          overflow: 'hidden',
          boxShadow: '0 6px 20px rgba(0,0,0,0.06)',
          border: '1px solid #f0f0f0',
        }}
        extra={cardExtra}
      >
        {viewMode === 'byDate' ? (
          logEntries.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>暂无检测日志</div>
          ) : (
            <div style={scrollBoxStyle} className="detection-log-scroll">
              <style>{scrollbarCss}</style>
              <Timeline mode="left" style={{ paddingTop: '8px' }}>
                {Object.keys(groupedEntries)
                  .sort((a, b) => dayjs(b).valueOf() - dayjs(a).valueOf())
                  .map((date) => (
                    <Timeline.Item key={date} color="#1890ff" style={{ paddingBottom: '20px' }}>
                      <div style={{ marginBottom: '12px' }}>
                        <Text strong style={{ fontSize: '15px', color: '#1890ff', fontWeight: 600 }}>
                          {date}：
                        </Text>
                        <div style={{ marginLeft: '24px', marginTop: '10px' }}>
                          {Object.keys(groupedEntries[date])
                            .sort()
                            .map((componentName) => (
                              <div key={componentName} style={{ marginBottom: '16px' }}>
                                <div style={{ marginBottom: '8px' }}>
                                  <Text strong style={{ fontSize: '14px', color: '#262626' }}>
                                    {componentName}
                                  </Text>
                                </div>
                                <div style={{ marginLeft: '16px' }}>
                                  {groupedEntries[date][componentName].map((entry, index) => (
                                    <div
                                      key={index}
                                      style={{
                                        marginBottom: '8px',
                                        padding: '8px 12px',
                                        backgroundColor: '#fafafa',
                                        borderRadius: '6px',
                                        borderLeft: '3px solid #1890ff',
                                        transition: 'all 0.2s',
                                        boxShadow: '0 1px 2px rgba(0,0,0,0.04)',
                                      }}
                                      onMouseEnter={(e) => {
                                        e.currentTarget.style.backgroundColor = '#f0f7ff';
                                        e.currentTarget.style.boxShadow = '0 2px 4px rgba(0,0,0,0.08)';
                                      }}
                                      onMouseLeave={(e) => {
                                        e.currentTarget.style.backgroundColor = '#fafafa';
                                        e.currentTarget.style.boxShadow = '0 1px 2px rgba(0,0,0,0.04)';
                                      }}
                                    >
                                      <div style={{ fontSize: '13px', color: '#595959', lineHeight: '1.7' }}>
                                        <Text style={{ color: '#1890ff', fontWeight: 500 }}>{entry.testMethod}</Text>
                                        {entry.detectionContent && entry.detectionContent !== '无' && (
                                          <Text>：{entry.detectionContent}</Text>
                                        )}
                                        {entry.hasDefect && entry.hasDefect !== '未设置' && (
                                          <Text
                                            style={{
                                              color:
                                                entry.hasDefect === '是'
                                                  ? '#ff4d4f'
                                                  : entry.hasDefect === '否'
                                                    ? '#52c41a'
                                                    : '#595959',
                                              fontWeight: 500,
                                              marginLeft: '4px',
                                            }}
                                          >
                                            ，
                                            {entry.hasDefect === '是'
                                              ? '存在缺陷'
                                              : entry.hasDefect === '否'
                                                ? '未见缺陷'
                                                : entry.hasDefect}
                                          </Text>
                                        )}
                                        {entry.inspector && entry.inspector !== '未指定' && (
                                          <Text style={{ marginLeft: '4px' }}>
                                            ，检测人员：<Text style={{ color: '#1890ff' }}>{entry.inspector}</Text>
                                          </Text>
                                        )}
                                      </div>
                                    </div>
                                  ))}
                                </div>
                              </div>
                            ))}
                        </div>
                      </div>
                    </Timeline.Item>
                  ))}
              </Timeline>
            </div>
          )
        ) : (
          <div style={scrollBoxStyle} className="detection-log-scroll">
            <style>{scrollbarCss}</style>
            {renderAggregateBody()}
          </div>
        )}
      </Card>

      <Modal
        title="生成检测通知单"
        open={notificationModalOpen}
        onCancel={() => setNotificationModalOpen(false)}
        width={720}
        footer={[
          <Tag
            key="status"
            color={
              generatingNotification || isNotificationRunning
                ? 'processing'
                : notificationJob?.status === 'SUCCEEDED'
                  ? 'success'
                  : notificationJob?.status === 'FAILED'
                    ? 'error'
                    : 'default'
            }
            style={{ marginRight: 8 }}
          >
            {generatingNotification || isNotificationRunning
              ? '后台生成中'
              : notificationJob?.status === 'SUCCEEDED'
                ? '已生成可下载'
                : notificationJob?.status === 'FAILED'
                  ? (notificationJob.errorMessage || '生成失败')
                  : '未生成'}
          </Tag>,
          <Button key="cancel" onClick={() => setNotificationModalOpen(false)}>
            关闭
          </Button>,
          <Button
            key="prepare"
            type="primary"
            loading={generatingNotification}
            disabled={generatingNotification || isNotificationRunning}
            onClick={handlePrepareNotification}
          >
            预生成
          </Button>,
          <Button
            key="download"
            onClick={handleDownloadNotification}
            disabled={notificationJob?.status !== 'SUCCEEDED'}
          >
            下载
          </Button>,
        ]}
        destroyOnClose
      >
        {defectDateKeys.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '24px', color: '#999' }}>
            暂无存在缺陷的检测记录（需填写检测日期且标记为存在缺陷）
          </div>
        ) : (
          <div
            style={{ maxHeight: 420, overflowY: 'auto', paddingRight: 8 }}
            className="detection-log-scroll"
          >
            <style>{scrollbarCss}</style>
            <div
              style={{
                marginBottom: 12,
                paddingBottom: 12,
                borderBottom: '1px solid #f0f0f0',
              }}
            >
              <Checkbox
                indeterminate={selectAllIndeterminate}
                checked={allDefectSelected}
                onChange={(e) => selectAllDefectReports(e.target.checked)}
              >
                全选（{selectedReportIds.length}/{allDefectReportIds.length}）
              </Checkbox>
            </div>
            <Timeline mode="left" style={{ paddingTop: 8 }}>
              {defectDateKeys.map((date) => (
                <Timeline.Item key={`defect-${date}`} color="#ff4d4f" style={{ paddingBottom: 20 }}>
                  <div style={{ marginBottom: 12 }}>
                    <Text strong style={{ fontSize: '15px', color: '#ff4d4f', fontWeight: 600 }}>
                      {date}：
                    </Text>
                    <div style={{ marginLeft: 24, marginTop: 10 }}>
                      {Object.keys(groupedDefectReports[date])
                        .sort()
                        .map((componentName) => (
                          <div key={componentName} style={{ marginBottom: 16 }}>
                            <div style={{ marginBottom: 8 }}>
                              <Text strong style={{ fontSize: '14px', color: '#262626' }}>
                                {componentName}
                              </Text>
                            </div>
                            <div style={{ marginLeft: 16 }}>
                              {groupedDefectReports[date][componentName].map((report) => {
                                const rid = report.id;
                                if (rid == null) return null;
                                const content = detectionContentTextForReport(report);
                                const method = report.testMethod || '未指定检测方法';
                                const inspector = report.inspector || '未指定';
                                return (
                                  <div
                                    key={rid}
                                    style={{
                                      marginBottom: 8,
                                      padding: '8px 12px',
                                      backgroundColor: '#fff7f7',
                                      borderRadius: 6,
                                      borderLeft: '3px solid #ff4d4f',
                                      display: 'flex',
                                      alignItems: 'flex-start',
                                      gap: 8,
                                    }}
                                  >
                                    <Checkbox
                                      checked={selectedReportIds.includes(rid)}
                                      onChange={() => toggleReportSelection(rid)}
                                    />
                                    <div style={{ flex: 1, fontSize: 13, color: '#595959', lineHeight: 1.7 }}>
                                      <Text style={{ color: '#ff4d4f', fontWeight: 500 }}>{method}</Text>
                                      {content && content !== '无' && <Text>：{content}</Text>}
                                      <Text style={{ color: '#ff4d4f', fontWeight: 500, marginLeft: 4 }}>
                                        ，存在缺陷
                                      </Text>
                                      {inspector !== '未指定' && (
                                        <Text style={{ marginLeft: 4 }}>
                                          ，检测人员：<Text style={{ color: '#1890ff' }}>{inspector}</Text>
                                        </Text>
                                      )}
                                    </div>
                                  </div>
                                );
                              })}
                            </div>
                          </div>
                        ))}
                    </div>
                  </div>
                </Timeline.Item>
              ))}
            </Timeline>
          </div>
        )}
      </Modal>
    </>
  );
};

export default DetectionLog;
