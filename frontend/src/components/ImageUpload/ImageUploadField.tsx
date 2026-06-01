import React, { useCallback, useState } from 'react';
import { Upload, Button, message } from 'antd';
import { UploadOutlined, DeleteOutlined } from '@ant-design/icons';
import { imageService } from '@/services/imageService';
import { getImageFilesFromClipboardEvent } from '@/utils/clipboardImages';

interface ImageUploadFieldProps {
  value?: string;
  onChange?: (value: string) => void;
  disabled?: boolean;
}

const ImageUploadField: React.FC<ImageUploadFieldProps> = ({
  value,
  onChange,
  disabled = false
}) => {
  const [uploading, setUploading] = useState(false);

  const uploadFile = useCallback(
    async (file: File) => {
      setUploading(true);
      try {
        const response = await imageService.upload(file);
        onChange?.(response.url);
        message.success('图片上传成功');
      } catch (error) {
        message.error('图片上传失败');
      } finally {
        setUploading(false);
      }
    },
    [onChange]
  );

  const handleUpload = async (file: File) => {
    await uploadFile(file);
    return false;
  };

  const handlePaste = useCallback(
    async (e: React.ClipboardEvent) => {
      if (disabled || uploading) {
        return;
      }
      const files = getImageFilesFromClipboardEvent(e);
      if (files.length === 0) {
        return;
      }
      e.preventDefault();
      e.stopPropagation();
      if (value) {
        message.warning('已有图片，请先删除后再粘贴');
        return;
      }
      if (files.length > 1) {
        message.info('仅上传剪贴板中的第一张图片');
      }
      await uploadFile(files[0]);
    },
    [disabled, uploading, value, uploadFile]
  );

  const handleRemove = () => {
    onChange?.('');
  };

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
      {value ? (
        <>
          <img
            src={value}
            alt="上传的图片"
            style={{ width: 40, height: 30, objectFit: 'cover', borderRadius: 4 }}
          />
          <Button
            type="text"
            size="small"
            icon={<DeleteOutlined />}
            onClick={handleRemove}
            disabled={disabled}
          />
        </>
      ) : (
        <>
          <Upload
            beforeUpload={handleUpload}
            showUploadList={false}
            disabled={disabled || uploading}
          >
            <Button
              icon={<UploadOutlined />}
              size="small"
              loading={uploading}
            >
              上传图片
            </Button>
          </Upload>

          <div
            onPaste={handlePaste}
            tabIndex={disabled || uploading ? -1 : 0}
            title="点此聚焦后 Ctrl+V 粘贴图片（不会打开文件选择框）"
            style={{
              padding: '2px 6px',
              border: '1px dashed #d9d9d9',
              borderRadius: 4,
              fontSize: 12,
              color: '#666',
              cursor: disabled || uploading ? 'not-allowed' : 'pointer',
              outline: 'none',
            }}
            onClick={(e) => {
              if (disabled || uploading) return;
              (e.currentTarget as HTMLDivElement).focus();
            }}
          >
            Ctrl+V 粘贴图片
          </div>
        </>
      )}
    </div>
  );
};

export default ImageUploadField;
