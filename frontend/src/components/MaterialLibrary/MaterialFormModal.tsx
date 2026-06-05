import React, { useEffect } from 'react';
import { Modal, Form, Input, message, Alert } from 'antd';
import { useMutation } from '@tanstack/react-query';
import {
  MATERIAL_CATEGORY_FIELDS,
  MATERIAL_META_FIELDS,
  type MaterialCategory,
} from '@/constants/materialLibraryFields';
import {
  materialLibraryService,
  type CreateMaterialRequest,
  type MaterialLibraryEntry,
  type UpdateMaterialRequest,
} from '@/services/materialLibraryService';

interface MaterialFormModalProps {
  open: boolean;
  mode: 'create' | 'edit';
  category: MaterialCategory;
  entry?: MaterialLibraryEntry | null;
  onClose: () => void;
  onSuccess: () => void;
}

const MaterialFormModal: React.FC<MaterialFormModalProps> = ({
  open,
  mode,
  category,
  entry,
  onClose,
  onSuccess,
}) => {
  const [form] = Form.useForm<CreateMaterialRequest & Record<string, string>>();
  const isEdit = mode === 'edit';
  const isRejectedResubmit =
    isEdit && (entry?.status === 'REJECTED' || !!(entry?.reviewComment?.trim()));

  useEffect(() => {
    if (!open) return;
    if (isEdit && entry) {
      const initial: Record<string, string> = {
        materialKey: entry.materialKey,
      };
      for (const field of MATERIAL_META_FIELDS) {
        initial[field.key] = entry.properties?.[field.key] || '';
      }
      const editCategory = (entry.primaryCategory as MaterialCategory) || category;
      for (const field of MATERIAL_CATEGORY_FIELDS[editCategory]) {
        initial[field.key] = entry.properties?.[field.key] || '';
      }
      form.setFieldsValue(initial);
    } else {
      form.resetFields();
    }
  }, [open, category, form, isEdit, entry]);

  const submitMutation = useMutation({
    mutationFn: materialLibraryService.submit,
    onSuccess: () => {
      message.success('材质已提交，等待审核');
      onSuccess();
      onClose();
      form.resetFields();
    },
    onError: (error: unknown) => {
      const err = error as { response?: { data?: { message?: string } } };
      message.error(err?.response?.data?.message || '提交失败，请重试');
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateMaterialRequest }) =>
      materialLibraryService.update(id, data),
    onSuccess: () => {
      message.success(isRejectedResubmit ? '已重新提交，等待审核' : '修改已提交，等待审核');
      onSuccess();
      onClose();
      form.resetFields();
    },
    onError: (error: unknown) => {
      const err = error as { response?: { data?: { message?: string } } };
      message.error(err?.response?.data?.message || '提交失败，请重试');
    },
  });

  const buildProperties = (values: CreateMaterialRequest & Record<string, string>) => {
    const properties: Record<string, string> = {};
    for (const field of MATERIAL_META_FIELDS) {
      const value = values[field.key as keyof typeof values];
      if (typeof value === 'string' && value.trim()) {
        properties[field.key] = value.trim();
      }
    }
    const editCategory = (entry?.primaryCategory as MaterialCategory) || category;
    for (const field of MATERIAL_CATEGORY_FIELDS[editCategory]) {
      const value = values[field.key as keyof typeof values];
      if (typeof value === 'string' && value.trim()) {
        properties[field.key] = value.trim();
      }
    }
    return { properties, editCategory };
  };

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      const { properties, editCategory } = buildProperties(values);
      const categoryFields = MATERIAL_CATEGORY_FIELDS[editCategory];
      const hasCategoryValue = categoryFields.some((f) => properties[f.key]);
      if (!hasCategoryValue) {
        message.warning('请至少填写一个该分类对应的标准字段');
        return;
      }

      if (isEdit && entry?.id) {
        await updateMutation.mutateAsync({
          id: entry.id,
          data: {
            primaryCategory: editCategory,
            properties,
          },
        });
      } else {
        await submitMutation.mutateAsync({
          materialKey: values.materialKey.trim(),
          primaryCategory: category,
          properties,
        });
      }
    } catch {
      // validation errors handled by form
    }
  };

  const loading = submitMutation.isPending || updateMutation.isPending;

  return (
    <Modal
      title={isEdit ? `编辑材质：${entry?.materialKey ?? ''}` : '新增材质'}
      open={open}
      onCancel={onClose}
      onOk={handleOk}
      confirmLoading={loading}
      width={720}
      destroyOnClose
    >
      <Form form={form} layout="vertical">
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message="标准值格式说明"
          description="区间用浪号（如 255～311）；大于等于用 ≥ 或 >=；小于等于用 ≤ 或 <="
        />
        {isRejectedResubmit && entry?.reviewComment && (
          <Alert
            type="warning"
            showIcon
            style={{ marginBottom: 16 }}
            message="驳回原因"
            description={entry.reviewComment}
          />
        )}
        <Form.Item
          name="materialKey"
          label="材质牌号（查询用）"
          rules={[{ required: true, message: '请输入材质牌号' }, { max: 100 }]}
        >
          <Input placeholder="如 P91、35CrMoA" disabled={isEdit} />
        </Form.Item>
        {MATERIAL_META_FIELDS.map((field) => (
          <Form.Item key={field.key} name={field.key} label={field.label}>
            <Input placeholder="选填" />
          </Form.Item>
        ))}
        {MATERIAL_CATEGORY_FIELDS[
          (entry?.primaryCategory as MaterialCategory) || category
        ].map((field) => (
          <Form.Item key={field.key} name={field.key} label={field.label}>
            <Input placeholder="如 255～311、≥585、0.40～0.70" />
          </Form.Item>
        ))}
      </Form>
    </Modal>
  );
};

export default MaterialFormModal;
