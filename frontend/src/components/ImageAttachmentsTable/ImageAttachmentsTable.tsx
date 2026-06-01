import React, { useState } from 'react';
import { Table, Button, Input, Space, Popconfirm, message } from 'antd';
import { PlusOutlined, DeleteOutlined, SaveOutlined, EditOutlined, CheckOutlined } from '@ant-design/icons';
import MultiImageUploadField from '../MultiImageUpload/MultiImageUploadField';
import type { ImageAttachment } from '../../types';

interface ImageAttachmentsTableProps {
  value?: ImageAttachment[];
  onChange?: (value: ImageAttachment[]) => void;
  onSave?: (value: ImageAttachment[]) => void;  // ✅ 保存回调
  disabled?: boolean;
}

const ImageAttachmentsTable: React.FC<ImageAttachmentsTableProps> = ({
  value = [],
  onChange,
  onSave,
  disabled = false
}) => {
  const [dataSource, setDataSource] = useState<ImageAttachment[]>(value);
  const [editingRows, setEditingRows] = useState<Set<number>>(new Set());

  React.useEffect(() => {
    setDataSource(value);
  }, [value]);

  const handleAdd = () => {
    const newAttachment: ImageAttachment = {
      imageUrls: [],
      description: ''
    };
    
    const newData = [...dataSource, newAttachment];
    setDataSource(newData);
    onChange?.(newData);
    
    // 新行自动进入编辑状态
    const newIndex = newData.length - 1;
    setEditingRows(new Set([...editingRows, newIndex]));
  };

  const handleDelete = (index: number) => {
    const newData = dataSource.filter((_, i) => i !== index);
    setDataSource(newData);
    onChange?.(newData);
    
    // 删除后更新编辑状态
    const newEditingRows = new Set<number>();
    editingRows.forEach(rowIndex => {
      if (rowIndex < index) {
        newEditingRows.add(rowIndex);
      } else if (rowIndex > index) {
        newEditingRows.add(rowIndex - 1);
      }
    });
    setEditingRows(newEditingRows);
  };

  const handleUpdate = (index: number, field: keyof ImageAttachment, newValue: any) => {
    const newData = [...dataSource];
    newData[index] = { ...newData[index], [field]: newValue };
    setDataSource(newData);
    onChange?.(newData);
  };

  const handleEdit = (index: number) => {
    setEditingRows(new Set([...editingRows, index]));
  };

  const handleSaveRow = (index: number) => {
    const row = dataSource[index];
    
    // ✅ 验证: 至少要有一张图片
    if (!row.imageUrls || row.imageUrls.length === 0) {
      message.error('请至少上传一张图片后再保存');
      return;
    }
    
    // 退出编辑状态
    const newEditingRows = new Set(editingRows);
    newEditingRows.delete(index);
    setEditingRows(newEditingRows);
    
    // ✅ 触发后端保存
    if (onSave) {
      onSave(dataSource);
    }
    
    message.success('附图行保存成功');
  };

  const isEditing = (index: number) => editingRows.has(index);

  const columns = [
    {
      title: '序号',
      dataIndex: 'index',
      key: 'index',
      width: 60,
      render: (_: any, _record: ImageAttachment, index: number) => index + 1,
    },
    {
      title: '附图',
      dataIndex: 'imageUrls',
      key: 'imageUrls',
      width: 200,
      render: (imageUrls: string[], _record: ImageAttachment, index: number) => (
        <MultiImageUploadField
          value={imageUrls}
          onChange={(urls) => handleUpdate(index, 'imageUrls', urls)}
          maxCount={3}
          disabled={disabled || !isEditing(index)}
        />
      ),
    },
    {
      title: '附图描述',
      dataIndex: 'description',
      key: 'description',
      render: (description: string, _record: ImageAttachment, index: number) => (
        <Input
          value={description}
          onChange={(e) => handleUpdate(index, 'description', e.target.value)}
          placeholder="请输入附图描述"
          disabled={disabled || !isEditing(index)}
          size="small"
        />
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_: any, _record: ImageAttachment, index: number) => (
        <Space size="small">
          {isEditing(index) ? (
            <Button
              type="link"
              size="small"
              icon={<CheckOutlined />}
              onClick={() => handleSaveRow(index)}
              disabled={disabled}
            >
              保存
            </Button>
          ) : (
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={() => handleEdit(index)}
              disabled={disabled}
            >
              编辑
            </Button>
          )}
          <Popconfirm
            title="确定删除这个附图项吗？"
            onConfirm={() => handleDelete(index)}
            disabled={disabled}
            okText="确定"
            cancelText="取消"
          >
            <Button
              type="link"
              danger
              size="small"
              icon={<DeleteOutlined />}
              disabled={disabled}
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const handleSaveAll = () => {
    // ✅ 验证: 检查是否所有附图都至少有一张图片
    const emptyRows = dataSource.filter(row => !row.imageUrls || row.imageUrls.length === 0);
    
    if (emptyRows.length > 0) {
      message.error(`有 ${emptyRows.length} 行附图未上传图片,请先上传图片后再保存`);
      return;
    }
    
    // 检查是否有行正在编辑
    if (editingRows.size > 0) {
      message.warning('请先保存正在编辑的行');
      return;
    }
    
    if (onSave) {
      onSave(dataSource);
    }
  };

  return (
    <div className="report-subcard-section-body">
      {/* 标题栏 - 包含标题和保存按钮 */}
      <div className="report-subcard-header">
        <h4 className="report-subcard-title">附图管理</h4>
        <Button 
          type="primary" 
          size="small"
          icon={<SaveOutlined />}
          onClick={handleSaveAll}
          disabled={disabled}
        >
          保存附图
        </Button>
      </div>
      
      <div className="report-subcard-table">
        <Table
          className="report-subcard-table-grid"
          bordered={false}
          columns={columns}
          dataSource={dataSource.map((item, index) => ({ ...item, key: index }))}
          pagination={false}
          size="small"
          locale={{
            emptyText: (
              <div style={{ padding: '16px 0', fontSize: '14px', color: '#999' }}>
                暂无数据
              </div>
            ),
          }}
          footer={() => (
            <div className="report-subcard-footer">
              <Button
                type="dashed"
                icon={<PlusOutlined />}
                onClick={handleAdd}
                disabled={disabled}
                block
              >
                添加附图行
              </Button>
            </div>
          )}
        />
      </div>
    </div>
  );
};

export default ImageAttachmentsTable;




