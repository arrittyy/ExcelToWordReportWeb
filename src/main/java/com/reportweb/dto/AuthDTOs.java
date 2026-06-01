package com.reportweb.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthDTOs {

    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
        private String username;

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
        private String password;
    }

    @Data
    public static class LoginResponse {
        private String token;
        private String userId;
        private String username;
        private String fullName;
        private String department;
        private String email;
        private String role; // 用户角色：ADMIN、USER 或 SUB_USER
        private String parentUserId; // 子账号的主账号 ID，供前端菜单/路由限制
    }

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
        private String username;

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
        private String password;

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;

        @NotBlank(message = "姓名不能为空")
        @Size(max = 200, message = "姓名长度不能超过200个字符")
        private String fullName;

        @Size(max = 100, message = "部门长度不能超过100个字符")
        private String department;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "当前密码不能为空")
        private String currentPassword;

        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 100, message = "新密码长度必须在6-100个字符之间")
        private String newPassword;
    }
}


