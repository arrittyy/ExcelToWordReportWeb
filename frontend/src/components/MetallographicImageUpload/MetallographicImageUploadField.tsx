import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Upload, Button, Image, Space, Popconfirm, message, Modal, Tag } from 'antd';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import type { UploadFile, UploadProps } from 'antd';
import { imageService } from '@/services/imageService';
import { getImageFilesFromClipboardEvent } from '@/utils/clipboardImages';
import MetallographicImageCropModal from '../MetallographicImageCropModal/MetallographicImageCropModal';

interface MetallographicImageUploadFieldProps {
  value?: string[];
  metCroppedFlags?: boolean[];
  onChange?: (urls: string[], metCroppedFlags: boolean[]) => void;
  maxCount?: number;
  disabled?: boolean;
}

const MetallographicImageUploadField: React.FC<MetallographicImageUploadFieldProps> = ({
  value = [],
  metCroppedFlags = [],
  onChange,
  maxCount = 5,
  disabled = false,
}) => {
  const [fileList] = useState<UploadFile[]>([]);
  const valueRef = useRef(value);
  const flagsRef = useRef(metCroppedFlags);

  useEffect(() => {
    valueRef.current = value;
  }, [value]);

  useEffect(() => {
    flagsRef.current = metCroppedFlags;
  }, [metCroppedFlags]);

  const [cropModalOpen, setCropModalOpen] = useState(false);
  const [choiceModalOpen, setChoiceModalOpen] = useState(false);
  const [pendingFile, setPendingFile] = useState<File | null>(null);

  const buildFullUrl = (imageUrl: string) => {
    const hostname = window.location.hostname;
    const isDev = import.meta.env.DEV;
    const baseURL = isDev
      ? hostname !== 'localhost' && hostname !== '127.0.0.1'
        ? `http://${hostname}:8080`
        : ''
      : '';
    return imageUrl.startsWith('http') ? imageUrl : `${baseURL}${imageUrl}`;
  };

  const appendImage = (url: string, cropped: boolean) => {
    const newUrls = [...valueRef.current, url];
    const newFlags = [...flagsRef.current, cropped];
    valueRef.current = newUrls;
    flagsRef.current = newFlags;
    onChange?.(newUrls, newFlags);
  };

  const uploadFile = async (file: File, cropped: boolean): Promise<void> => {
    message.loading({ content: '正在上传图片...', key: 'upload' });
    const response = await imageService.upload(file);
    appendImage(buildFullUrl(response.url), cropped);
    message.success({ content: '图片上传成功', key: 'upload', duration: 2 });
  };

  const askCropAndProcess = (file: File) => {
    setPendingFile(file);
    setChoiceModalOpen(true);
  };

  const handleChoiceCrop = () => {
    setChoiceModalOpen(false);
    setCropModalOpen(true);
  };

  const handleChoiceDirectUpload = () => {
    const file = pendingFile;
    setChoiceModalOpen(false);
    if (!file) return;
    uploadFile(file, false).catch(() => {
      message.error({ content: '图片上传失败,请重试', key: 'upload', duration: 2 });
    });
  };

  const handleChoiceCancel = () => {
    setChoiceModalOpen(false);
    setPendingFile(null);
  };

  const handleChange: UploadProps['onChange'] = async (info) => {
    const { file } = info;
    const fileObj = (file.originFileObj || file) as File;
    if (!fileObj || !(fileObj instanceof File)) {
      message.error('无法获取文件对象,请重试');
      return;
    }
    if (valueRef.current.length >= maxCount) {
      message.warning(`最多只能上传 ${maxCount} 张图片`);
      return;
    }
    askCropAndProcess(fileObj);
  };

  const handlePaste = useCallback(
    async (e: React.ClipboardEvent) => {
      if (disabled) return;
      const files = getImageFilesFromClipboardEvent(e);
      if (files.length === 0) return;
      e.preventDefault();
      e.stopPropagation();

      const remaining = maxCount - valueRef.current.length;
      if (remaining <= 0) {
        message.warning(`最多只能上传 ${maxCount} 张图片`);
        return;
      }
      askCropAndProcess(files[0]);
    },
    [disabled, maxCount],
  );

  const handleCropConfirm = async (blob: Blob) => {
    setCropModalOpen(false);
    const fileName = pendingFile?.name?.replace(/\.[^.]+$/, '') || 'met-image';
    const file = new File([blob], `${fileName}-cropped.jpg`, { type: 'image/jpeg' });
    setPendingFile(null);
    try {
      await uploadFile(file, true);
    } catch {
      message.error({ content: '图片上传失败,请重试', key: 'upload', duration: 2 });
    }
  };

  const handleCropCancel = () => {
    setCropModalOpen(false);
    setPendingFile(null);
  };

  const handleRemove = (index: number) => {
    const newUrls = value.filter((_, i) => i !== index);
    const newFlags = metCroppedFlags.filter((_, i) => i !== index);
    valueRef.current = newUrls;
    flagsRef.current = newFlags;
    onChange?.(newUrls, newFlags);
  };

  return (
    <div>
      {!disabled && (
        <div
          onPaste={handlePaste}
          tabIndex={0}
          title="点此聚焦后 Ctrl+V 粘贴图片"
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
          支持截图粘贴：点此后按 Ctrl+V 上传（可选金相标尺裁剪）
        </div>
      )}

      <Space wrap>
        {value.map((url, index) => (
          <div key={index} style={{ position: 'relative', display: 'inline-block' }}>
            <Image
              src={url}
              alt={`图片 ${index + 1}`}
              style={{ width: 60, height: 60, objectFit: 'cover', borderRadius: 4 }}
              preview={{ mask: <div>预览</div> }}
            />
            {metCroppedFlags[index] && (
              <Tag
                color="blue"
                style={{
                  position: 'absolute',
                  bottom: 0,
                  left: 0,
                  fontSize: 10,
                  margin: 0,
                  lineHeight: '14px',
                  padding: '0 4px',
                }}
              >
                已裁剪
              </Tag>
            )}
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
                    border: 'none',
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

      <Modal
        title="是否进行金相标尺裁剪？"
        open={choiceModalOpen}
        onCancel={handleChoiceCancel}
        footer={
          <Space>
            <Button onClick={handleChoiceCancel}>取消</Button>
            <Button onClick={handleChoiceDirectUpload}>否，直接上传</Button>
            <Button type="primary" onClick={handleChoiceCrop}>
              是，裁剪
            </Button>
          </Space>
        }
      >
        选择「是」将进入裁剪与标尺编辑；选择「否」将直接上传原图。
      </Modal>

      <MetallographicImageCropModal
        open={cropModalOpen}
        file={pendingFile}
        onConfirm={handleCropConfirm}
        onCancel={handleCropCancel}
      />
    </div>
  );
};

export default MetallographicImageUploadField;
