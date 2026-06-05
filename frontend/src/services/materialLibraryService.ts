import apiClient from '@/utils/axios';
import type { MaterialCategory } from '@/constants/materialLibraryFields';

export type MaterialModificationType = 'CREATE' | 'UPDATE' | 'DELETE';
export type MaterialEntryStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'DELETED';
export type MaterialEntrySource = 'USER' | 'SEEDED';

export interface MaterialLibraryEntry {
  id?: number;
  materialKey: string;
  primaryCategory?: MaterialCategory;
  status: MaterialEntryStatus;
  source: MaterialEntrySource;
  modificationType?: MaterialModificationType;
  pendingChange?: boolean;
  properties: Record<string, string>;
  submittedByUserName?: string;
  reviewedByUserName?: string;
  reviewComment?: string;
  createdAt?: string;
  reviewedAt?: string;
  /** 我的提交列表展示用；修改/删除驳回时库内 status 仍为 APPROVED */
  submissionStatus?: MaterialEntryStatus;
}

export interface MaterialApprovalLogItem {
  id: number;
  action: string;
  actorUserName?: string;
  comment?: string;
  createdAt: string;
}

export interface CreateMaterialRequest {
  materialKey: string;
  primaryCategory: MaterialCategory;
  properties: Record<string, string>;
}

export interface UpdateMaterialRequest {
  primaryCategory: MaterialCategory;
  properties: Record<string, string>;
}

export interface RejectMaterialRequest {
  reviewComment: string;
}

export interface MaterialLibraryCapabilities {
  canReview: boolean;
  canSubmit: boolean;
  pendingReviewCount: number;
  rejectedCount: number;
}

export const MODIFICATION_TYPE_LABELS: Record<MaterialModificationType, string> = {
  CREATE: '新增',
  UPDATE: '修改',
  DELETE: '删除',
};

export const STATUS_LABELS: Record<MaterialEntryStatus, string> = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  DELETED: '已删除',
};

export const materialLibraryService = {
  capabilities: async (): Promise<MaterialLibraryCapabilities> => {
    const response = await apiClient.get<MaterialLibraryCapabilities>('/material-library/capabilities');
    return response.data;
  },

  list: async (params?: {
    category?: MaterialCategory;
    keyword?: string;
  }): Promise<MaterialLibraryEntry[]> => {
    const response = await apiClient.get<MaterialLibraryEntry[]>('/material-library', { params });
    return response.data;
  },

  listKeys: async (): Promise<string[]> => {
    const response = await apiClient.get<{ keys: string[] }>('/material-library/keys');
    return response.data.keys;
  },

  listPending: async (): Promise<MaterialLibraryEntry[]> => {
    const response = await apiClient.get<MaterialLibraryEntry[]>('/material-library/pending');
    return response.data;
  },

  listMySubmissions: async (): Promise<MaterialLibraryEntry[]> => {
    const response = await apiClient.get<MaterialLibraryEntry[]>('/material-library/my-submissions');
    return response.data;
  },

  submit: async (data: CreateMaterialRequest): Promise<MaterialLibraryEntry> => {
    const response = await apiClient.post<MaterialLibraryEntry>('/material-library', data);
    return response.data;
  },

  update: async (id: number, data: UpdateMaterialRequest): Promise<MaterialLibraryEntry> => {
    const response = await apiClient.put<MaterialLibraryEntry>(`/material-library/${id}`, data);
    return response.data;
  },

  deleteRequest: async (id: number): Promise<MaterialLibraryEntry> => {
    const response = await apiClient.post<MaterialLibraryEntry>(`/material-library/${id}/delete-request`);
    return response.data;
  },

  deleteDraft: async (id: number): Promise<void> => {
    await apiClient.delete(`/material-library/${id}`);
  },

  approve: async (id: number): Promise<MaterialLibraryEntry> => {
    const response = await apiClient.post<MaterialLibraryEntry>(`/material-library/${id}/approve`);
    return response.data;
  },

  reject: async (id: number, data: RejectMaterialRequest): Promise<MaterialLibraryEntry> => {
    const response = await apiClient.post<MaterialLibraryEntry>(`/material-library/${id}/reject`, data);
    return response.data;
  },

  listLogs: async (id: number): Promise<MaterialApprovalLogItem[]> => {
    const response = await apiClient.get<MaterialApprovalLogItem[]>(`/material-library/${id}/logs`);
    return response.data;
  },
};
