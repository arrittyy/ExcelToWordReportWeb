import type { AxiosHeaders, AxiosRequestConfig, AxiosRequestHeaders } from 'axios';
import apiClient, { JSON_FETCH_LONG_TIMEOUT_MS } from '@/utils/axios';
import type { ProjectList, ProjectDetail, CreateProject, UpdateProject, TodoItem, ImageAttachment, ProjectOverviewPreview } from '@/types';

const LARGE_ATTACHMENT_UPLOAD_TIMEOUT_MS = 300_000;
const WORD_EXPORT_JOB_REQUEST_TIMEOUT_MS = 30_000;

/**
 * Word 在服务端整包生成后再一次性下载；第三方单项可能含大量附图，生成与传输都很慢。
 * 需与 Nginx `proxy_read_timeout` / `proxy_send_timeout`（秒）及云上 SLB 超时对齐；不足时再同比调大。
 */
const WORD_EXPORT_TIMEOUT_MS = 3_600_000; // 60 分钟

const wordExportConfig: AxiosRequestConfig = {
  timeout: WORD_EXPORT_TIMEOUT_MS,
};

const wordExportJobConfig: AxiosRequestConfig = {
  timeout: WORD_EXPORT_JOB_REQUEST_TIMEOUT_MS,
};

/** 让浏览器为 FormData 自动设置 multipart boundary（覆盖实例默认的 application/json） */
function stripContentTypeForMultipart(data: unknown, headers?: AxiosRequestHeaders): unknown {
  if (headers && typeof (headers as AxiosHeaders).delete === 'function') {
    (headers as AxiosHeaders).delete('Content-Type');
  } else if (headers && typeof headers === 'object') {
    delete (headers as Record<string, unknown>)['Content-Type'];
  }
  return data;
}

const multipartUploadConfig: AxiosRequestConfig = {
  timeout: LARGE_ATTACHMENT_UPLOAD_TIMEOUT_MS,
  transformRequest: [stripContentTypeForMultipart],
};

