import apiClient from '@/utils/axios';
import { ExperimentType } from '@/types';

export const experimentTypeService = {
  getAll: async (): Promise<ExperimentType[]> => {
    const response = await apiClient.get<ExperimentType[]>('/experimenttypes');
    return response.data;
  },

  getById: async (id: number): Promise<ExperimentType> => {
    const response = await apiClient.get<ExperimentType>(`/experimenttypes/${id}`);
    return response.data;
  },
};


