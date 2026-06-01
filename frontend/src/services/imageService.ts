import apiClient from '@/utils/axios';
import { ImageDTO, ImageUploadResponse } from '@/types';
import { compressImageForUpload } from '@/utils/imageCompression';

export const imageService = {
  upload: async (file: File): Promise<ImageUploadResponse> => {
    const compressedFile = await compressImageForUpload(file);
    const formData = new FormData();
    formData.append('file', compressedFile);
    
    const response = await apiClient.post<ImageUploadResponse>('/images/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      // 10MB 上传 + 弱网下会更慢，给更长的超时时间
      timeout: 120000,
    });
    return response.data;
  },

  getMyImages: async (): Promise<ImageDTO[]> => {
    const response = await apiClient.get<ImageDTO[]>('/images/my-images');
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/images/${id}`);
  },

  getImageUrl: (id: number): string => {
    return `/api/images/${id}`;
  },
};


