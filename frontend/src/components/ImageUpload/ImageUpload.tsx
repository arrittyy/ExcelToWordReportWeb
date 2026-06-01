import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Upload, Modal, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import type { UploadFile, UploadProps } from 'antd';
import { imageService } from '@/services/imageService';
import { getImageFilesFromClipboardEvent } from '@/utils/clipboardImages';

interface ImageUploadProps {
  value?: number[];
  onChange?: (imageIds: number[]) => void;
  maxCount?: number;
}

const ImageUpload: React.FC<ImageUploadProps> = ({ value = [], onChange, maxCount = 10 }) => {
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewImage, setPreviewImage] = useState('');
  const [previewTitle, setPreviewTitle] = useState('');
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const valueRef = useRef(value);
  useEffect(() => {
    valueRef.current = value;
  }, [value]);

  // Convert imageIds to fileList
  React.useEffect(() => {
    const files: UploadFile[] = value.map((id) => ({
      uid: `${id}`,
      name: `image-${id}`,
      status: 'done',
      url: imageService.getImageUrl(id),
    }));
    setFileList(files);
  }, [value]);

  const handlePreview = async (file: UploadFile) => {
    setPreviewImage(file.url || '');
    setPreviewOpen(true);
    setPreviewTitle(file.name || file.url!.substring(file.url!.lastIndexOf('/') + 1));
  };

  const handleChange: UploadProps['onChange'] = ({ fileList: newFileList }) => {
    setFileList(newFileList);

    // Extract image IDs from successful uploads
    const imageIds = newFileList
      .filter((file) => file.status === 'done')
      .map((file) => {
        if (file.response?.id) {
          return Number(file.response.id);
        }
        const parsedUid = Number(file.uid);
        return Number.isFinite(parsedUid) ? parsedUid : null;
      })
      .filter((id): id is number => id !== null);

    // 关键：有些 antd 上传周期里 file.response 在 onChange 事件触发时尚未就绪，
    // 会导致 imageIds 临时为空。我们避免把空值覆盖掉 customRequest 成功时回填的正确结果。
    if (imageIds.length > 0 || newFileList.length === 0) {
      onChange?.(imageIds);
    }
  };

  const uploadOneFile = useCallback(
    async (file: File) => {
      const response = await imageService.upload(file);
      const prev = valueRef.current || [];
      const next = Array.from(new Set([...prev, response.id]));
      valueRef.current = next;
      onChange?.(next);
      message.success('图片上传成功');
      return response;
    },
    [onChange]
  );

  const customRequest: UploadProps['customRequest'] = async (options) => {
    const { file, onSuccess, onError, onProgress } = options;

    try {
      onProgress?.({ percent: 50 });
      const response = await uploadOneFile(file as File);
      onProgress?.({ percent: 100 });
      onSuccess?.(response);
    } catch (error) {
      onError?.(error as Error);
      const anyErr = error as any;
      const errorMsg = anyErr?.response?.data?.message || anyErr?.message || '上传失败';
      message.error(`图片上传失败: ${errorMsg}`);
    }
  };

  const handlePaste = useCallback(
    async (e: React.ClipboardEvent) => {
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
          await uploadOneFile(file);
        } catch (err: any) {
          const errorMsg = err?.response?.data?.message || err?.message || '上传失败';
          message.error(`图片上传失败: ${errorMsg}`);
        }
      }
    },
    [maxCount, uploadOneFile]
  );

  return (
    <>
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
          // 点击自身只用于获取焦点，不触发其他行为
          (e.currentTarget as HTMLDivElement).focus();
        }}
      >
        支持截图粘贴：点此后按 Ctrl+V 上传图片
      </div>
      <div>
        <Upload
          listType="picture-card"
          fileList={fileList}
          onPreview={handlePreview}
          onChange={handleChange}
          customRequest={customRequest}
          maxCount={maxCount}
          accept="image/*"
        >
          {fileList.length >= maxCount ? null : (
            <div>
              <PlusOutlined />
              <div style={{ marginTop: 8 }}>上传图片</div>
            </div>
          )}
        </Upload>
      </div>
      <Modal
        open={previewOpen}
        title={previewTitle}
        footer={null}
        onCancel={() => setPreviewOpen(false)}
      >
        <img alt="preview" style={{ width: '100%' }} src={previewImage} />
      </Modal>
    </>
  );
};

export default ImageUpload;
