import React, { useState, useEffect } from 'react';
import {
  Form,
  Input,
  DatePicker,
  Button,
  Card,
  Space,
  Select,
  message,
  Typography,
  Divider,
  Collapse,
} from 'antd';
import { PlusOutlined, SaveOutlined, MinusCircleOutlined } from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { reportService } from '@/services/reportService';
import { experimentTypeService } from '@/services/experimentTypeService';
import DynamicTable from '@/components/DynamicTable/DynamicTable';
import type { CreateReport } from '@/types';
import dayjs from 'dayjs';

const { Title } = Typography;
const { TextArea } = Input;
const { Panel } = Collapse;

const ReportFormPage: React.FC = () => {
  const navigate = useNavigate();
  const { id } = useParams();
  const queryClient = useQueryClient();
  const [form] = Form.useForm();
  const [reportItems, setReportItems] = useState<any[]>([]);

  const isEditMode = !!id;

  // Load experiment types
  const { data: experimentTypes = [] } = useQuery({
    queryKey: ['experimentTypes'],
    queryFn: experimentTypeService.getAll,
  });

  // Load projects
  const { data: projects = [] } = useQuery({
    queryKey: ['projects'],
    queryFn: async () => {
      const { projectService } = await import('@/services/projectService');
      return projectService.getAll();
    },
  });

  // Load report data if editing
  const { data: reportData } = useQuery({
    queryKey: ['report', id],
    queryFn: () => reportService.getById(Number(id)),
    enabled: isEditMode,
  });

  useEffect(() => {
    if (reportData) {
      form.setFieldsValue({
        projectId: reportData.projectId,
        title: reportData.title,
        inspector: reportData.inspector,
        testMethod: reportData.testMethod,
        equipment: reportData.equipment,
        testStandard: reportData.testStandard,
        componentName: reportData.componentName,
        testDate: dayjs(reportData.testDate),
        location: reportData.location,
        status: reportData.status,
      });

      // Set report items
      const items = reportData.reportItems.map((item) => {
        const expType = experimentTypes.find((t) => t.id === item.experimentTypeId);
        return {
          experimentTypeId: item.experimentTypeId,
          tableData: JSON.parse(item.tableData),
          summary: item.summary,
          schema: expType ? JSON.parse(expType.tableSchema) : null,
        };
      });
      setReportItems(items);
    }
  }, [reportData, experimentTypes, form]);

  const createMutation = useMutation({
    mutationFn: reportService.create,
    onSuccess: () => {
      message.success('报告创建成功！');
      queryClient.invalidateQueries({ queryKey: ['reports'] });
      navigate('/reports');
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: any }) => reportService.update(id, data),
    onSuccess: () => {
      message.success('报告更新成功！');
      queryClient.invalidateQueries({ queryKey: ['reports'] });
      queryClient.invalidateQueries({ queryKey: ['report', id] });
      navigate('/reports');
    },
  });

  const handleSubmit = async (values: any) => {
    try {
      const data: CreateReport = {
        projectId: values.projectId,
        title: values.title,
        inspector: values.inspector,
        testMethod: values.testMethod,
        equipment: values.equipment,
        testStandard: values.testStandard,
        componentName: values.componentName,
        testDate: values.testDate.format('YYYY-MM-DD'),
        location: values.location,
        reportItems: reportItems.map((item) => ({
          experimentTypeId: item.experimentTypeId,
          tableData: JSON.stringify({ rows: item.tableData }),
          summary: item.summary || '',
        })),
      };

      if (isEditMode) {
        updateMutation.mutate({ id: Number(id), data: { ...data, status: values.status || 'Draft' } });
      } else {
        createMutation.mutate(data);
      }
    } catch (error) {
      message.error('保存失败，请检查数据');
    }
  };

  const handleAddReportItem = (experimentTypeId: number) => {
    const expType = experimentTypes.find((t) => t.id === experimentTypeId);
    if (expType) {
      const schema = JSON.parse(expType.tableSchema);
      setReportItems([
        ...reportItems,
        {
          experimentTypeId,
          tableData: [],
          summary: '',
          schema,
        },
      ]);
    }
  };

  const handleRemoveReportItem = (index: number) => {
    setReportItems(reportItems.filter((_, i) => i !== index));
  };

  const handleTableDataChange = (index: number, data: any) => {
    const newItems = [...reportItems];
    newItems[index].tableData = data;
    setReportItems(newItems);
  };

  const handleSummaryChange = (index: number, summary: string) => {
    const newItems = [...reportItems];
    newItems[index].summary = summary;
    setReportItems(newItems);
  };

  return (
    <div>
      <Title level={2}>{isEditMode ? '编辑报告' : '创建新报告'}</Title>

      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        initialValues={{
          testDate: dayjs(),
          status: 'Draft',
        }}
      >
        <Card title="基本信息" style={{ marginBottom: 16 }}>
          <Form.Item
            label="所属项目"
            name="projectId"
            rules={[{ required: true, message: '请选择所属项目' }]}
          >
            <Select
              placeholder="请选择项目"
              size="large"
              showSearch
              optionFilterProp="children"
              filterOption={(input, option) =>
                (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
              }
              options={projects.map((p) => ({
                label: `${p.projectNumber} - ${p.projectName}`,
                value: p.id,
              }))}
            />
          </Form.Item>

          <Form.Item
            label="单项报告标题"
            name="title"
            rules={[{ required: true, message: '请输入报告标题' }]}
            tooltip="如：高压转子焊缝磁粉检测"
          >
            <Input placeholder="请输入单项报告标题" size="large" />
          </Form.Item>

          <Form.Item
            label="部件名称"
            name="componentName"
            tooltip="被检测的部件名称"
          >
            <Input placeholder="如：高压转子焊缝" />
          </Form.Item>

          <Form.Item
            label="检测人员"
            name="inspector"
          >
            <Input placeholder="请输入检测人员姓名" />
          </Form.Item>

          <Form.Item
            label="检测方法"
            name="testMethod"
          >
            <Input placeholder="如：连续法、剩磁法" />
          </Form.Item>

          <Form.Item
            label="使用仪器/设备"
            name="equipment"
          >
            <Input placeholder="请输入仪器型号或名称" />
          </Form.Item>

          <Form.Item
            label="检测标准"
            name="testStandard"
          >
            <Input placeholder="如：GB/T xxx-2020" />
          </Form.Item>

          <Form.Item
            label="检测日期"
            name="testDate"
            rules={[{ required: true, message: '请选择检测日期' }]}
          >
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            label="检测地点"
            name="location"
            rules={[{ required: true, message: '请输入检测地点' }]}
          >
            <Input placeholder="请输入检测地点" />
          </Form.Item>

          {isEditMode && (
            <Form.Item label="状态" name="status">
              <Select
                options={[
                  { label: '草稿', value: 'Draft' },
                  { label: '已完成', value: 'Completed' },
                ]}
              />
            </Form.Item>
          )}
        </Card>

        <Card
          title="检测数据"
          extra={
            <Select
              placeholder="选择实验类型添加检测项"
              style={{ width: 200 }}
              onChange={handleAddReportItem}
              value={undefined}
            >
              {experimentTypes.map((type) => (
                <Select.Option key={type.id} value={type.id}>
                  {type.name}
                </Select.Option>
              ))}
            </Select>
          }
          style={{ marginBottom: 16 }}
        >
          {reportItems.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '40px 0', color: '#999' }}>
              <PlusOutlined style={{ fontSize: 48, marginBottom: 16 }} />
              <div>暂无检测数据，请点击上方下拉框添加检测项</div>
            </div>
          ) : (
            <Collapse accordion>
              {reportItems.map((item, index) => {
                const expType = experimentTypes.find((t) => t.id === item.experimentTypeId);
                return (
                  <Panel
                    key={index}
                    header={`${expType?.name} (${expType?.code})`}
                    extra={
                      <Button
                        type="text"
                        danger
                        size="small"
                        icon={<MinusCircleOutlined />}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleRemoveReportItem(index);
                        }}
                      >
                        删除此检测项
                      </Button>
                    }
                  >
                    <DynamicTable
                      schema={item.schema}
                      value={item.tableData}
                      onChange={(data) => handleTableDataChange(index, data)}
                    />

                    <Divider />

                    <Form.Item label="检测结论">
                      <TextArea
                        rows={3}
                        placeholder="请输入检测结论"
                        value={item.summary}
                        onChange={(e) => handleSummaryChange(index, e.target.value)}
                      />
                    </Form.Item>
                  </Panel>
                );
              })}
            </Collapse>
          )}
        </Card>

        <Space size="large">
          <Button
            type="primary"
            htmlType="submit"
            size="large"
            icon={<SaveOutlined />}
            loading={createMutation.isPending || updateMutation.isPending}
          >
            {isEditMode ? '保存修改' : '创建报告'}
          </Button>
          <Button size="large" onClick={() => navigate('/reports')}>
            取消
          </Button>
        </Space>
      </Form>
    </div>
  );
};

export default ReportFormPage;


