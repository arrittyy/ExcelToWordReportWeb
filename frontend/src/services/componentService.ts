import apiClient, { JSON_FETCH_LONG_TIMEOUT_MS } from '@/utils/axios';
import { UnitComponent } from './powerPlantService';

/** 存库：null/undefined 表示按名称自动；PHI=Φ，M，NONE=无前缀 */
export type ComponentSpecPrefix = 'PHI' | 'M' | 'NONE';

export interface ProjectComponent {
  id: number;
  projectId: number;
  componentName: string;
  material?: string;
  category?: string;
  pipeDiameter?: string;
  wallThickness?: string;
  specPrefix?: ComponentSpecPrefix | null;
  threadPitch?: string | null;
  /** 与后端 formatSpecUnified 一致；缺失时前端可 fallback */
  displaySpec?: string | null;
  remark?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AvailableComponents {
  projectComponents: ProjectComponent[];
  unitComponents: UnitComponent[];
}

export interface CreateComponentRequest {
  componentName: string;
  material?: string;
  category?: string;
  pipeDiameter?: string;
  wallThickness?: string;
  specPrefix?: ComponentSpecPrefix | null;
  threadPitch?: string | null;
  remark?: string;
}

export interface UpdateComponentRequest {
  componentName: string;
  material?: string;
  category?: string;
  pipeDiameter?: string;
  wallThickness?: string;
  specPrefix?: ComponentSpecPrefix | null;
  threadPitch?: string | null;
  remark?: string;
}

export const componentService = {
  getProjectComponents: async (projectId: number): Promise<ProjectComponent[]> => {
    const response = await apiClient.get<ProjectComponent[]>(`/projects/${projectId}/components`, {
      timeout: JSON_FETCH_LONG_TIMEOUT_MS,
    });
    return response.data;
  },

  createComponent: async (projectId: number, data: CreateComponentRequest): Promise<ProjectComponent> => {
    const response = await apiClient.post<ProjectComponent>(`/projects/${projectId}/components`, data);
    return response.data;
  },

  updateComponent: async (id: number, data: UpdateComponentRequest): Promise<void> => {
    await apiClient.put(`/components/${id}`, data);
  },

  deleteComponent: async (id: number): Promise<void> => {
    await apiClient.delete(`/components/${id}`);
  },

  // 获取项目的可用部件列表（包含项目部件和机组部件）
  getAvailableComponents: async (projectId: number): Promise<AvailableComponents> => {
    const response = await apiClient.get<AvailableComponents>(`/projects/${projectId}/available-components`);
    return response.data;
  },

  // 从机组导入部件
  importComponentsFromUnit: async (projectId: number, unitComponentIds: number[]): Promise<{ importedComponents: ProjectComponent[]; count: number }> => {
    const response = await apiClient.post<{ importedComponents: ProjectComponent[]; count: number }>(
      `/projects/${projectId}/components/import-from-unit`,
      { unitComponentIds }
    );
    return response.data;
  },
};
