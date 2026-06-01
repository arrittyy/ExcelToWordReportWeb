package com.reportweb.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

public class UserDTOs {

    @Data
    public static class UserList {
        private String id;
        private String username;
        private String fullName;
        private String email;
        private String role;
        private String department;
        private LocalDateTime createdAt;
        private String parentUserId;   // 子账号的主账号 ID
        private String parentFullName; // 主账号姓名，便于列表展示
    }

    @Data
    public static class CreateUserRequest {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
        private String username;

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
        private String password;

        @Email(message = "邮箱格式不正确")
        private String email;

        @NotBlank(message = "姓名不能为空")
        @Size(max = 200, message = "姓名长度不能超过200个字符")
        private String fullName;

        @Size(max = 100, message = "部门长度不能超过100个字符")
        private String department;

        @NotBlank(message = "角色不能为空")
        private String role; // "ADMIN"、"USER" 或 "SUB_USER"

        /** 创建子账号时必填：主账号用户 ID */
        private String parentUserId;
    }

    @Data
    public static class UpdateUserRequest {
        @NotBlank(message = "姓名不能为空")
        @Size(max = 200, message = "姓名长度不能超过200个字符")
        private String fullName;

        @Email(message = "邮箱格式不正确")
        private String email;

        @Size(max = 100, message = "部门长度不能超过100个字符")
        private String department;

        @NotBlank(message = "角色不能为空")
        private String role; // "ADMIN"、"USER" 或 "SUB_USER"
    }

    @Data
    public static class UserStats {
        private long adminCount;
        private long userCount;
        private long subUserCount;
        private long totalCount;
    }
}
