import { LoginResponse, UserRole } from '../types';

/**
 * 检查当前用户是否为管理员
 */
export const isAdmin = (): boolean => {
  const userStr = localStorage.getItem('user');
  if (!userStr) {
    return false;
  }

  try {
    const user: LoginResponse = JSON.parse(userStr);
    return user.role === 'ADMIN';
  } catch {
    return false;
  }
};

/**
 * 检查当前用户是否为子账号（录入账号）
 */
export const isSubUser = (): boolean => {
  const userStr = localStorage.getItem('user');
  if (!userStr) {
    return false;
  }
  try {
    const user: LoginResponse = JSON.parse(userStr);
    return user.role === 'SUB_USER' || !!(user.parentUserId != null && user.parentUserId !== '');
  } catch {
    return false;
  }
};

/**
 * 获取当前用户角色
 */
export const getCurrentUserRole = (): UserRole | null => {
  const userStr = localStorage.getItem('user');
  if (!userStr) {
    return null;
  }

  try {
    const user: LoginResponse = JSON.parse(userStr);
    return (user.role as UserRole) || 'USER';
  } catch {
    return null;
  }
};

/**
 * 获取当前用户信息
 */
export const getCurrentUser = (): LoginResponse | null => {
  const userStr = localStorage.getItem('user');
  if (!userStr) {
    return null;
  }

  try {
    return JSON.parse(userStr);
  } catch {
    return null;
  }
};
