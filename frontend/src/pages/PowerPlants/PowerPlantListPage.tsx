import React, { useState } from 'react';
import {
  Card,
  Table,
  Button,
  Space,
  Typography,
  message,
  Input,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { powerPlantService, PowerPlant } from '@/services/powerPlantService';

const { Title } = Typography;

// 大区选项常量
const REGIONS = [
  '华北大区',
  '华中大区',
  '华东大区',
  '华南大区',
  '北方大区',
  '东北大区',
  '中西大区',
  '外部客户'
];

const PowerPlantListPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchText, setSearchText] = useState('');

  const { data: powerPlantsData = [], isLoading, error } = useQuery({
    queryKey: ['powerPlants'],
    queryFn: powerPlantService.getAll,
  });

  // 调试信息
  React.useEffect(() => {
    if (error) {
      console.error('获取电厂列表失败:', error);
      message.error('获取电厂列表失败，请检查网络连接');
    }
    if (powerPlantsData) {
      console.log('电厂数据:', powerPlantsData);
    }
  }, [error, powerPlantsData]);

  // 按大区排序：使用 REGIONS 数组的索引作为排序依据
  const sortedPowerPlants = React.useMemo(() => {
    return [...powerPlantsData].sort((a, b) => {
      const indexA = REGIONS.indexOf(a.region);
      const indexB = REGIONS.indexOf(b.region);
      // 如果大区不在预定义列表中，排在最后
      if (indexA === -1 && indexB === -1) return 0;
      if (indexA === -1) return 1;
      if (indexB === -1) return -1;
      // 按大区索引排序，如果大区相同则按名称排序
      if (indexA !== indexB) return indexA - indexB;
      return a.name.localeCompare(b.name, 'zh-CN');
    });
  }, [powerPlantsData]);

  const deleteMutation = useMutation({
    mutationFn: powerPlantService.delete,
    onSuccess: () => {
      message.success('电厂删除成功');
      queryClient.invalidateQueries({ queryKey: ['powerPlants'] });
    },
    onError: () => {
      message.error('删除失败');
    },
  });

  const handleDelete = (id: number) => {
    deleteMutation.mutate(id);
  };

  // 过滤数据：只搜索电厂名称和大区
  const filteredPowerPlants = React.useMemo(() => {
    if (!searchText) return sortedPowerPlants;
    const search = searchText.toLowerCase();
    return sortedPowerPlants.filter(
      (record) =>
        record.name.toLowerCase().includes(search) ||
        record.region.toLowerCase().includes(search)
    );
  }, [sortedPowerPlants, searchText]);

  const columns: ColumnsType<PowerPlant> = [
    {
      title: '大区',
      dataIndex: 'region',
      key: 'region',
      width: 150,
    },
    {
      title: '电厂名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      fixed: 'right',
      render: (_, record) => (
        <Space size="middle">
          <Button
            type="link"
            icon={<EyeOutlined />}
            onClick={() => navigate(`/power-plants/${record.id}`)}
          >
            查看
          </Button>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => navigate(`/power-plants/${record.id}/edit`)}
          >
            编辑
          </Button>
          <Button
            type="link"
            danger
            icon={<DeleteOutlined />}
            onClick={() => {
              if (window.confirm('确定要删除这个电厂吗？删除后将同时删除其下的所有机组和部件。')) {
                handleDelete(record.id);
              }
            }}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <Title level={2}>电厂数据管理</Title>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          size="large"
          onClick={() => navigate('/power-plants/new')}
        >
          添加电厂
        </Button>
      </div>

      <Card style={{ borderRadius: 20 }}>
        <div style={{ marginBottom: 16 }}>
          <Input
            placeholder="搜索电厂名称、大区"
            prefix={<SearchOutlined />}
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            allowClear
            style={{ width: 300 }}
          />
        </div>

        <Table
          columns={columns}
          dataSource={filteredPowerPlants}
          rowKey="id"
          loading={isLoading}
          locale={{
            emptyText: '暂无电厂数据，请点击"添加电厂"按钮创建',
          }}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
          }}
        />
      </Card>
    </div>
  );
};

export default PowerPlantListPage;
