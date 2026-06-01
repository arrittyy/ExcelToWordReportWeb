import React, { useState, useEffect } from 'react';
import { Table, Button, Input, Select, Space, Form, Popconfirm } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined, DeleteOutlined, SaveOutlined } from '@ant-design/icons';
import { DetectionDataRow, DetectionData } from '../../types';

interface DetectionDataTableProps {
  value?: DetectionData;
  onChange?: (value: DetectionData) => void;
  disabled?: boolean;
}

const DetectionDataTable: React.FC<DetectionDataTableProps> = ({ 
  value, 
  onChange, 
  disabled = false 
}) => {
  const [dataSource, setDataSource] = useState<DetectionDataRow[]>([]);
  const [editingKey, setEditingKey] = useState<number | null>(null);
  const [form] = Form.useForm();

  type EditableDetectionRow = DetectionDataRow & { key: string };

  useEffect(() => {
    if (value?.rows) {
      setDataSource(value.rows);
    } else {
      setDataSource([]);
    }
  }, [value]);

  const isEditing = (index: number | null) => index === editingKey;

  const edit = (record: DetectionDataRow, index: number) => {
    form.setFieldsValue({
      序号: '',
      起始位置: '',
      终点位置: '',
      长度: '',
      级别: '',
      备注: '',
      ...record,
    });
    setEditingKey(index);
  };

  const cancel = () => {
    setEditingKey(null);
    form.resetFields();
  };

  const save = async (key: number) => {
    try {
      const row = await form.validateFields();
      const newData = [...dataSource];
      const index = newData.findIndex((_, idx) => idx === key);
      
      if (index > -1) {
        const item = newData[index];
        newData.splice(index, 1, { ...item, ...row });
        setDataSource(newData);
        setEditingKey(null);
        
        // 通知父组件数据变化
        if (onChange) {
          onChange({ rows: newData });
        }
      }
    } catch (errInfo) {
      console.log('Validate Failed:', errInfo);
    }
  };

  const handleAdd = () => {
    const newRow: DetectionDataRow = {
      序号: `${dataSource.length + 1}`,
      起始位置: '',
      终点位置: '',
      长度: '',
      级别: '',
      备注: '',
    };
    const newData = [...dataSource, newRow];
    setDataSource(newData);
    
    if (onChange) {
      onChange({ rows: newData });
    }
  };

  const handleDelete = (key: number) => {
    const newData = dataSource.filter((_, index) => index !== key);
    // 重新编号
    const renumberedData = newData.map((item, index) => ({
      ...item,
      序号: `${index + 1}`,
    }));
    
    setDataSource(renumberedData);
    
    if (onChange) {
      onChange({ rows: renumberedData });
    }
  };

  const EditableCell: React.FC<{
    editing: boolean;
    dataIndex: string;
    title: string;
    inputType: 'text' | 'select';
    record: EditableDetectionRow;
    index?: number;
    children: React.ReactNode;
  }> = ({ editing, dataIndex, title, inputType, record, index, children, ...restProps }) => {
    const inputNode = inputType === 'select' && dataIndex === '级别' ? (
      <Select placeholder="请选择级别" size="small">
        <Select.Option value="I">I级</Select.Option>
        <Select.Option value="II">II级</Select.Option>
        <Select.Option value="III">III级</Select.Option>
        <Select.Option value="IV">IV级</Select.Option>
      </Select>
    ) : (
      <Input size="small" />
    );

    return (
      <td {...restProps}>
        {editing ? (
          <Form.Item
            name={dataIndex}
            style={{ margin: 0 }}
            rules={[
              {
                required: true,
                message: `请输入${title}!`,
              },
            ]}
          >
            {inputNode}
          </Form.Item>
        ) : (
          children
        )}
      </td>
    );
  };

  const columns: ColumnsType<EditableDetectionRow> = [
    {
      title: '序号',
      dataIndex: '序号',
      width: 80,
      onCell: (_record, index) => ({
        editing: isEditing(index ?? null),
        dataIndex: '序号',
        title: '序号',
        inputType: 'text' as const,
        index,
      }),
    },
    {
      title: '起始位置',
      dataIndex: '起始位置',
      width: 120,
      onCell: (_record, index) => ({
        editing: isEditing(index ?? null),
        dataIndex: '起始位置',
        title: '起始位置',
        inputType: 'text' as const,
        index,
      }),
    },
    {
      title: '终点位置',
      dataIndex: '终点位置',
      width: 120,
      onCell: (_record, index) => ({
        editing: isEditing(index ?? null),
        dataIndex: '终点位置',
        title: '终点位置',
        inputType: 'text' as const,
        index,
      }),
    },
    {
      title: '长度',
      dataIndex: '长度',
      width: 100,
      onCell: (_record, index) => ({
        editing: isEditing(index ?? null),
        dataIndex: '长度',
        title: '长度',
        inputType: 'text' as const,
        index,
      }),
    },
    {
      title: '级别',
      dataIndex: '级别',
      width: 100,
      onCell: (_record, index) => ({
        editing: isEditing(index ?? null),
        dataIndex: '级别',
        title: '级别',
        inputType: 'select' as const,
        index,
      }),
    },
    {
      title: '备注',
      dataIndex: '备注',
      onCell: (_record, index) => ({
        editing: isEditing(index ?? null),
        dataIndex: '备注',
        title: '备注',
        inputType: 'text' as const,
        index,
      }),
    },
    {
      title: '操作',
      width: 120,
      render: (_: any, _record: DetectionDataRow, index: number) => {
        const editable = isEditing(index);
        return editable ? (
          <Space size="small">
            <Button
              type="link"
              size="small"
              icon={<SaveOutlined />}
              onClick={() => save(index)}
            >
              保存
            </Button>
            <Button type="link" size="small" onClick={cancel}>
              取消
            </Button>
          </Space>
        ) : (
          <Space size="small">
            <Button
              type="link"
              size="small"
              disabled={editingKey !== null || disabled}
              onClick={() => edit(dataSource[index], index)}
            >
              编辑
            </Button>
            <Popconfirm
              title="确定删除这条记录吗？"
              onConfirm={() => handleDelete(index)}
              disabled={disabled}
            >
              <Button
                type="link"
                size="small"
                danger
                icon={<DeleteOutlined />}
                disabled={disabled}
              >
                删除
              </Button>
            </Popconfirm>
          </Space>
        );
      },
    },
  ];

  const tableData: EditableDetectionRow[] = dataSource.map((item, index) => ({
    ...item,
    key: index.toString(),
  }));

  return (
    <Form form={form} component={false}>
      <div style={{ marginBottom: 16 }}>
        <Button
          type="dashed"
          icon={<PlusOutlined />}
          onClick={handleAdd}
          disabled={disabled}
          block
        >
          添加检测数据行
        </Button>
      </div>
        <Table
        components={{
          body: {
            cell: EditableCell,
          },
        }}
        bordered
          dataSource={tableData}
        columns={columns}
        rowClassName="editable-row"
        pagination={false}
        size="small"
        scroll={{ x: 800 }}
      />
    </Form>
  );
};

export default DetectionDataTable;


