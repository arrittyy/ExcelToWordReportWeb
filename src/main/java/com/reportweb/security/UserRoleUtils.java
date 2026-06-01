package com.reportweb.security;

import com.reportweb.entity.User;

/**
 * 用户角色工具类
 * 用于判断用户角色和权限
 */
public class UserRoleUtils {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";
    public static final String ROLE_SUB_USER = "SUB_USER";

    /**
     * 判断用户是否为管理员
     * 
     * @param user 用户对象
     * @return 如果是管理员返回true，否则返回false
     */
    public static boolean isAdmin(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return ROLE_ADMIN.equals(user.getRole());
    }

    /**
     * 判断角色是否为管理员
     * 
     * @param role 角色字符串
     * @return 如果是管理员角色返回true，否则返回false
     */
    public static boolean isAdmin(String role) {
        return ROLE_ADMIN.equals(role);
    }

    /**
     * 判断用户是否为普通用户
     * 
     * @param user 用户对象
     * @return 如果是普通用户返回true，否则返回false
     */
    public static boolean isUser(User user) {
        if (user == null || user.getRole() == null) {
            return true; // 默认为普通用户
        }
        return ROLE_USER.equals(user.getRole());
    }

    /**
     * 判断角色是否为普通用户
     * 
     * @param role 角色字符串
     * @return 如果是普通用户角色返回true，否则返回false
     */
    public static boolean isUser(String role) {
        return role == null || ROLE_USER.equals(role);
    }

    /**
     * 判断用户是否为子账号（录入账号）
     */
    public static boolean isSubUser(User user) {
        if (user == null) return false;
        return ROLE_SUB_USER.equals(user.getRole()) || (user.getParentUserId() != null && !user.getParentUserId().isEmpty());
    }

    /**
     * 判断角色是否为子账号
     */
    public static boolean isSubUser(String role) {
        return ROLE_SUB_USER.equals(role);
    }
}
