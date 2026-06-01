import React, { useMemo, useState } from 'react';
import { Table, Button, Space, Tag, Popconfirm, message, Typography, Tooltip } from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  FileWordOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { reportService } from '@/services/reportService';
import type { ColumnsType } from 'antd/es/table';
import type { ReportList } from '@/types';
import dayjs from 'dayjs';

const { Title } = Typography;

const ReportListPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  const { data: reports = [], isLoading } = useQuery({
    queryKey: ['reports'],
    queryFn: reportService.getAll,
  });

  const deleteMutation = useMutation({
    mutationFn: reportService.delete,
    onSuccess: () => {
      message.success('报告已删除');
      queryClient.invalidateQueries({ queryKey: ['reports'] });
    },
  });

  const handleGenerateWord = async (id: number, reportNumber: string) => {
    try {
      message.loading({ content: '正在生成Word文档...', key: 'word', duration: 0 });
      const blob = await reportService.generateWord(id);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${reportNumber}_${dayjs().format('YYYYMMDDHHmmss')}.docx`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      message.success({ content: 'Word文档生成成功！', key: 'word' });
    } catch (error) {
      message.error({ content: 'Word文档生成失败', key: 'word' });
    }
  };

  const orderedSelectedIds = useMemo(() => {
    const set = new Set(selectedRowKeys.map(Number));
    return reports.filter((r) => set.has(r.id)).map((r) => r.id);
  }, [reports, selectedRowKeys]);

  const handleMergeGenerateWord = async () => {
    if (orderedSelectedIds.length < 2) {
      message.warning('请至少选择 2 条报告');
      return;
    }
    try {
      message.loading({ content: '正在合并生成正式单项 Word...', key: 'word-merge', duration: 0 });
      const blob = await reportService.batchGenerateWordMerged(orderedSelectedIds);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `合并单项报告_${dayjs().format('YYYYMMDDHHmmss')}.docx`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      message.success({ content: '合并 Word 生成成功！', key: 'word-merge' });
      setSelectedRowKeys([]);
    } catch {
      message.error({ content: '合并生成 Word 失败', key: 'word-merge' });
    }
  };

  const columns: ColumnsType<ReportList> = [
    {
      title: '报告编号',
      dataIndex: 'reportNumber',
      key: 'reportNumber',
      width: 180,
    },
    {
      title: '标题',
      dataIndex: 'title',
      key: 'title',
      width: 200,
    },
    {
      title: '项目名称',
      dataIndex: 'projectName',
      key: 'projectName',
      width: 150,
    },
    {
      title: '检测日期',
      dataIndex: 'testDate',
      key: 'testDate',
      width: 120,
      render: (date: string) => dayjs(date).format('YYYY-MM-DD'),
    },
    {
      title: '地点',
      dataIndex: 'location',
      key: 'location',
      width: 120,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => (
        <Tag color={status === 'Completed' ? 'success' : 'warning'}>
          {status === 'Completed' ? '已完成' : '草稿'}
        </Tag>
      ),
    },
    {
      title: '检测项',
      dataIndex: 'itemCount',
      key: 'itemCount',
      width: 80,
      render: (count: number) => `${count} 项`,
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 150,
      render: (date: string) => dayjs(date).format('YYYY-MM-DD HH:mm'),
    },
    {
      title: '操作',
      key: 'action',
      width: 250,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => navigate(`/reports/${record.id}`)}
          >
            查看
          </Button>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => navigate(`/reports/${record.id}/edit`)}
          >
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            icon={<FileWordOutlined />}
            onClick={() => handleGenerateWord(record.id, record.reportNumber)}
          >
            生成Word
          </Button>
          <Popconfirm
            title="确定删除这个报告吗？"
            description="此操作不可恢复"
            onConfirm={() => deleteMutation.mutate(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={2}>报告管理</Title>
        <Space>
          <Tooltip title={orderedSelectedIds.length < 2 ? '请至少选择 2 条报告' : undefined}>
            <Button
              type="default"
              icon={<FileWordOutlined />}
              disabled={orderedSelectedIds.length < 2}
              onClick={() => void handleMergeGenerateWord()}
            >
              合并生成正式单项 Word
              {orderedSelectedIds.length > 0 ? ` (${orderedSelectedIds.length})` : ''}
            </Button>
          </Tooltip>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            size="large"
            onClick={() => navigate('/reports/new')}
          >
            创建新报告
          </Button>
        </Space>
      </div>

      <Table
        columns={columns}
        dataSource={reports}
        rowKey="id"
        loading={isLoading}
        rowSelection={{
          selectedRowKeys,
          onChange: (keys) => setSelectedRowKeys(keys),
        }}
        scroll={{ x: 1400 }}
        pagination={{
          pageSize: 10,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条记录`,
        }}
      />
    </div>
  );
};

export default ReportListPage;