export const projectService = {
  getAll: async (): Promise<ProjectList[]> => {
    const response = await apiClient.get<ProjectList[]>('/projects');
    return response.data;
  },

  getMyTodos: async (): Promise<TodoItem[]> => {
    const response = await apiClient.get<TodoItem[]>('/projects/my-todos');
    return response.data;
  },

  submitApproval: async (id: number, track: 'ndt' | 'chem' | 'both'): Promise<void> => {
    await apiClient.post(`/projects/${id}/submit-approval`, { track });
  },

  approvalPass: async (id: number, track: 'ndt' | 'chem'): Promise<void> => {
    await apiClient.post(`/projects/${id}/approval/pass`, { track });
  },

  approvalReject: async (id: number, track: 'ndt' | 'chem'): Promise<void> => {
    await apiClient.post(`/projects/${id}/approval/reject`, { track });
  },

  approvalRollback: async (id: number, track: 'ndt' | 'chem'): Promise<void> => {
    await apiClient.post(`/projects/${id}/approval/rollback`, { track });
  },

  getByUserId: async (userId: string): Promise<ProjectList[]> => {
    const response = await apiClient.get<ProjectList[]>(`/projects/user/${userId}`);
    return response.data;
  },

  getById: async (id: number): Promise<ProjectDetail> => {
    const response = await apiClient.get<ProjectDetail>(`/projects/${id}`, {
      timeout: JSON_FETCH_LONG_TIMEOUT_MS,
    });
    return response.data;
  },

  create: async (data: CreateProject): Promise<ProjectDetail> => {
    const response = await apiClient.post<ProjectDetail>('/projects', data);
    return response.data;
  },

  update: async (id: number, data: UpdateProject): Promise<void> => {
    await apiClient.put(`/projects/${id}`, data);
  },

  saveAggregateDetectionLogOrder: async (
    id: number,
    body: {
      version?: number;
      componentKeys: string[];
      reportIdsByComponent: Record<string, number[]>;
      experimentTypeOrder?: string[];
    },
  ): Promise<ProjectDetail> => {
    const response = await apiClient.put<ProjectDetail>(
      `/projects/${id}/aggregate-detection-log-order`,
      body,
    );
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/projects/${id}`);
  },

  generateSummaryWord: async (id: number): Promise<Blob> => {
    const response = await apiClient.get(`/projects/${id}/generate-summary-word`, {
      responseType: 'blob',
      ...wordExportConfig,
    });
    return response.data;
  },

  uploadSummaryNotificationSigned: async (id: number, file: File): Promise<ProjectDetail> => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await apiClient.post<ProjectDetail>(
      `/projects/${id}/summary-notification-signed`,
      formData,
      multipartUploadConfig
    );
    return response.data;
  },

  deleteSummaryNotificationSigned: async (id: number): Promise<ProjectDetail> => {
    const response = await apiClient.delete<ProjectDetail>(`/projects/${id}/summary-notification-signed`);
    return response.data;
  },

  uploadSummaryThirdPartyFull: async (id: number, file: File): Promise<ProjectDetail> => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await apiClient.post<ProjectDetail>(
      `/projects/${id}/summary-third-party-full`,
      formData,
      multipartUploadConfig
    );
    return response.data;
  },

  deleteSummaryThirdPartyFull: async (id: number): Promise<ProjectDetail> => {
    const response = await apiClient.delete<ProjectDetail>(`/projects/${id}/summary-third-party-full`);
    return response.data;
  },

  getReportFigures: async (id: number): Promise<ImageAttachment[]> => {
    const response = await apiClient.get<ImageAttachment[]>(`/projects/${id}/report-figures`);
    return response.data ?? [];
  },

  getOverviewPreview: async (id: number): Promise<ProjectOverviewPreview> => {
    const response = await apiClient.get<ProjectOverviewPreview>(`/projects/${id}/overview-preview`, {
      timeout: JSON_FETCH_LONG_TIMEOUT_MS,
    });
    return response.data;
  },

  saveReportFigures: async (id: number, figures: ImageAttachment[]): Promise<ImageAttachment[]> => {
    const response = await apiClient.put<ImageAttachment[]>(`/projects/${id}/report-figures`, figures);
    return response.data ?? [];
  },

  generateThirdPartyWord: async (id: number): Promise<Blob> => {
    const response = await apiClient.get(`/projects/${id}/generate-third-party-word`, {
      responseType: 'blob',
      ...wordExportConfig,
    });
    return response.data;
  },

  generateDetectionNotificationWord: async (
    id: number,
    body: { reportIds: number[] }
  ): Promise<Blob> => {
    const response = await apiClient.post(`/projects/${id}/generate-detection-notification-word`, body, {
      responseType: 'blob',
      ...wordExportConfig,
    });
    return response.data;
  },

  createWordExportJob: async (
    id: number,
    body: CreateWordExportJobRequest
  ): Promise<WordExportJob> => {
    const response = await apiClient.post<WordExportJob>(`/projects/${id}/word-export-jobs`, body, {
      ...wordExportJobConfig,
    });
    return response.data;
  },

  getWordExportJob: async (id: number, jobId: string): Promise<WordExportJob> => {
    const response = await apiClient.get<WordExportJob>(`/projects/${id}/word-export-jobs/${jobId}`, {
      ...wordExportJobConfig,
    });
    return response.data;
  },

  /** 无对应类型的导出记录时后端返回 404；无权访问时可能 403：均属探测，勿触发全局 toast */
  getLatestWordExportJob: async (id: number, type: WordExportJobType): Promise<WordExportJob> => {
    const response = await apiClient.get<WordExportJob>(`/projects/${id}/word-export-jobs/latest`, {
      params: { type },
      ...wordExportJobConfig,
      suppressNotFoundMessage: true,
      suppressServerErrorMessage: true,
      suppressForbiddenMessage: true,
    });
    return response.data;
  },

  downloadWordExportJob: async (id: number, jobId: string): Promise<Blob> => {
    const response = await apiClient.get(`/projects/${id}/word-export-jobs/${jobId}/download`, {
      responseType: 'blob',
      ...wordExportConfig,
    });
    return response.data;
  },

  getApprovalLogs: async (projectId: number): Promise<ApprovalLogEntry[]> => {
    try {
      const response = await apiClient.get<ApprovalLogEntry[]>(`/projects/${projectId}/approval-logs`);
      return response.data ?? [];
    } catch {
      return [];
    }
  },

  getReportChangeLogs: async (
    projectId: number,
    params?: { limit?: number; offset?: number },
  ): Promise<ReportChangeLogEntry[]> => {
    const response = await apiClient.get<ReportChangeLogEntry[]>(
      `/projects/${projectId}/report-change-logs`,
      { params },
    );
    return response.data ?? [];
  },

  getReportChangeSummary: async (projectId: number): Promise<ReportChangeLogSummaryResponse> => {
    const response = await apiClient.get<ReportChangeLogSummaryResponse>(
      `/projects/${projectId}/report-change-summary`,
    );
    return response.data ?? { byExperimentType: [] };
  },
};

export interface ApprovalLogEntry {
  id?: number;
  projectId: number;
  track: string;
  action: string;
  actorName: string;
  createdAt: string;
}

export interface ReportChangeLogEntry {
  id: number;
  projectId: number;
  reportId: number;
  action: string;
  experimentTypeId: number;
  experimentTypeName: string;
  experimentTypeCode: string;
  reportNumber?: string;
  testMethod?: string;
  status?: string;
  changeSummary?: Record<string, unknown>;
  operatorUserId: string;
  operatorUserName?: string;
  source: string;
  createdAt: string;
  reportDeleted?: boolean;
}

export interface ReportChangeLogSummaryRow {
  experimentTypeId: number;
  experimentTypeName: string;
  experimentTypeCode: string;
  createdCount: number;
  updatedCount: number;
  deletedCount: number;
  currentReportCount: number;
}

export interface ReportChangeLogSummaryResponse {
  byExperimentType: ReportChangeLogSummaryRow[];
}

export type WordExportJobType = 'SUMMARY' | 'THIRD_PARTY' | 'DETECTION_NOTIFICATION';
export type WordExportJobStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED';

export interface CreateWordExportJobRequest {
  type: WordExportJobType;
  reportIds?: number[];
}

export interface WordExportJob {
  jobId: string;
  type: WordExportJobType;
  status: WordExportJobStatus;
  suggestedFileName?: string;
  errorMessage?: string;
  createdAt?: string;
  startedAt?: string;
  finishedAt?: string;
}



