import React from 'react';
import { Form, Input, InputNumber, DatePicker, Select, Row, Col } from 'antd';
import { ReportField, ReportFieldsSchema } from '../../types';

interface DynamicReportFieldsProps {
  reportFieldsSchema: string; // JSON string from ExperimentType
  value?: Record<string, any>;
  onChange?: (value: Record<string, any>) => void;
  disabled?: boolean;
}

const DynamicReportFields: React.FC<DynamicReportFieldsProps> = ({
  reportFieldsSchema,
  value,
  onChange,
  disabled = false
}) => {
  let schema: ReportFieldsSchema;
  
  try {
    schema = JSON.parse(reportFieldsSchema);
  } catch (error) {
    console.error('Invalid ReportFieldsSchema JSON:', error);
    return <div>报告字段配置错误</div>;
  }

  const handleFieldChange = (fieldName: string, fieldValue: any) => {
    if (onChange) {
      onChange({
        ...value,
        [fieldName]: fieldValue
      });
    }
  };

  const renderField = (field: ReportField) => {
    const fieldValue = value?.[field.name];

    switch (field.type) {
      case 'text':
        return (
          <Input
            placeholder={`请输入${field.label}`}
            value={fieldValue}
            onChange={(e) => handleFieldChange(field.name, e.target.value)}
            disabled={disabled}
          />
        );
      
      case 'number': {
        const num =
          typeof fieldValue === 'number'
            ? fieldValue
            : fieldValue != null && fieldValue !== ''
              ? Number(String(fieldValue))
              : undefined;
        const displayNum = typeof num === 'number' && Number.isFinite(num) ? num : undefined;
        return (
          <InputNumber
            placeholder={`请输入${field.label}`}
            value={displayNum}
            onChange={(val) =>
              handleFieldChange(field.name, val == null ? '' : String(val))
            }
            disabled={disabled}
            style={{ width: '100%' }}
          />
        );
      }
      
      case 'date':
        return (
          <DatePicker
            placeholder={`请选择${field.label}`}
            value={fieldValue}
            onChange={(date) => handleFieldChange(field.name, date)}
            disabled={disabled}
            style={{ width: '100%' }}
          />
        );
      
      case 'select':
        return (
          <Select
            placeholder={`请选择${field.label}`}
            value={fieldValue}
            onChange={(val) => handleFieldChange(field.name, val)}
            disabled={disabled}
            options={field.options?.map(option => ({
              label: option,
              value: option
            }))}
          />
        );
      
      default:
        return (
          <Input
            placeholder={`请输入${field.label}`}
            value={fieldValue}
            onChange={(e) => handleFieldChange(field.name, e.target.value)}
            disabled={disabled}
          />
        );
    }
  };

  return (
    <Row gutter={[16, 16]}>
      {schema.fields.map((field) => (
        <Col span={12} key={field.name}>
          <Form.Item
            label={field.label}
            required={field.required}
            style={{ marginBottom: 16 }}
          >
            {renderField(field)}
          </Form.Item>
        </Col>
      ))}
    </Row>
  );
};

export default DynamicReportFields;


