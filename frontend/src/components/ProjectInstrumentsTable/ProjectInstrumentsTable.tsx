import React, { useState } from 'react';
import { Table, Button, Space, Modal, Form, Input, message, Popconfirm, Select, Checkbox, theme } from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
} from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  instrumentService,
  ProjectInstrument,
  CreateInstrumentRequest,
  UpdateInstrumentRequest,
} from '@/services/instrumentService';

const { Option } = Select;

// 设备ID到检测类型代码的映射（与后端保持一致）
const getExperimentTypeCodeByInstrumentId = (instrumentId: number | null | undefined): string | null => {
  if (!instrumentId) return null;
  
  const mapping: Record<number, string> = {
    // 氧化皮堆积检测 (SOD)
    1: 'SOD', 147: 'SOD',
    // 金相检测 (MET)
    2: 'MET', 3: 'MET', 47: 'MET', 48: 'MET', 49: 'MET', 50: 'MET', 51: 'MET', 56: 'MET', 142: 'MET', 145: 'MET',
    // 里氏硬度检测 (LHT)
    5: 'LHT', 6: 'LHT', 7: 'LHT', 8: 'LHT', 9: 'LHT', 41: 'LHT', 42: 'LHT', 43: 'LHT', 44: 'LHT', 45: 'LHT', 46: 'LHT',
    124: 'LHT', 125: 'LHT', 126: 'LHT', 127: 'LHT', 128: 'LHT',
    // 合金分析检测 (PMI)
    10: 'PMI', 11: 'PMI', 73: 'PMI', 74: 'PMI', 129: 'PMI',
    // 磁粉检测 (MT)
    21: 'MT', 22: 'MT', 23: 'MT', 24: 'MT', 25: 'MT', 26: 'MT', 60: 'MT', 61: 'MT',
    67: 'MT', 68: 'MT', 69: 'MT', 70: 'MT', 71: 'MT', 88: 'MT', 89: 'MT', 90: 'MT',
    110: 'MT', 111: 'MT', 112: 'MT', 113: 'MT', 114: 'MT', 115: 'MT',
    152: 'MT', 153: 'MT', 154: 'MT', 155: 'MT', 156: 'MT',
    // 维氏硬度检测 (VHT)
    27: 'VHT',
    // 布氏硬度检测 (BHT)
    30: 'BHT', 31: 'BHT', 32: 'BHT', 33: 'BHT', 34: 'BHT', 35: 'BHT', 91: 'BHT', 92: 'BHT',
    // 洛氏硬度检测 (RHT) — 41 已在 LHT 中定义
    40: 'RHT',
    // 涡流检测 (ET)
    57: 'ET',
    // 内窥镜检测 (VT)
    58: 'VT', 59: 'VT', 106: 'VT', 107: 'VT', 148: 'VT',
    // 超声检测 (UT)
    62: 'UT', 63: 'UT', 64: 'UT', 65: 'UT', 66: 'UT', 104: 'UT', 105: 'UT',
    133: 'UT', 134: 'UT', 135: 'UT', 150: 'UT',
    // 超声波测厚 (UTM)
    72: 'UTM', 98: 'UTM', 99: 'UTM', 100: 'UTM', 101: 'UTM', 102: 'UTM', 103: 'UTM',
    116: 'UTM', 117: 'UTM', 118: 'UTM', 119: 'UTM', 120: 'UTM', 121: 'UTM', 122: 'UTM', 123: 'UTM',
    136: 'UTM', 137: 'UTM', 138: 'UTM', 139: 'UTM', 140: 'UTM', 141: 'UTM',
    // 射线检测 (RT)
    76: 'RT', 77: 'RT', 78: 'RT', 79: 'RT', 80: 'RT', 81: 'RT', 82: 'RT', 83: 'RT',
    // 相控阵超声波检测 (PAUT)
    146: 'PAUT', 165: 'PAUT',
  };
  
  return mapping[instrumentId] || null;
};

