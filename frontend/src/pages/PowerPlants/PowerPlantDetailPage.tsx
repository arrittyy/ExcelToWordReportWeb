import React, { useState, useEffect } from 'react';
import {
  Card,
  Descriptions,
  Button,
  Space,
  Typography,
  Table,
  message,
  Modal,
  Form,
  Input,
  Select,
  Popconfirm,
  Tabs,
} from 'antd';
import {
  EditOutlined,
  PlusOutlined,
  DeleteOutlined,
  ArrowLeftOutlined,
} from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { powerPlantService, unitService, unitComponentService, Unit, UnitComponent } from '@/services/powerPlantService';
import type { ColumnsType } from 'antd/es/table';

const { Title } = Typography;
const { TextArea } = Input;

const PowerPlantDetailPage: React.FC = () => {
  const navigate = useNavigate();
  const { id } = useParams();
  const queryClient = useQueryClient();
  const [unitForm] = Form.useForm();
  const [componentForm] = Form.useForm();
  const [unitModalVisible, setUnitModalVisible] = useState(false);
  const [componentModalVisible, setComponentModalVisible] = useState(false);
  const [editingUnit, setEditingUnit] = useState<Unit | null>(null);
  const [editingComponent, setEditingComponent] = useState<UnitComponent | null>(null);
  const [selectedUnitId, setSelectedUnitId] = useState<number | null>(null);
  const [activeTabKey, setActiveTabKey] = useState<string>('');

  const { data: powerPlant, isLoading } = useQuery({
    queryKey: ['powerPlant', id],
    queryFn: () => powerPlantService.getById(Number(id)),
  });

  const { data: units = [] } = useQuery({
    queryKey: ['units', id],
    queryFn: () => unitService.getByPowerPlantId(Number(id)),
    enabled: !!id,
  });

  // 当机组列表加载后，自动选择第一个机组
  useEffect(() => {
    if (units.length > 0 && !activeTabKey) {
      setActiveTabKey(units[0].id.toString());
      setSelectedUnitId(units[0].id);
    }
  }, [units, activeTabKey]);

  const createUnitMutation = useMutation({
    mutationFn: (data: any) => unitService.create(Number(id), data),
    onSuccess: (newUnit) => {
      message.success('机组创建成功');
      queryClient.invalidateQueries({ queryKey: ['units', id] });
      setUnitModalVisible(false);
      unitForm.resetFields();
      // 自动切换到新创建的机组
      if (newUnit?.id) {
        setActiveTabKey(newUnit.id.toString());
        setSelectedUnitId(newUnit.id);
      }
    },
  });

  const updateUnitMutation = useMutation({
    mutationFn: (data: { id: number; data: any }) => unitService.update(data.id, data.data),
    onSuccess: () => {
      message.success('机组更新成功');
      queryClient.invalidateQueries({ queryKey: ['units', id] });
      setUnitModalVisible(false);
      setEditingUnit(null);
      unitForm.resetFields();
    },
  });

  const deleteUnitMutation = useMutation({
    mutationFn: unitService.delete,
    onSuccess: () => {
      message.success('机组删除成功');
      queryClient.invalidateQueries({ queryKey: ['units', id] });
    },
  });

  const handleAddUnit = () => {
    setEditingUnit(null);
    setUnitModalVisible(true);
    unitForm.resetFields();
  };

  const handleEditUnit = (unit: Unit) => {
    setEditingUnit(unit);
    setUnitModalVisible(true);
    unitForm.setFieldsValue({
      unitNumber: unit.unitNumber,
      installedCapacity: unit.installedCapacity,
    });
  };

  const handleUnitSubmit = (values: any) => {
    if (editingUnit) {
      updateUnitMutation.mutate({ id: editingUnit.id, data: values });
    } else {
      createUnitMutation.mutate(values);
    }
  };

  // 获取当前选中机组的部件列表
  const { data: unitComponents = [] } = useQuery({
    queryKey: ['unitComponents', selectedUnitId],
    queryFn: () => unitComponentService.getByUnitId(selectedUnitId!),
    enabled: !!selectedUnitId,
  });

  const componentColumns: ColumnsType<UnitComponent> = [
    {
      title: '部件名称',
      dataIndex: 'componentName',
      key: 'componentName',
    },
    {
      title: '类别',
      dataIndex: 'category',
      key: 'category',
    },
    {
      title: '材质',
      dataIndex: 'material',
      key: 'material',
    },
    {
      title: '管径',
      dataIndex: 'pipeDiameter',
      key: 'pipeDiameter',
    },
    {
      title: '壁厚',
      dataIndex: 'wallThickness',
      key: 'wallThickness',
    },
    {
      title: '备注',
      dataIndex: 'remark',
      key: 'remark',
      ellipsis: true,
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            onClick={() => {
              setEditingComponent(record);
              setComponentModalVisible(true);
            }}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定要删除这个部件吗？"
            onConfirm={() => {
              unitComponentService.delete(record.id).then(() => {
                message.success('部件删除成功');
                queryClient.invalidateQueries({ queryKey: ['unitComponents', selectedUnitId] });
              });
            }}
          >
            <Button type="link" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  if (isLoading) {
    return <div>加载中...</div>;
  }

  if (!powerPlant) {
    return <div>电厂不存在</div>;
  }

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/power-plants')}>
          返回电厂列表
        </Button>
        <Button
          type="primary"
          icon={<EditOutlined />}
          onClick={() => navigate(`/power-plants/${id}/edit`)}
        >
          编辑电厂
        </Button>
      </Space>

      <Title level={2}>电厂详情</Title>

      <Card style={{ marginBottom: 16 }}>
        <Descriptions column={2} bordered>
          <Descriptions.Item label="电厂名称">{powerPlant.name}</Descriptions.Item>
          <Descriptions.Item label="大区">{powerPlant.region}</Descriptions.Item>
          <Descriptions.Item label="简称">{powerPlant.shortName || '-'}</Descriptions.Item>
          <Descriptions.Item label="省">{powerPlant.province}</Descriptions.Item>
          <Descriptions.Item label="市">{powerPlant.city}</Descriptions.Item>
          <Descriptions.Item label="详细地址">{powerPlant.address}</Descriptions.Item>
          <Descriptions.Item label="电话">{powerPlant.phone || '-'}</Descriptions.Item>
          <Descriptions.Item label="传真">{powerPlant.fax || '-'}</Descriptions.Item>
          <Descriptions.Item label="备注" span={2}>
            {powerPlant.remark || '-'}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card
        title="机组管理"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAddUnit}>
            添加机组
          </Button>
        }
      >
        {units.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
            暂无机组，请点击"添加机组"创建
          </div>
        ) : (
          <Tabs
            activeKey={activeTabKey}
            onChange={(key) => {
              setActiveTabKey(key);
              setSelectedUnitId(Number(key));
            }}
            type="card"
            items={units.map((unit) => ({
              key: unit.id.toString(),
              label: unit.unitNumber || `机组${unit.id}`,
              children: (
                <div>
                  <Card
                    title="机组信息"
                    style={{ marginBottom: 16 }}
                    extra={
                      <Space>
                        <Button
                          type="primary"
                          icon={<PlusOutlined />}
                          onClick={() => {
                            setComponentModalVisible(true);
                            setEditingComponent(null);
                          }}
                        >
                          添加部件
                        </Button>
                        <Button icon={<EditOutlined />} onClick={() => handleEditUnit(unit)}>
                          编辑机组
                        </Button>
                        <Popconfirm
                          title="确定要删除这个机组吗？删除后将同时删除其下的所有部件。"
                          onConfirm={() => {
                            deleteUnitMutation.mutate(unit.id);
                            // 如果删除的是当前选中的机组，切换到第一个机组
                            if (activeTabKey === unit.id.toString() && units.length > 1) {
                              const remainingUnits = units.filter((u) => u.id !== unit.id);
                              if (remainingUnits.length > 0) {
                                setActiveTabKey(remainingUnits[0].id.toString());
                                setSelectedUnitId(remainingUnits[0].id);
                              } else {
                                setActiveTabKey('');
                                setSelectedUnitId(null);
                              }
                            }
                          }}
                        >
                          <Button danger icon={<DeleteOutlined />}>
                            删除机组
                          </Button>
                        </Popconfirm>
                      </Space>
                    }
                  >
                    <Descriptions column={2} bordered>
                      <Descriptions.Item label="机组编号">
                        {unit.unitNumber || '-'}
                      </Descriptions.Item>
                      <Descriptions.Item label="机组装机容量">
                        {unit.installedCapacity || '-'}
                      </Descriptions.Item>
                    </Descriptions>
                  </Card>
                  <Card title="部件列表">
                    <Table
                      columns={componentColumns}
                      dataSource={unitComponents}
                      rowKey="id"
                      pagination={false}
                    />
                  </Card>
                </div>
              ),
            }))}
          />
        )}
      </Card>

      <Modal
        title={editingUnit ? '编辑机组' : '添加机组'}
        open={unitModalVisible}
        onCancel={() => {
          setUnitModalVisible(false);
          setEditingUnit(null);
          unitForm.resetFields();
        }}
        footer={null}
      >
        <Form form={unitForm} layout="vertical" onFinish={handleUnitSubmit}>
          <Form.Item
            label="机组编号"
            name="unitNumber"
            rules={[{ required: true, message: '请输入机组编号' }]}
          >
            <Input placeholder="请输入机组编号" />
          </Form.Item>
          <Form.Item label="机组装机容量" name="installedCapacity">
            <Input placeholder="请输入机组装机容量（如：600MW）" />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" loading={createUnitMutation.isPending || updateUnitMutation.isPending}>
                保存
              </Button>
              <Button onClick={() => {
                setUnitModalVisible(false);
                setEditingUnit(null);
                unitForm.resetFields();
              }}>
                取消
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingComponent ? '编辑部件' : '添加部件'}
        open={componentModalVisible}
        onCancel={() => {
          setComponentModalVisible(false);
          setEditingComponent(null);
          componentForm.resetFields();
        }}
        footer={null}
        width={600}
      >
        <UnitComponentForm
          unitId={selectedUnitId}
          editingComponent={editingComponent}
          onSuccess={() => {
            setComponentModalVisible(false);
            setEditingComponent(null);
            componentForm.resetFields();
            queryClient.invalidateQueries({ queryKey: ['unitComponents', selectedUnitId] });
          }}
        />
      </Modal>
    </div>
  );
};

