import apiClient from '@/utils/axios';
import type {
  OverviewStatistics,
  ExperimentDistribution,
  ReportTrend,
  QualityStatistics,
} from '@/types';

export const statisticsService = {
  getOverview: async (
    startDate?: string,
    endDate?: string
  ): Promise<OverviewStatistics> => {
    const response = await apiClient.get<OverviewStatistics>(
      '/statistics/overview',
      {
        params: { startDate, endDate },
      }
    );
    return response.data;
  },

  getExperimentDistribution: async (
    startDate?: string,
    endDate?: string
  ): Promise<ExperimentDistribution[]> => {
    const response = await apiClient.get<ExperimentDistribution[]>(
      '/statistics/experiment-distribution',
      {
        params: { startDate, endDate },
      }
    );
    return response.data;
  },

  getReportTrend: async (
    startDate?: string,
    endDate?: string,
    groupBy: string = 'day'
  ): Promise<ReportTrend[]> => {
    const response = await apiClient.get<ReportTrend[]>(
      '/statistics/report-trend',
      {
        params: { startDate, endDate, groupBy },
      }
    );
    return response.data;
  },

  getQualityStatistics: async (
    startDate?: string,
    endDate?: string
  ): Promise<QualityStatistics[]> => {
    const response = await apiClient.get<QualityStatistics[]>(
      '/statistics/quality-statistics',
      {
        params: { startDate, endDate },
      }
    );
    return response.data;
  },

  exportReports: async (startDate?: string, endDate?: string): Promise<Blob> => {
    const response = await apiClient.get('/statistics/export-reports', {
      params: { startDate, endDate },
      responseType: 'blob',
    });
    return response.data;
  },

  exportStatistics: async (
    startDate?: string,
    endDate?: string
  ): Promise<Blob> => {
    const response = await apiClient.get('/statistics/export-statistics', {
      params: { startDate, endDate },
      responseType: 'blob',
    });
    return response.data;
  },
};



