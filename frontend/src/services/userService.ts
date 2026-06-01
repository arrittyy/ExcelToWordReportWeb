import apiClient from '@/utils/axios';
import { UserList, CreateUserRequest, UpdateUserRequest, UserStats } from '../types';

export const userService = {
  /**
   * 获取所有用户列表
   */
  getAllUsers: async (): Promise<UserList[]> => {
    const response = await apiClient.get<UserList[]>('/admin/users');
    return response.data;
  },

  /**
   * 获取用户统计信息
   */
  getUserStats: async (): Promise<UserStats> => {
    const response = await apiClient.get<UserStats>('/admin/users/stats');
    return response.data;
  },

  /**
   * 创建用户
   */
  createUser: async (data: CreateUserRequest): Promise<UserList> => {
    const response = await apiClient.post<UserList>('/admin/users', data);
    return response.data;
  },

  /**
   * 更新用户
   */
  updateUser: async (id: string, data: UpdateUserRequest): Promise<UserList> => {
    const response = await apiClient.put<UserList>(`/admin/users/${id}`, data);
    return response.data;
  },

  /**
   * 删除用户
   */
  deleteUser: async (id: string): Promise<void> => {
    await apiClient.delete(`/admin/users/${id}`);
  },
};