// 检测类型选项（与代码对应，用于手工选择）
const EXPERIMENT_TYPE_OPTIONS: { value: string; label: string }[] = [
  { value: 'UT', label: '超声检测 (UT)' },
  { value: 'UTM', label: '超声波测厚 (UTM)' },
  { value: 'SOD', label: '氧化皮堆积检测 (SOD)' },
  { value: 'PMI', label: '合金分析检测 (PMI)' },
  { value: 'MT', label: '磁粉检测 (MT)' },
  { value: 'VHT', label: '维氏硬度检测 (VHT)' },
  { value: 'BHT', label: '布氏硬度检测 (BHT)' },
  { value: 'RHT', label: '洛氏硬度检测 (RHT)' },
  { value: 'ET', label: '涡流检测 (ET)' },
  { value: 'VT', label: '内窥镜检测 (VT)' },
  { value: 'RT', label: '射线检测 (RT)' },
  { value: 'PDM', label: '管径测量 (PDM)' },
  { value: 'PAUT', label: '相控阵超声波检测 (PAUT)' },
  { value: 'LHT', label: '里氏硬度检测 (LHT)' },
  { value: 'MET', label: '金相检测 (MET)' },
];

interface ProjectInstrumentsTableProps {
  projectId: number;
  onInstrumentChange?: () => void;
}

