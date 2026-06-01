import React, { useEffect, useRef, useState, useCallback } from 'react';
import { Upload, Button, Image, Space, Popconfirm, message } from 'antd';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import type { UploadFile, UploadProps } from 'antd';
import { imageService } from '@/services/imageService';
import { getImageFilesFromClipboardEvent } from '@/utils/clipboardImages';

interface MultiImageUploadFieldProps {
  value?: string[];  // 图片URL数组
  onChange?: (urls: string[]) => void;
  maxCount?: number;  // 最多上传几张
  disabled?: boolean;
}

const MultiImageUploadField: React.FC<MultiImageUploadFieldProps> = ({
  value = [],
  onChange,
  maxCount = 5,
  disabled = false
}) => {
  const [fileList] = useState<UploadFile[]>([]);
  const valueRef = useRef(value);
  useEffect(() => {
    valueRef.current = value;
  }, [value]);

  const handleUpload = async (file: File): Promise<string> => {
    const hostname = window.location.hostname;
    const isDev = import.meta.env.DEV;
    const response = await imageService.upload(file);

    const imageUrl = response.url;
    const baseURL = isDev
      ? (hostname !== 'localhost' && hostname !== '127.0.0.1'
          ? `http://${hostname}:8080`
          : '')
      : '';

    const fullUrl = imageUrl.startsWith('http') ? imageUrl : `${baseURL}${imageUrl}`;
    return fullUrl;
  };

  const handleChange: UploadProps['onChange'] = async (info) => {
    const { file } = info;
    const fileObj = (file.originFileObj || file) as File;

    if (fileObj && fileObj instanceof File) {
      try {
        message.loading({ content: '正在上传图片...', key: 'upload' });
        const url = await handleUpload(fileObj);
        const newUrls = [...valueRef.current, url];
        valueRef.current = newUrls;
        onChange?.(newUrls);
        message.success({ content: '图片上传成功', key: 'upload', duration: 2 });
      } catch (error) {
        console.error('图片上传失败:', error);
        message.error({ content: '图片上传失败,请重试', key: 'upload', duration: 2 });
      }
    } else {
      message.error('无法获取文件对象,请重试');
    }
  };

  const handlePaste = useCallback(
    async (e: React.ClipboardEvent) => {
      if (disabled) {
        return;
      }
      const files = getImageFilesFromClipboardEvent(e);
      if (files.length === 0) {
        return;
      }
      e.preventDefault();
      e.stopPropagation();

      const current = valueRef.current || [];
      const remaining = maxCount - current.length;
      if (remaining <= 0) {
        message.warning(`最多只能上传 ${maxCount} 张图片`);
        return;
      }

      const toUpload = files.slice(0, remaining);
      for (const file of toUpload) {
        try {
          message.loading({ content: '正在上传图片...', key: 'upload' });
          const url = await handleUpload(file);
          const newUrls = [...valueRef.current, url];
          valueRef.current = newUrls;
          onChange?.(newUrls);
          message.success({ content: '图片上传成功', key: 'upload', duration: 2 });
        } catch (error) {
          console.error('图片上传失败:', error);
          message.error({ content: '图片上传失败,请重试', key: 'upload', duration: 2 });
        }
      }
    },
    [disabled, maxCount, onChange]
  );

  const handleRemove = (index: number) => {
    const newUrls = value.filter((_, i) => i !== index);
    onChange?.(newUrls);
  };

  return (
    <div>
      {!disabled && (
        <div
          onPaste={handlePaste}
          tabIndex={0}
          title="点此聚焦后 Ctrl+V 粘贴图片（不会打开文件选择框）"
          style={{
            display: 'inline-block',
            padding: '4px 8px',
            border: '1px dashed #d9d9d9',
            borderRadius: 4,
            fontSize: 12,
            color: '#666',
            cursor: 'pointer',
            marginBottom: 8,
            outline: 'none',
          }}
          onClick={(e) => {
            (e.currentTarget as HTMLDivElement).focus();
          }}
        >
          支持截图粘贴：点此后按 Ctrl+V 上传图片
        </div>
      )}

      <Space wrap>
        {value.map((url, index) => (
          <div key={index} style={{ position: 'relative', display: 'inline-block' }}>
            <Image
              src={url}
              alt={`图片 ${index + 1}`}
              style={{ width: 60, height: 60, objectFit: 'cover', borderRadius: 4 }}
              preview={{
                mask: <div>预览</div>
              }}
            />
            {!disabled && (
              <Popconfirm
                title="确定删除这张图片吗？"
                onConfirm={() => handleRemove(index)}
                okText="确定"
                cancelText="取消"
              >
                <Button
                  type="text"
                  danger
                  size="small"
                  icon={<DeleteOutlined />}
                  style={{
                    position: 'absolute',
                    top: -8,
                    right: -8,
                    minWidth: 20,
                    height: 20,
                    padding: 0,
                    borderRadius: '50%',
                    backgroundColor: '#ff4d4f',
                    color: 'white',
                    border: 'none'
                  }}
                />
              </Popconfirm>
            )}
          </div>
        ))}

        {!disabled && value.length < maxCount && (
          <Upload
            listType="picture-card"
            showUploadList={false}
            beforeUpload={() => false}
            onChange={handleChange}
            fileList={fileList}
            accept="image/*"
          >
            <div>
              <PlusOutlined />
              <div style={{ marginTop: 8 }}>上传图片</div>
            </div>
          </Upload>
        )}
      </Space>

      {value.length > 0 && (
        <div style={{ marginTop: 8, fontSize: 12, color: '#666' }}>
          已上传 {value.length}/{maxCount} 张图片
        </div>
      )}
    </div>
  );
};

export default MultiImageUploadField;