// 部件表单组件
const UnitComponentForm: React.FC<{
  unitId: number | null;
  editingComponent: UnitComponent | null;
  onSuccess: () => void;
}> = ({ unitId, editingComponent, onSuccess }) => {
  const [form] = Form.useForm();

  React.useEffect(() => {
    if (editingComponent) {
      form.setFieldsValue({
        componentName: editingComponent.componentName,
        material: editingComponent.material,
        category: editingComponent.category,
        pipeDiameter: editingComponent.pipeDiameter,
        wallThickness: editingComponent.wallThickness,
        remark: editingComponent.remark,
      });
    }
  }, [editingComponent, form]);

  const createMutation = useMutation({
    mutationFn: (data: any) => unitComponentService.create(unitId!, data),
    onSuccess: () => {
      message.success('部件创建成功');
      onSuccess();
    },
  });

  const updateMutation = useMutation({
    mutationFn: (data: { id: number; data: any }) => unitComponentService.update(data.id, data.data),
    onSuccess: () => {
      message.success('部件更新成功');
      onSuccess();
    },
  });

  const handleSubmit = (values: any) => {
    if (editingComponent) {
      updateMutation.mutate({ id: editingComponent.id, data: values });
    } else {
      createMutation.mutate(values);
    }
  };

  return (
    <Form form={form} layout="vertical" onFinish={handleSubmit}>
      <Form.Item
        label="部件名称"
        name="componentName"
        rules={[{ required: true, message: '请输入部件名称' }]}
      >
        <Input />
      </Form.Item>
      <Form.Item
        label="类别"
        name="category"
        rules={[{ required: true, message: '请选择类别' }]}
      >
        <Select
          options={[
            { value: '汽机', label: '汽机' },
            { value: '锅炉本体', label: '锅炉本体' },
            { value: '四大管道', label: '四大管道' },
            { value: '机炉外管道', label: '机炉外管道' },
            { value: '钢结构', label: '钢结构' },
            { value: '其他', label: '其他' },
          ]}
          placeholder="请选择类别"
          style={{ width: '100%' }}
        />
      </Form.Item>
      <Form.Item label="材质" name="material">
        <Input />
      </Form.Item>
      <Form.Item label="管径" name="pipeDiameter">
        <Input />
      </Form.Item>
      <Form.Item label="壁厚" name="wallThickness">
        <Input />
      </Form.Item>
      <Form.Item label="备注" name="remark">
        <TextArea rows={3} />
      </Form.Item>
      <Form.Item>
        <Space>
          <Button
            type="primary"
            htmlType="submit"
            loading={createMutation.isPending || updateMutation.isPending}
            disabled={!unitId}
          >
            保存
          </Button>
          <Button onClick={onSuccess}>取消</Button>
        </Space>
      </Form.Item>
    </Form>
  );
};

export default PowerPlantDetailPage;