const ProjectInstrumentsTable: React.FC<ProjectInstrumentsTableProps> = ({ projectId, onInstrumentChange }) => {
  const { token } = theme.useToken();
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [editingInstrument, setEditingInstrument] = useState<ProjectInstrument | null>(null);
  const [form] = Form.useForm();
  const queryClient = useQueryClient();

  const { data: instruments = [], isLoading } = useQuery({
    queryKey: ['projectInstruments', projectId],
    queryFn: () => instrumentService.getProjectInstruments(projectId),
    enabled: !!projectId,
  });

  // 查询全局仪器库
  const { data: globalInstruments = [], isLoading: isLoadingGlobal } = useQuery({
    queryKey: ['instruments'],
    queryFn: () => instrumentService.getAllInstruments(),
  });

  const createMutation = useMutation({
    mutationFn: (data: CreateInstrumentRequest) => instrumentService.createProjectInstrument(projectId, data),
    onSuccess: () => {
      message.success('仪器创建成功');
      setIsModalVisible(false);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['projectInstruments', projectId] });
      onInstrumentChange?.();
    },
    onError: () => {
      message.error('创建失败');
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateInstrumentRequest }) =>
      instrumentService.updateProjectInstrument(id, data),
    onSuccess: () => {
      message.success('仪器更新成功');
      setIsModalVisible(false);
      form.resetFields();
      setEditingInstrument(null);
      queryClient.invalidateQueries({ queryKey: ['projectInstruments', projectId] });
      onInstrumentChange?.();
    },
    onError: () => {
      message.error('更新失败');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: instrumentService.deleteProjectInstrument,
    onSuccess: () => {
      message.success('仪器删除成功');
      queryClient.invalidateQueries({ queryKey: ['projectInstruments', projectId] });
      onInstrumentChange?.();
    },
    onError: () => {
      message.error('删除失败');
    },
  });

  const handleAdd = () => {
    setEditingInstrument(null);
    form.resetFields();
    setIsModalVisible(true);
  };

  const handleEdit = (instrument: ProjectInstrument) => {
    setEditingInstrument(instrument);
    // 优先使用globalInstrumentId，如果没有则根据编号匹配全局库中的仪器
    const globalInstrumentId = instrument.globalInstrumentId;
    const matchedGlobalInstrument = globalInstrumentId
      ? globalInstruments.find((gi) => gi.id === globalInstrumentId)
      : globalInstruments.find((gi) => gi.instrumentNumber === instrument.instrumentNumber);
    
    form.setFieldsValue({
      selectedInstrumentId: matchedGlobalInstrument?.id || globalInstrumentId || undefined,
      instrumentName: instrument.instrumentName,
      instrumentModel: instrument.instrumentModel,
      instrumentNumber: instrument.instrumentNumber,
      globalInstrumentId: instrument.globalInstrumentId,
      isDefault: instrument.isDefault || false,
      experimentTypeCode: instrument.experimentTypeCode,
    });
    setIsModalVisible(true);
  };

  const handleDelete = (id: number) => {
    deleteMutation.mutate(id);
  };

  const handleSubmit = () => {
    form.validateFields().then((values) => {
      // 确保隐藏字段有值（如果用户选择了仪器，这些字段应该已经被填充）
      if (!values.instrumentName) {
        message.error('请选择仪器设备');
        return;
      }
      
      // 从表单值中提取所有字段
      const submitData: CreateInstrumentRequest = {
        instrumentName: values.instrumentName || '',
        instrumentModel: values.instrumentModel || '',
        instrumentNumber: values.instrumentNumber || '',
        globalInstrumentId: values.globalInstrumentId,
        isDefault: values.isDefault || false,
        experimentTypeCode: values.experimentTypeCode,
      };
      
      if (editingInstrument) {
        updateMutation.mutate({ id: editingInstrument.id, data: submitData });
      } else {
        createMutation.mutate(submitData);
      }
    }).catch((error) => {
      console.error('表单验证失败:', error);
      message.error('请完成必填项');
    });
  };

  // 处理仪器选择变化
  const handleInstrumentSelect = (instrumentId: number | null) => {
    if (instrumentId === null || instrumentId === undefined) {
      // 清空选择时，清空隐藏字段
      form.setFieldsValue({
        selectedInstrumentId: undefined,
        instrumentName: '',
        instrumentModel: '',
        instrumentNumber: '',
        globalInstrumentId: undefined,
        experimentTypeCode: undefined,
      });
      return;
    }
    
    const selectedInstrument = globalInstruments.find((inst) => inst.id === instrumentId);
    if (selectedInstrument) {
      // 根据设备ID自动确定检测类型代码
      const experimentTypeCode = getExperimentTypeCodeByInstrumentId(instrumentId);
      
      // 使用 setFieldsValue 确保所有字段都被设置
      form.setFieldsValue({
        selectedInstrumentId: instrumentId,
        instrumentName: selectedInstrument.instrumentName || '',
        instrumentModel: selectedInstrument.instrumentModel || '',
        instrumentNumber: selectedInstrument.instrumentNumber || '',
        globalInstrumentId: instrumentId,
        experimentTypeCode: experimentTypeCode || undefined,
      });
      // 验证隐藏字段
      form.validateFields(['instrumentName']).catch(() => {
        // 忽略验证错误，因为这是隐藏字段
      });
    } else {
      message.error('未找到选中的仪器设备');
      form.setFieldsValue({
        selectedInstrumentId: undefined,
        instrumentName: '',
        instrumentModel: '',
        instrumentNumber: '',
        globalInstrumentId: undefined,
        experimentTypeCode: undefined,
      });
    }
  };

  // 生成下拉选项
  const getInstrumentOptions = () => {
    return globalInstruments.map((inst) => {
      const parts = [
        inst.instrumentName,
        inst.instrumentModel,
        inst.instrumentNumber,
      ].filter(Boolean);
      const displayLabel = parts.join(' - ');
      // 搜索关键词：名称、型号、编号
      const searchText = [
        inst.instrumentName,
        inst.instrumentModel,
        inst.instrumentNumber,
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();
      return {
        value: inst.id,
        label: displayLabel,
        searchText: searchText,
        instrument: inst,
      };
    });
  };

  const columns = [
    {
      title: '序号',
      key: 'serialNumber',
      width: 60,
      render: (_: any, __: any, index: number) => index + 1,
    },
    {
      title: '仪器名称',
      dataIndex: 'instrumentName',
      key: 'instrumentName',
    },
    {
      title: '型号',
      dataIndex: 'instrumentModel',
      key: 'instrumentModel',
    },
    {
      title: '编号',
      dataIndex: 'instrumentNumber',
      key: 'instrumentNumber',
    },
    {
      title: '默认设备',
      dataIndex: 'isDefault',
      key: 'isDefault',
      width: 100,
      render: (value: boolean) => (value ? '是' : '否'),
    },
    {
      title: '检测类型',
      dataIndex: 'experimentTypeCode',
      key: 'experimentTypeCode',
      width: 120,
      render: (value: string) => value || '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_: any, record: ProjectInstrument) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定要删除这个仪器吗？"
            onConfirm={() => handleDelete(record.id)}
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
    <>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 8,
          width: '100%',
          paddingRight: 8,
          marginBottom: 12,
        }}
      >
        <h3
          style={{
            margin: 0,
            color: token.colorTextHeading,
            fontSize: token.fontSizeLG,
            fontWeight: token.fontWeightStrong,
          }}
        >
          仪器设备列表
          <span style={{ marginLeft: 8, fontSize: token.fontSizeLG, fontWeight: token.fontWeightStrong }}>
            （共 {instruments.length} 条）
          </span>
        </h3>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          添加仪器
        </Button>
      </div>
      <Table
        columns={columns}
        dataSource={instruments}
        rowKey="id"
        loading={isLoading}
        pagination={false}
      />
      <Modal
        title={editingInstrument ? '编辑仪器' : '添加仪器'}
        open={isModalVisible}
        onOk={handleSubmit}
        onCancel={() => {
          setIsModalVisible(false);
          form.resetFields();
          setEditingInstrument(null);
        }}
        confirmLoading={createMutation.isPending || updateMutation.isPending}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="selectedInstrumentId"
            label="选择仪器"
          >
            <Select
              placeholder="请选择仪器设备"
              showSearch
              loading={isLoadingGlobal}
              filterOption={(input, option) => {
                const optionData = getInstrumentOptions().find((opt) => opt.value === option?.value);
                if (optionData) {
                  return optionData.searchText.includes(input.toLowerCase());
                }
                return false;
              }}
              onChange={handleInstrumentSelect}
              allowClear
            >
              {getInstrumentOptions().map((opt) => (
                <Option key={opt.value} value={opt.value}>
                  {opt.label}
                </Option>
              ))}
            </Select>
            {globalInstruments.length === 0 && !isLoadingGlobal && (
              <div style={{ color: '#999', fontSize: '12px', marginTop: '4px' }}>
                全局仪器库为空，请联系管理员添加仪器设备
              </div>
            )}
          </Form.Item>
          {/* 仪器基础信息：既可从库中自动填充，也可手工录入 */}
          <Form.Item 
            name="instrumentName" 
            label="仪器名称"
            rules={[{ required: true, message: '请输入仪器名称或从仪器库选择' }]}
          >
            <Input placeholder="请输入仪器名称" />
          </Form.Item>
          <Form.Item name="instrumentModel" label="型号">
            <Input placeholder="请输入型号（可选）" />
          </Form.Item>
          <Form.Item name="instrumentNumber" label="编号">
            <Input placeholder="请输入编号（可选）" />
          </Form.Item>
          <Form.Item name="globalInstrumentId" hidden>
            <Input />
          </Form.Item>
          <Form.Item
            name="experimentTypeCode"
            label="检测类型"
            rules={[{ required: true, message: '请选择检测类型' }]}
          >
            <Select placeholder="请选择检测类型">
              {EXPERIMENT_TYPE_OPTIONS.map((opt) => (
                <Option key={opt.value} value={opt.value}>
                  {opt.label}
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="isDefault" valuePropName="checked">
            <Checkbox>设为默认设备</Checkbox>
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default ProjectInstrumentsTable;
