import React from 'react';
import { Form, Input, Select, InputNumber, Button, Card } from 'antd';
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons';
import { TableSchema, TableRowData } from '@/types';
import ImageUpload from '../ImageUpload/ImageUpload';

interface DynamicTableProps {
  schema: TableSchema;
  value?: TableRowData[];
  onChange?: (value: TableRowData[]) => void;
}

const DynamicTable: React.FC<DynamicTableProps> = ({ schema, value = [], onChange }) => {
  const handleChange = (index: number, field: string, fieldValue: any) => {
    const newValue = [...value];
    if (!newValue[index]) {
      newValue[index] = {};
    }
    newValue[index][field] = fieldValue;
    onChange?.(newValue);
  };

  const handleAdd = () => {
    const newRow: TableRowData = {};
    schema.columns.forEach((col) => {
      const fieldKey = col.key ?? col.name ?? '';
      if (fieldKey) {
        newRow[fieldKey] = '';
      }
    });
    newRow.imageIds = [];
    onChange?.([...value, newRow]);
  };

  const handleRemove = (index: number) => {
    const newValue = value.filter((_, i) => i !== index);
    onChange?.(newValue);
  };

  const renderField = (column: any, rowIndex: number) => {
    const fieldKey = column.key ?? column.name;
    const label = column.label ?? column.name ?? column.key ?? '';
    const fieldValue = fieldKey ? value[rowIndex]?.[fieldKey] : undefined;

    switch (column.type) {
      case 'number': {
        const numericValue =
          typeof fieldValue === 'number'
            ? fieldValue
            : fieldValue != null && fieldValue !== ''
              ? Number(String(fieldValue))
              : undefined;
        const displayNum = typeof numericValue === 'number' && Number.isFinite(numericValue) ? numericValue : undefined;
        return (
          <InputNumber
            style={{ width: '100%' }}
            placeholder={`请输入${label}`}
            value={displayNum}
            onChange={(val) =>
              fieldKey && handleChange(rowIndex, fieldKey, val == null ? '' : String(val))
            }
          />
        );
      }
      case 'select':
        const selectValue = typeof fieldValue === 'string' ? fieldValue : undefined;
        return (
          <Select
            style={{ width: '100%' }}
            placeholder={`请选择${label}`}
            value={selectValue}
            onChange={(val) => fieldKey && handleChange(rowIndex, fieldKey, val)}
            options={column.options?.map((opt: string) => ({ label: opt, value: opt }))}
          />
        );
      default:
        const textValue = typeof fieldValue === 'string' ? fieldValue : fieldValue == null ? '' : String(fieldValue);
        return (
          <Input
            placeholder={`请输入${label}`}
            value={textValue}
            onChange={(e) => fieldKey && handleChange(rowIndex, fieldKey, e.target.value)}
          />
        );
    }
  };

  return (
    <div>
      {value.map((row, index) => (
        <Card
          key={index}
          size="small"
          title={`记录 ${index + 1}`}
          extra={
            <Button
              type="text"
              danger
              icon={<MinusCircleOutlined />}
              onClick={() => handleRemove(index)}
            >
              删除
            </Button>
          }
          style={{ marginBottom: 16 }}
        >
          <Form layout="vertical">
            {schema.columns.map((column) => {
              const fieldKey = column.key ?? column.name ?? '';
              const label = column.label ?? column.name ?? column.key;
              return (
              <Form.Item
                key={fieldKey}
                label={label}
                required={column.required}
              >
                {renderField(column, index)}
              </Form.Item>
            );
            })}

            <Form.Item label="相关图片">
              <ImageUpload
                value={row.imageIds || []}
                onChange={(imageIds) => handleChange(index, 'imageIds', imageIds)}
                maxCount={5}
              />
            </Form.Item>
          </Form>
        </Card>
      ))}

      <Button
        type="dashed"
        onClick={handleAdd}
        block
        icon={<PlusOutlined />}
      >
        添加记录
      </Button>
    </div>
  );
};

export default DynamicTable;


