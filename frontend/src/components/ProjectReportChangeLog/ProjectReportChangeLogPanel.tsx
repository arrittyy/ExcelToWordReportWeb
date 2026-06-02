import React, { useEffect } from 'react';
import { Card, Table, Tag, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import dayjs from 'dayjs';
import {
  projectService,
  type ReportChangeLogEntry,
  type ReportChangeLogSummaryRow,
} from '@/services/projectService';

const { Text } = Typography;

const ACTION_LABELS: Record<string, string> = {
  CREATED: '新建',
  UPDATED: '更新',
  DELETED: '删除',
};

const ACTION_COLORS: Record<string, string> = {
  CREATED: 'green',
  UPDATED: 'blue',
  DELETED: 'red',
};

const FIELD_LABELS: Record<string, string> = {
  experimentTypeId: '检测类型',
  title: '标题',
  reportNumber: '报告编号',
  testMethod: '检测方法',
  status: '状态',
  hasDefect: '是否存在缺陷',
  inspector: '检测人员',
  testDate: '检测日期',
  location: '检测地点',
  componentName: '部件名称',
  equipmentName: '设备名称',
  instrumentModel: '仪器型号',
  instrumentNumber: '仪器编号',
};

function formatChangeSummary(entry: ReportChangeLogEntry): string {
  const summary = entry.changeSummary;
  if (!summary || typeof summary !== 'object') {
    return '—';
  }
  const fields = (summary as { fields?: string[] }).fields;
  if (!fields || fields.length === 0) {
    return entry.action === 'UPDATED' ? '保存（检测数据等）' : '—';
  }
  return fields.map((f) => FIELD_LABELS[f] ?? f).join('、');
}

interface ProjectReportChangeLogPanelProps {
  projectId: number;
  /** 当前 Tab 是否选中；切到本 Tab 时主动拉取最新记录 */
  active?: boolean;
}

const ProjectReportChangeLogPanel: React.FC<ProjectReportChangeLogPanelProps> = ({
  projectId,
  active = true,
}) => {
  const { data: summary, isLoading: summaryLoading, refetch: refetchSummary } = useQuery({
    queryKey: ['report-change-summary', projectId],
    queryFn: () => projectService.getReportChangeSummary(projectId),
    enabled: !!projectId,
    staleTime: 0,
  });

  const { data: logs = [], isLoading: logsLoading, refetch: refetchLogs } = useQuery({
    queryKey: ['report-change-logs', projectId],
    queryFn: () => projectService.getReportChangeLogs(projectId),
    enabled: !!projectId,
    staleTime: 0,
  });

  useEffect(() => {
    if (active && projectId) {
      void refetchSummary();
      void refetchLogs();
    }
  }, [active, projectId, refetchSummary, refetchLogs]);

  const summaryColumns = [
    {
      title: '检测类型',
      dataIndex: 'experimentTypeName',
      key: 'experimentTypeName',
      render: (_: string, row: ReportChangeLogSummaryRow) => (
        <span>
          {row.experimentTypeName}
          <Text type="secondary" style={{ marginLeft: 8, fontSize: 12 }}>
            ({row.experimentTypeCode})
          </Text>
        </span>
      ),
    },
    {
      title: '新建',
      dataIndex: 'createdCount',
      key: 'createdCount',
      width: 72,
      align: 'center' as const,
    },
    {
      title: '更新',
      dataIndex: 'updatedCount',
      key: 'updatedCount',
      width: 72,
      align: 'center' as const,
    },
    {
      title: '删除',
      dataIndex: 'deletedCount',
      key: 'deletedCount',
      width: 72,
      align: 'center' as const,
    },
    {
      title: '当前剩余',
      dataIndex: 'currentReportCount',
      key: 'currentReportCount',
      width: 88,
      align: 'center' as const,
    },
  ];

  const logColumns = [
    {
      title: '时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (v: string) => dayjs(v).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: '操作',
      dataIndex: 'action',
      key: 'action',
      width: 72,
      render: (action: string) => (
        <Tag color={ACTION_COLORS[action] ?? 'default'}>{ACTION_LABELS[action] ?? action}</Tag>
      ),
    },
    {
      title: '检测类型',
      dataIndex: 'experimentTypeName',
      key: 'experimentTypeName',
      width: 140,
    },
    {
      title: '报告编号',
      dataIndex: 'reportNumber',
      key: 'reportNumber',
      width: 160,
      render: (num: string | undefined, row: ReportChangeLogEntry) => (
        <span>
          {num || '—'}
          {row.reportDeleted && (
            <Tag color="default" style={{ marginLeft: 6 }}>
              已删除
            </Tag>
          )}
        </span>
      ),
    },
    {
      title: '报告ID',
      dataIndex: 'reportId',
      key: 'reportId',
      width: 80,
    },
    {
      title: '变更摘要',
      key: 'changeSummary',
      ellipsis: true,
      render: (_: unknown, row: ReportChangeLogEntry) => formatChangeSummary(row),
    },
    {
      title: '操作人',
      dataIndex: 'operatorUserName',
      key: 'operatorUserName',
      width: 100,
      render: (name: string | undefined) => name || '—',
    },
  ];

  const visibleLogs = logs.filter((l) => l.source !== 'SYSTEM_WORD_NUMBER');

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Card className="project-section-card" title="按检测类型统计" size="small">
        <Table
          rowKey={(row) => String(row.experimentTypeId)}
          columns={summaryColumns}
          dataSource={summary?.byExperimentType ?? []}
          loading={summaryLoading}
          pagination={false}
          size="small"
          locale={{ emptyText: '暂无变更记录' }}
        />
      </Card>
      <Card className="project-section-card" title="变更时间线" size="small">
        <Table
          rowKey="id"
          columns={logColumns}
          dataSource={visibleLogs}
          loading={logsLoading}
          pagination={{ pageSize: 20, showSizeChanger: true }}
          size="small"
          scroll={{ x: 960 }}
          locale={{ emptyText: '暂无变更记录' }}
        />
      </Card>
    </div>
  );
};

export default ProjectReportChangeLogPanel;
