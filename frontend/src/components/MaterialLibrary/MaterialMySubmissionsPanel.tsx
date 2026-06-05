import React, { useState } from 'react';
import {
  Button,
  Card,
  Descriptions,
  Popconfirm,
  Space,
  Table,
  Tag,
  message,
} from 'antd';
import { EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { ColumnsType } from 'antd/es/table';
import {
  MATERIAL_CATEGORY_FIELDS,
  MATERIAL_CATEGORY_LABELS,
  MATERIAL_META_FIELDS,
  type MaterialCategory,
} from '@/constants/materialLibraryFields';
import {
  materialLibraryService,
  MODIFICATION_TYPE_LABELS,
  STATUS_LABELS,
  type MaterialLibraryEntry,
  type MaterialEntryStatus,
  type MaterialModificationType,
} from '@/services/materialLibraryService';
import MaterialFormModal from './MaterialFormModal';
import dayjs from 'dayjs';

interface MaterialMySubmissionsPanelProps {
  active: boolean;
}

const statusColor: Record<MaterialEntryStatus, string> = {
  PENDING: 'processing',
  APPROVED: 'success',
  REJECTED: 'error',
  DELETED: 'default',
};

const resolveSubmissionStatus = (record: MaterialLibraryEntry): MaterialEntryStatus =>
  record.submissionStatus ?? record.status;

const resolveSubmissionStatusLabel = (record: MaterialLibraryEntry): string => {
  const submissionStatus = resolveSubmissionStatus(record);
  if (
    submissionStatus === 'REJECTED' &&
    record.status === 'APPROVED' &&
    (record.modificationType === 'UPDATE' || record.modificationType === 'DELETE')
  ) {
    return '变更已驳回';
  }
  return STATUS_LABELS[submissionStatus] || submissionStatus;
};

const isSubmissionRejected = (record: MaterialLibraryEntry): boolean =>
  resolveSubmissionStatus(record) === 'REJECTED';

const isModificationRejected = (record: MaterialLibraryEntry): boolean =>
  record.status === 'APPROVED' && !!(record.reviewComment?.trim());

const canDismissSubmission = (record: MaterialLibraryEntry): boolean =>
  record.status === 'PENDING' || record.status === 'REJECTED' || isModificationRejected(record);

const MaterialMySubmissionsPanel: React.FC<MaterialMySubmissionsPanelProps> = ({ active }) => {
  const queryClient = useQueryClient();
  const [editEntry, setEditEntry] = useState<MaterialLibraryEntry | null>(null);
  const [modalOpen, setModalOpen] = useState(false);

  const { data: submissions = [], isLoading, refetch } = useQuery({
    queryKey: ['materialLibraryMySubmissions'],
    queryFn: materialLibraryService.listMySubmissions,
    enabled: active,
  });

  const invalidateAfterDismiss = () => {
    refetch();
    queryClient.invalidateQueries({ queryKey: ['materialLibrary'] });
    queryClient.invalidateQueries({ queryKey: ['materialLibraryCapabilities'] });
  };

  const deleteDraftMutation = useMutation({
    mutationFn: materialLibraryService.deleteDraft,
    onError: (error: unknown) => {
      const err = error as { response?: { data?: { message?: string } } };
      message.error(err?.response?.data?.message || '操作失败');
    },
  });

  const columns: ColumnsType<MaterialLibraryEntry> = [
    { title: '材质牌号', dataIndex: 'materialKey', key: 'materialKey', width: 140 },
    {
      title: '分类',
      dataIndex: 'primaryCategory',
      key: 'primaryCategory',
      width: 120,
      render: (cat: string) => MATERIAL_CATEGORY_LABELS[cat as MaterialCategory] || cat,
    },
    {
      title: '变更类型',
      dataIndex: 'modificationType',
      key: 'modificationType',
      width: 90,
      render: (type: MaterialModificationType) =>
        type ? <Tag>{MODIFICATION_TYPE_LABELS[type]}</Tag> : '—',
    },
    {
      title: '状态',
      key: 'submissionStatus',
      width: 110,
      render: (_: unknown, record) => {
        const submissionStatus = resolveSubmissionStatus(record);
        return (
          <Tag color={statusColor[submissionStatus]}>
            {resolveSubmissionStatusLabel(record)}
          </Tag>
        );
      },
    },
    {
      title: '驳回原因',
      dataIndex: 'reviewComment',
      key: 'reviewComment',
      width: 200,
      ellipsis: true,
      render: (v: string) => v || '—',
    },
    {
      title: '提交时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (v: string) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '—'),
    },
    {
      title: '操作',
      key: 'actions',
      fixed: 'right',
      width: 180,
      render: (_: unknown, record) => (
        <Space wrap>
          {isSubmissionRejected(record) && (
            <Button
              type="link"
              icon={<EditOutlined />}
              onClick={() => {
                setEditEntry(record);
                setModalOpen(true);
              }}
            >
              修改重提
            </Button>
          )}
          {canDismissSubmission(record) && record.id && (
            <Popconfirm
              title={
                isModificationRejected(record)
                  ? '确认消除该驳回提示？材质将保持当前已通过版本。'
                  : '确认撤销该提交？'
              }
              onConfirm={() =>
                deleteDraftMutation.mutate(record.id!, {
                  onSuccess: () => {
                    message.success(
                      isModificationRejected(record) ? '已消除驳回提示' : '已撤销提交',
                    );
                    invalidateAfterDismiss();
                  },
                })
              }
              okText="确认"
              cancelText="取消"
            >
              <Button type="link" danger icon={<DeleteOutlined />}>
                {isModificationRejected(record) ? '消除提示' : '撤销'}
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  const renderProperties = (entry: MaterialLibraryEntry) => {
    const cat = entry.primaryCategory as MaterialCategory | undefined;
    const categoryFields = cat ? MATERIAL_CATEGORY_FIELDS[cat] : [];
    return (
      <Descriptions column={2} size="small" bordered>
        {MATERIAL_META_FIELDS.map((f) => (
          <Descriptions.Item key={f.key} label={f.label}>
            {entry.properties?.[f.key] || '—'}
          </Descriptions.Item>
        ))}
        {categoryFields.map((f) => (
          <Descriptions.Item key={f.key} label={f.label}>
            {entry.properties?.[f.key] || '—'}
          </Descriptions.Item>
        ))}
      </Descriptions>
    );
  };

  return (
    <Card className="project-section-card">
      <Table
        className="material-library-table"
        bordered
        rowKey="id"
        columns={columns}
        dataSource={submissions}
        loading={isLoading}
        expandable={{
          expandedRowRender: renderProperties,
        }}
        pagination={{ pageSize: 10, showTotal: (t) => `共 ${t} 条` }}
        locale={{ emptyText: '暂无提交记录' }}
      />

      {editEntry && (
        <MaterialFormModal
          open={modalOpen}
          mode="edit"
          category={(editEntry.primaryCategory as MaterialCategory) || 'alloy'}
          entry={editEntry}
          onClose={() => {
            setModalOpen(false);
            setEditEntry(null);
          }}
          onSuccess={() => {
            refetch();
            queryClient.invalidateQueries({ queryKey: ['materialLibrary'] });
            queryClient.invalidateQueries({ queryKey: ['materialLibraryCapabilities'] });
          }}
        />
      )}
    </Card>
  );
};

export default MaterialMySubmissionsPanel;
