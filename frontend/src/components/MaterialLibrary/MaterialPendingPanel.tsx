import React, { useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  Modal,
  Space,
  Table,
  Tag,
  message,
  Descriptions,
  List,
} from 'antd';
import { CheckOutlined, CloseOutlined, HistoryOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { ColumnsType } from 'antd/es/table';
import {
  MATERIAL_CATEGORY_LABELS,
  MATERIAL_CATEGORY_FIELDS,
  MATERIAL_META_FIELDS,
} from '@/constants/materialLibraryFields';
import {
  materialLibraryService,
  MODIFICATION_TYPE_LABELS,
  type MaterialLibraryEntry,
  type MaterialModificationType,
} from '@/services/materialLibraryService';
import dayjs from 'dayjs';

interface MaterialPendingPanelProps {
  active: boolean;
}

const MaterialPendingPanel: React.FC<MaterialPendingPanelProps> = ({ active }) => {
  const queryClient = useQueryClient();
  const [rejectModal, setRejectModal] = useState<{ open: boolean; entry: MaterialLibraryEntry | null }>({
    open: false,
    entry: null,
  });
  const [logModal, setLogModal] = useState<{ open: boolean; entryId: number | null }>({
    open: false,
    entryId: null,
  });
  const [rejectForm] = Form.useForm();

  const { data: pending = [], isLoading, refetch } = useQuery({
    queryKey: ['materialLibraryPending'],
    queryFn: materialLibraryService.listPending,
    enabled: active,
  });

  const { data: logs = [], isLoading: logsLoading } = useQuery({
    queryKey: ['materialLibraryLogs', logModal.entryId],
    queryFn: () => materialLibraryService.listLogs(logModal.entryId!),
    enabled: logModal.open && logModal.entryId != null,
  });

  const approveMutation = useMutation({
    mutationFn: materialLibraryService.approve,
    onSuccess: () => {
      message.success('审核通过，材质已生效');
      refetch();
      queryClient.invalidateQueries({ queryKey: ['materialLibrary'] });
      queryClient.invalidateQueries({ queryKey: ['materialKeys'] });
      queryClient.invalidateQueries({ queryKey: ['materialLibraryCapabilities'] });
      queryClient.invalidateQueries({ queryKey: ['materialLibraryMySubmissions'] });
    },
    onError: (error: unknown) => {
      const err = error as { response?: { data?: { message?: string } } };
      message.error(err?.response?.data?.message || '审核失败');
    },
  });

  const rejectMutation = useMutation({
    mutationFn: ({ id, reviewComment }: { id: number; reviewComment: string }) =>
      materialLibraryService.reject(id, { reviewComment }),
    onSuccess: () => {
      message.success('已驳回');
      setRejectModal({ open: false, entry: null });
      rejectForm.resetFields();
      refetch();
      queryClient.invalidateQueries({ queryKey: ['materialLibraryCapabilities'] });
      queryClient.invalidateQueries({ queryKey: ['materialLibraryMySubmissions'] });
    },
    onError: (error: unknown) => {
      const err = error as { response?: { data?: { message?: string } } };
      message.error(err?.response?.data?.message || '驳回失败');
    },
  });

  const columns: ColumnsType<MaterialLibraryEntry> = [
    { title: '材质牌号', dataIndex: 'materialKey', key: 'materialKey', width: 140 },
    {
      title: '变更类型',
      dataIndex: 'modificationType',
      key: 'modificationType',
      width: 100,
      render: (type: MaterialModificationType) => {
        const label = type ? MODIFICATION_TYPE_LABELS[type] : '—';
        const color = type === 'DELETE' ? 'red' : type === 'UPDATE' ? 'blue' : 'green';
        return type ? <Tag color={color}>{label}</Tag> : '—';
      },
    },
    {
      title: '分类',
      dataIndex: 'primaryCategory',
      key: 'primaryCategory',
      width: 120,
      render: (cat: string) => MATERIAL_CATEGORY_LABELS[cat as keyof typeof MATERIAL_CATEGORY_LABELS] || cat,
    },
    { title: '提交人', dataIndex: 'submittedByUserName', key: 'submittedByUserName', width: 120 },
    {
      title: '提交时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (v: string) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '—'),
    },
    {
      title: '操作',
      key: 'actions',
      fixed: 'right',
      width: 260,
      render: (_: unknown, record) => (
        <Space wrap>
          <Button
            type="link"
            icon={<HistoryOutlined />}
            onClick={() => setLogModal({ open: true, entryId: record.id! })}
          >
            日志
          </Button>
          <Button
            type="link"
            icon={<CheckOutlined />}
            loading={approveMutation.isPending}
            onClick={() => approveMutation.mutate(record.id!)}
          >
            通过
          </Button>
          <Button
            type="link"
            danger
            icon={<CloseOutlined />}
            onClick={() => setRejectModal({ open: true, entry: record })}
          >
            驳回
          </Button>
        </Space>
      ),
    },
  ];

  const renderProperties = (entry: MaterialLibraryEntry | null) => {
    if (!entry) return null;
    const cat = entry.primaryCategory;
    const categoryFields = cat ? MATERIAL_CATEGORY_FIELDS[cat] : [];
    return (
      <Descriptions column={2} size="small" bordered style={{ marginBottom: 16 }}>
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
        dataSource={pending}
        loading={isLoading}
        expandable={{
          expandedRowRender: (record) => renderProperties(record),
        }}
        pagination={{ pageSize: 10, showTotal: (t) => `待审核 ${t} 条` }}
        locale={{ emptyText: '暂无待审核材质' }}
      />

      <Modal
        title={`驳回：${rejectModal.entry?.materialKey ?? ''}`}
        open={rejectModal.open}
        onCancel={() => {
          setRejectModal({ open: false, entry: null });
          rejectForm.resetFields();
        }}
        onOk={async () => {
          const values = await rejectForm.validateFields();
          if (rejectModal.entry?.id) {
            rejectMutation.mutate({ id: rejectModal.entry.id, reviewComment: values.reviewComment });
          }
        }}
        confirmLoading={rejectMutation.isPending}
        destroyOnClose
      >
        {renderProperties(rejectModal.entry)}
        <Form form={rejectForm} layout="vertical">
          <Form.Item
            name="reviewComment"
            label="驳回原因"
            rules={[{ required: true, message: '请填写驳回原因' }, { max: 500 }]}
          >
            <Input.TextArea rows={3} placeholder="请说明驳回原因，便于提交人修改" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="审批日志"
        open={logModal.open}
        onCancel={() => setLogModal({ open: false, entryId: null })}
        footer={null}
      >
        <List
          loading={logsLoading}
          dataSource={logs}
          locale={{ emptyText: '暂无日志' }}
          renderItem={(item) => (
            <List.Item>
              <Space direction="vertical" size={0} style={{ width: '100%' }}>
                <Space>
                  <Tag>{item.action}</Tag>
                  <span>{item.actorUserName}</span>
                  <span style={{ color: '#999' }}>
                    {dayjs(item.createdAt).format('YYYY-MM-DD HH:mm')}
                  </span>
                </Space>
                {item.comment && <span style={{ color: '#666' }}>{item.comment}</span>}
              </Space>
            </List.Item>
          )}
        />
      </Modal>
    </Card>
  );
};

export default MaterialPendingPanel;
