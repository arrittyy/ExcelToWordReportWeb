import apiClient, { JSON_FETCH_LONG_TIMEOUT_MS } from '@/utils/axios';
import {
  ReportList,
  ReportDetail,
  CreateReport,
  UpdateReport,
  ExportTextPreview,
  ExportTextOverridesPatch,
} from '@/types';

export const reportService = {
  getAll: async (): Promise<ReportList[]> => {
    const response = await apiClient.get<ReportList[]>('/reports');
    return response.data;
  },

  getById: async (id: number): Promise<ReportDetail> => {
    const response = await apiClient.get<ReportDetail>(`/reports/${id}`);
    return response.data;
  },

  getByProject: async (projectId: number): Promise<ReportList[]> => {
    const response = await apiClient.get<ReportList[]>(`/reports?projectId=${projectId}`, {
      timeout: JSON_FETCH_LONG_TIMEOUT_MS,
    });
    return response.data;
  },

  create: async (data: CreateReport): Promise<ReportDetail> => {
    const response = await apiClient.post<ReportDetail>('/reports', data);
    return response.data;
  },

  update: async (id: number, data: UpdateReport): Promise<void> => {
    await apiClient.put(`/reports/${id}`, data);
  },

  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/reports/${id}`);
  },

  generateWord: async (id: number): Promise<Blob> => {
    const response = await apiClient.get(`/reports/${id}/generate-word`, {
      responseType: 'blob',
    });
    return response.data;
  },

  /** 多选合并为单个正式单项 Word（无封面/概述），超时加长 */
  batchGenerateWordMerged: async (ids: number[]): Promise<Blob> => {
    const response = await apiClient.post('/reports/batch-generate-word-merged', ids, {
      responseType: 'blob',
      timeout: 600_000,
    });
    return response.data;
  },

  getReportItems: async (reportId: number): Promise<any> => {
    const response = await apiClient.get(`/reports/${reportId}/items`);
    return response.data;
  },

  // 批量删除报告
  batchDelete: async (ids: number[]): Promise<void> => {
    await apiClient.post('/reports/batch-delete', ids);
  },

  // 批量更新报告状态
  batchUpdateStatus: async (ids: number[], status: string): Promise<void> => {
    await apiClient.post('/reports/batch-update-status', { ids, status });
  },

  getExportTextPreview: async (id: number, contentRowIndex = 0): Promise<ExportTextPreview> => {
    const response = await apiClient.get<ExportTextPreview>(`/reports/${id}/export-text-preview`, {
      params: { contentRowIndex },
    });
    return response.data;
  },

  putExportTextOverrides: async (id: number, patch: ExportTextOverridesPatch): Promise<void> => {
    await apiClient.put(`/reports/${id}/export-text-overrides`, patch);
  },

  /** 里氏硬度：按编号推断管件/钢管/焊缝（与后端 LeebHardnessCategoryResolver 一致） */
  leebClassifySuggestions: async (
    numbers: string[],
  ): Promise<Array<{ number: string; suggestedCategory: string }>> => {
    const response = await apiClient.post('/reports/leeb-classify-suggestions', { numbers });
    return response.data;
  },
};


