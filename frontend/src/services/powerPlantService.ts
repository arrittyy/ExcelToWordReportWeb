import apiClient from '@/utils/axios';

export interface PowerPlant {
  id: number;
  name: string;
  region: string;
  shortName?: string;
  province: string;
  city: string;
  address: string;
  phone?: string;
  fax?: string;
  remark?: string;
  createdAt: string;
  updatedAt: string;
  units?: Unit[];
}

export interface Unit {
  id: number;
  powerPlantId: number;
  unitName: string; // 保留用于向后兼容，实际显示使用unitNumber
  unitNumber?: string;
  installedCapacity?: string; // 机组装机容量
  remark?: string;
  createdAt: string;
  updatedAt: string;
  components?: UnitComponent[];
}

export interface UnitComponent {
  id: number;
  unitId: number;
  componentName: string;
  material?: string;
  category?: string;
  pipeDiameter?: string;
  wallThickness?: string;
  remark?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePowerPlant {
  name: string;
  region: string;
  shortName?: string;
  province: string;
  city: string;
  address: string;
  phone?: string;
  fax?: string;
  remark?: string;
}

export interface UpdatePowerPlant extends CreatePowerPlant {}

export interface CreateUnit {
  unitNumber: string; // 机组编号（必填）
  installedCapacity?: string; // 机组装机容量
}

export interface UpdateUnit extends CreateUnit {}

export interface CreateUnitComponent {
  componentName: string;
  material?: string;
  category?: string;
  pipeDiameter?: string;
  wallThickness?: string;
  remark?: string;
}

export interface UpdateUnitComponent extends CreateUnitComponent {}

export const powerPlantService = {
  // 获取所有电厂列表
  getAll: async (): Promise<PowerPlant[]> => {
    const response = await apiClient.get<PowerPlant[]>('/power-plants');
    return response.data;
  },

  // 获取电厂详情（包含机组列表）
  getById: async (id: number): Promise<PowerPlant> => {
    const response = await apiClient.get<PowerPlant>(`/power-plants/${id}`);
    return response.data;
  },

  // 创建电厂
  create: async (data: CreatePowerPlant): Promise<PowerPlant> => {
    const response = await apiClient.post<PowerPlant>('/power-plants', data);
    return response.data;
  },

  // 更新电厂
  update: async (id: number, data: UpdatePowerPlant): Promise<PowerPlant> => {
    const response = await apiClient.put<PowerPlant>(`/power-plants/${id}`, data);
    return response.data;
  },

  // 删除电厂
  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/power-plants/${id}`);
  },
};

export const unitService = {
  // 获取电厂下的所有机组
  getByPowerPlantId: async (powerPlantId: number): Promise<Unit[]> => {
    const response = await apiClient.get<Unit[]>(`/power-plants/${powerPlantId}/units`);
    return response.data;
  },

  // 获取机组详情（包含部件列表）
  getById: async (id: number): Promise<Unit> => {
    const response = await apiClient.get<Unit>(`/units/${id}`);
    return response.data;
  },

  // 创建机组
  create: async (powerPlantId: number, data: CreateUnit): Promise<Unit> => {
    const response = await apiClient.post<Unit>(`/power-plants/${powerPlantId}/units`, data);
    return response.data;
  },

  // 更新机组
  update: async (id: number, data: UpdateUnit): Promise<Unit> => {
    const response = await apiClient.put<Unit>(`/units/${id}`, data);
    return response.data;
  },

  // 删除机组
  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/units/${id}`);
  },
};

export const unitComponentService = {
  // 获取机组下的所有部件
  getByUnitId: async (unitId: number): Promise<UnitComponent[]> => {
    const response = await apiClient.get<UnitComponent[]>(`/units/${unitId}/components`);
    return response.data;
  },

  // 创建部件
  create: async (unitId: number, data: CreateUnitComponent): Promise<UnitComponent> => {
    const response = await apiClient.post<UnitComponent>(`/units/${unitId}/components`, data);
    return response.data;
  },

  // 更新部件
  update: async (id: number, data: UpdateUnitComponent): Promise<UnitComponent> => {
    const response = await apiClient.put<UnitComponent>(`/unit-components/${id}`, data);
    return response.data;
  },

  // 删除部件
  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/unit-components/${id}`);
  },
};
