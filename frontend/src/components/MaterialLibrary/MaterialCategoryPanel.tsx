import React, { useMemo, useState } from 'react';
import { Button, Card, Input, Popconfirm, Space, Table, Tag, message } from 'antd';
import { PlusOutlined, SearchOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { ColumnsType } from 'antd/es/table';
import {
  MATERIAL_CATEGORY_FIELDS,
  MATERIAL_META_FIELDS,
  type MaterialCategory,
} from '@/constants/materialLibraryFields';
import { materialLibraryService, type MaterialLibraryEntry } from '@/services/materialLibraryService';
import MaterialFormModal from './MaterialFormModal';

interface MaterialCategoryPanelProps {
  category: MaterialCategory;
  active: boolean;
}

const MaterialCategoryPanel: React.FC<MaterialCategoryPanelProps> = ({ category, active }) => {
  const queryClient = useQueryClient();
  const [keyword, setKeyword] = useState('');
  const [searchKeyword, setSearchKeyword] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingEntry, setEditingEntry] = useState<MaterialLibraryEntry | null>(null);

  const { data: entries = [], isLoading, refetch } = useQuery({
    queryKey: ['materialLibrary', category, searchKeyword],
    queryFn: () =>
      materialLibraryService.list({
        category,
        keyword: searchKeyword || undefined,
      }),
    enabled: active,
  });

  const deleteMutation = useMutation({
    mutationFn: materialLibraryService.deleteRequest,
    onSuccess: () => {
      message.success('删除申请已提交，等待审核');
      refetch();
      queryClient.invalidateQueries({ queryKey: ['materialLibraryCapabilities'] });
      queryClient.invalidateQueries({ queryKey: ['materialLibraryMySubmissions'] });
    },
    onError: (error: unknown) => {
      const err = error as { response?: { data?: { message?: string } } };
      message.error(err?.response?.data?.message || '删除申请失败');
    },
  });

  const columns: ColumnsType<MaterialLibraryEntry> = useMemo(() => {
    const cols: ColumnsType<MaterialLibraryEntry> = [
      {
        title: '材质牌号',
        dataIndex: 'materialKey',
        key: 'materialKey',
        width: 160,
        fixed: 'left',
        render: (key: string, record) => (
          <Space>
            <span>{key}</span>
            {record.pendingChange && (
              <Tag color="orange">变更待审核</Tag>
            )}
          </Space>
        ),
      },
    ];

    for (const field of MATERIAL_META_FIELDS) {
      cols.push({
        title: field.label,
        key: field.key,
        width: 140,
        render: (_: unknown, record) => record.properties?.[field.key] || '—',
      });
    }

    for (const field of MATERIAL_CATEGORY_FIELDS[category]) {
      cols.push({
        title: field.label,
        key: field.key,
        width: 130,
        render: (_: unknown, record) => record.properties?.[field.key] || '—',
      });
    }

    cols.push({
      title: '操作',
      key: 'actions',
      fixed: 'right',
      width: 140,
      render: (_: unknown, record) => {
        if (!record.id || record.pendingChange) {
          return '—';
        }
        return (
          <Space>
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={() => {
                setEditingEntry(record);
                setModalOpen(true);
              }}
            >
              编辑
            </Button>
            <Popconfirm
              title="确认申请删除该材质？"
              description="删除需审核通过后生效，审核期间仍使用当前标准值。"
              onConfirm={() => deleteMutation.mutate(record.id!)}
              okText="申请删除"
              cancelText="取消"
            >
              <Button type="link" size="small" danger icon={<DeleteOutlined />}>
                删除
              </Button>
            </Popconfirm>
          </Space>
        );
      },
    });

    return cols;
  }, [category, deleteMutation]);

  return (
    <Card className="project-section-card">
      <Space style={{ marginBottom: 16, width: '100%', justifyContent: 'space-between' }} wrap>
        <Space wrap>
          <Input
            placeholder="搜索牌号或标准值"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onPressEnter={() => setSearchKeyword(keyword.trim())}
            style={{ width: 260 }}
            allowClear
          />
          <Button icon={<SearchOutlined />} onClick={() => setSearchKeyword(keyword.trim())}>
            搜索
          </Button>
        </Space>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => {
            setEditingEntry(null);
            setModalOpen(true);
          }}
        >
          新增材质
        </Button>
      </Space>

      <Table
        className="material-library-table"
        bordered
        rowKey={(row) => String(row.id ?? row.materialKey)}
        columns={columns}
        dataSource={entries}
        loading={isLoading}
        scroll={{ x: 'max-content' }}
        pagination={{ pageSize: 20, showSizeChanger: true, showTotal: (t) => `共 ${t} 条` }}
        size="middle"
      />

      <MaterialFormModal
        open={modalOpen}
        mode={editingEntry ? 'edit' : 'create'}
        category={category}
        entry={editingEntry}
        onClose={() => {
          setModalOpen(false);
          setEditingEntry(null);
        }}
        onSuccess={() => {
          refetch();
          queryClient.invalidateQueries({ queryKey: ['materialLibraryCapabilities'] });
          queryClient.invalidateQueries({ queryKey: ['materialLibraryMySubmissions'] });
        }}
      />
    </Card>
  );
};

export default MaterialCategoryPanel;
