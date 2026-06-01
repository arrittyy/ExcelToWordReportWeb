import apiClient, { JSON_FETCH_LONG_TIMEOUT_MS } from '@/utils/axios';

// 项目级别仪器设备（向后兼容）
export interface ProjectInstrument {
  id: number;
  projectId: number;
  instrumentName: string;
  instrumentModel?: string;
  instrumentNumber?: string;
  globalInstrumentId?: number;
  isDefault?: boolean;
  experimentTypeCode?: string;
  createdAt: string;
  updatedAt: string;
}

// 全局库仪器设备
export interface Instrument {
  id: number;
  instrumentName: string;
  instrumentModel?: string;
  instrumentNumber?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateInstrumentRequest {
  instrumentName: string;
  instrumentModel?: string;
  instrumentNumber?: string;
  globalInstrumentId?: number;
  isDefault?: boolean;
  experimentTypeCode?: string;
}

export interface UpdateInstrumentRequest {
  instrumentName: string;
  instrumentModel?: string;
  instrumentNumber?: string;
  globalInstrumentId?: number;
  isDefault?: boolean;
  experimentTypeCode?: string;
}

export const instrumentService = {
  // 全局库仪器设备API
  getAllInstruments: async (search?: string): Promise<Instrument[]> => {
    const params = search ? { search } : {};
    const response = await apiClient.get<Instrument[]>('/instruments', { params });
    return response.data;
  },

  getInstrumentById: async (id: number): Promise<Instrument> => {
    const response = await apiClient.get<Instrument>(`/instruments/${id}`);
    return response.data;
  },

  createInstrument: async (data: CreateInstrumentRequest): Promise<Instrument> => {
    const response = await apiClient.post<Instrument>('/instruments', data);
    return response.data;
  },

  updateInstrument: async (id: number, data: UpdateInstrumentRequest): Promise<Instrument> => {
    const response = await apiClient.put<Instrument>(`/instruments/${id}`, data);
    return response.data;
  },

  deleteInstrument: async (id: number): Promise<void> => {
    await apiClient.delete(`/instruments/${id}`);
  },

  // 项目级别仪器设备API（向后兼容）
  getProjectInstruments: async (projectId: number): Promise<ProjectInstrument[]> => {
    const response = await apiClient.get<ProjectInstrument[]>(`/projects/${projectId}/instruments`, {
      timeout: JSON_FETCH_LONG_TIMEOUT_MS,
    });
    return response.data;
  },

  createProjectInstrument: async (projectId: number, data: CreateInstrumentRequest): Promise<ProjectInstrument> => {
    const response = await apiClient.post<ProjectInstrument>(`/projects/${projectId}/instruments`, data);
    return response.data;
  },

  updateProjectInstrument: async (id: number, data: UpdateInstrumentRequest): Promise<void> => {
    await apiClient.put(`/project-instruments/${id}`, data);
  },

  deleteProjectInstrument: async (id: number): Promise<void> => {
    await apiClient.delete(`/project-instruments/${id}`);
  },

  // 获取指定检测类型的默认设备
  getDefaultInstrument: async (projectId: number, experimentTypeCode: string): Promise<ProjectInstrument | null> => {
    try {
      const response = await apiClient.get<ProjectInstrument>(
        `/projects/${projectId}/instruments/default/${experimentTypeCode}`,
        { suppressNotFoundMessage: true } as any
      );
      return response.data;
    } catch (error: any) {
      if (error.response?.status === 404) {
        return null;
      }
      throw error;
    }
  },
};
