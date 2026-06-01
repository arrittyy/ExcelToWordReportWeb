package com.reportweb.controller;

import com.reportweb.dto.UserDTOs;
import com.reportweb.entity.User;
import com.reportweb.repository.UserRepository;
import com.reportweb.repository.ProjectRepository;
import com.reportweb.security.UserRoleUtils;
import com.reportweb.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int MAX_ADMIN_COUNT = 5;
    private static final int MAX_USER_COUNT = 30;

    @GetMapping
    public ResponseEntity<List<UserDTOs.UserList>> getAllUsers(Authentication authentication) {
        try {
            // 管理员在前，普通用户在后，同角色内按创建时间倒序
            List<User> users = userRepository.findAllOrderByRoleAndCreatedAtDesc();
            List<UserDTOs.UserList> userList = users.stream()
                    .map(this::convertToUserListDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(userList);
        } catch (Exception ex) {
            log.error("Error getting all users", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<UserDTOs.UserStats> getUserStats(Authentication authentication) {
        try {
            long adminCount = userRepository.countByRole(UserRoleUtils.ROLE_ADMIN);
            long userCount = userRepository.countByRole(UserRoleUtils.ROLE_USER);
            long subUserCount = userRepository.countByRole(UserRoleUtils.ROLE_SUB_USER);
            long totalCount = userRepository.count();

            UserDTOs.UserStats stats = new UserDTOs.UserStats();
            stats.setAdminCount(adminCount);
            stats.setUserCount(userCount);
            stats.setSubUserCount(subUserCount);
            stats.setTotalCount(totalCount);

            return ResponseEntity.ok(stats);
        } catch (Exception ex) {
            log.error("Error getting user stats", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createUser(
            @Valid @RequestBody UserDTOs.CreateUserRequest createUserRequest,
            Authentication authentication) {
        try {
            // 验证角色：ADMIN、USER、SUB_USER
            String role = createUserRequest.getRole();
            if (!UserRoleUtils.ROLE_ADMIN.equals(role) &&
                !UserRoleUtils.ROLE_USER.equals(role) &&
                !UserRoleUtils.ROLE_SUB_USER.equals(role)) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "角色必须是ADMIN、USER或SUB_USER");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 子账号必须指定主账号
            if (UserRoleUtils.ROLE_SUB_USER.equals(role)) {
                if (createUserRequest.getParentUserId() == null || createUserRequest.getParentUserId().trim().isEmpty()) {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("message", "创建子账号时必须选择主账号");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                User parent = userRepository.findById(createUserRequest.getParentUserId().trim()).orElse(null);
                if (parent == null) {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("message", "主账号用户不存在");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                if (parent.getParentUserId() != null && !parent.getParentUserId().isEmpty()) {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("message", "主账号不能是子账号");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }

            // 检查数量限制（SUB_USER 不占用普通用户名额）
            if (UserRoleUtils.ROLE_ADMIN.equals(role)) {
                long adminCount = userRepository.countByRole(UserRoleUtils.ROLE_ADMIN);
                if (adminCount >= MAX_ADMIN_COUNT) {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("message", String.format("管理员数量已达上限（%d个）", MAX_ADMIN_COUNT));
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            } else if (UserRoleUtils.ROLE_USER.equals(role)) {
                long userCount = userRepository.countByRole(UserRoleUtils.ROLE_USER);
                if (userCount >= MAX_USER_COUNT) {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("message", String.format("普通用户数量已达上限（%d个）", MAX_USER_COUNT));
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }

            // 检查用户名是否已存在
            if (userRepository.existsByUserName(createUserRequest.getUsername())) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "用户名已存在");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 检查邮箱是否已存在（只有当邮箱不为空时才检查）
            if (createUserRequest.getEmail() != null && !createUserRequest.getEmail().isEmpty()) {
                if (userRepository.existsByEmail(createUserRequest.getEmail())) {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("message", "邮箱已被注册");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }

            // 创建新用户
            User newUser = new User();
            newUser.setId(UUID.randomUUID().toString());
            newUser.setUserName(createUserRequest.getUsername());
            newUser.setNormalizedUserName(createUserRequest.getUsername().toUpperCase());
            newUser.setEmail(createUserRequest.getEmail());
            if (createUserRequest.getEmail() != null && !createUserRequest.getEmail().isEmpty()) {
                newUser.setNormalizedEmail(createUserRequest.getEmail().toUpperCase());
            } else {
                newUser.setNormalizedEmail(null);
            }
            newUser.setFullName(createUserRequest.getFullName());
            newUser.setDepartment(createUserRequest.getDepartment());
            if (UserRoleUtils.ROLE_SUB_USER.equals(role)) {
                newUser.setRole(UserRoleUtils.ROLE_SUB_USER);
                newUser.setParentUserId(createUserRequest.getParentUserId().trim());
            } else {
                newUser.setRole(createUserRequest.getRole());
            }
            newUser.setPasswordHash(passwordEncoder.encode(createUserRequest.getPassword()));
            newUser.setSecurityStamp(UUID.randomUUID().toString());
            newUser.setConcurrencyStamp(UUID.randomUUID().toString());
            newUser.setEmailConfirmed(true);
            newUser.setPhoneNumberConfirmed(false);
            newUser.setTwoFactorEnabled(false);
            newUser.setLockoutEnabled(true);
            newUser.setAccessFailedCount(0);
            newUser.setCreatedAt(LocalDateTime.now());

            User savedUser = userRepository.save(newUser);
            UserDTOs.UserList response = convertToUserListDTO(savedUser);

            log.info("User created successfully: {} with role: {}", createUserRequest.getUsername(), createUserRequest.getRole());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception ex) {
            log.error("Error creating user", ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "创建用户失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UserDTOs.UpdateUserRequest updateUserRequest,
            Authentication authentication) {
        try {
            User user = userRepository.findById(id).orElse(null);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            // 验证角色：ADMIN、USER、SUB_USER（不能通过编辑将主账号改为子账号，子账号需在创建时指定主账号）
            String newRole = updateUserRequest.getRole();
            if (!UserRoleUtils.ROLE_ADMIN.equals(newRole) &&
                !UserRoleUtils.ROLE_USER.equals(newRole) &&
                !UserRoleUtils.ROLE_SUB_USER.equals(newRole)) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "角色必须是ADMIN、USER或SUB_USER");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            if (UserRoleUtils.ROLE_SUB_USER.equals(newRole) && (user.getParentUserId() == null || user.getParentUserId().isEmpty())) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "不能将主账号改为子账号，请使用创建用户并选择主账号");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 如果修改角色，检查数量限制（子账号改为主账号/管理员时占名额）
            String oldRole = user.getRole();
            if (!oldRole.equals(newRole)) {
                if (UserRoleUtils.ROLE_ADMIN.equals(newRole)) {
                    long adminCount = userRepository.countByRole(UserRoleUtils.ROLE_ADMIN);
                    if (!UserRoleUtils.ROLE_ADMIN.equals(oldRole) && adminCount >= MAX_ADMIN_COUNT) {
                        Map<String, String> errorResponse = new HashMap<>();
                        errorResponse.put("message", String.format("管理员数量已达上限（%d个）", MAX_ADMIN_COUNT));
                        return ResponseEntity.badRequest().body(errorResponse);
                    }
                } else if (UserRoleUtils.ROLE_USER.equals(newRole)) {
                    long userCount = userRepository.countByRole(UserRoleUtils.ROLE_USER);
                    if (!UserRoleUtils.ROLE_USER.equals(oldRole) && userCount >= MAX_USER_COUNT) {
                        Map<String, String> errorResponse = new HashMap<>();
                        errorResponse.put("message", String.format("普通用户数量已达上限（%d个）", MAX_USER_COUNT));
                        return ResponseEntity.badRequest().body(errorResponse);
                    }
                }
            }

            // 检查邮箱是否已被其他用户使用（只有当新邮箱不为空时才检查）
            if (updateUserRequest.getEmail() != null && !updateUserRequest.getEmail().isEmpty()) {
                String oldEmail = user.getEmail() != null ? user.getEmail() : "";
                String newEmail = updateUserRequest.getEmail();
                if (!oldEmail.equals(newEmail) && userRepository.existsByEmail(newEmail)) {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("message", "邮箱已被其他用户使用");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }

            // 更新用户信息
            user.setFullName(updateUserRequest.getFullName());
            user.setEmail(updateUserRequest.getEmail());
            if (updateUserRequest.getEmail() != null && !updateUserRequest.getEmail().isEmpty()) {
                user.setNormalizedEmail(updateUserRequest.getEmail().toUpperCase());
            } else {
                user.setNormalizedEmail(null);
            }
            user.setDepartment(updateUserRequest.getDepartment());
            user.setRole(updateUserRequest.getRole());
            // 若改为非子账号角色，清除主账号关联
            if (!UserRoleUtils.ROLE_SUB_USER.equals(updateUserRequest.getRole())) {
                user.setParentUserId(null);
            }

            User updatedUser = userRepository.save(user);
            UserDTOs.UserList response = convertToUserListDTO(updatedUser);

            log.info("User updated successfully: {}", user.getUserName());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error updating user", ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "更新用户失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(
            @PathVariable String id,
            Authentication authentication) {
        try {
            User user = userRepository.findById(id).orElse(null);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            // 检查用户是否有项目
            long projectCount = projectRepository.findByUserIdOrderByCreatedAtDesc(id).size();
            if (projectCount > 0) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", String.format("该用户有%d个项目，无法删除。请先转移或删除这些项目。", projectCount));
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 不能删除自己
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            String currentUserId = userPrincipal.getUser().getId();
            if (id.equals(currentUserId)) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "不能删除自己的账户");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            userRepository.deleteById(id);
            log.info("User deleted successfully: {}", user.getUserName());
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Error deleting user", ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "删除用户失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private UserDTOs.UserList convertToUserListDTO(User user) {
        UserDTOs.UserList dto = new UserDTOs.UserList();
        dto.setId(user.getId());
        dto.setUsername(user.getUserName());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole() != null ? user.getRole() : UserRoleUtils.ROLE_USER);
        dto.setDepartment(user.getDepartment());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setParentUserId(user.getParentUserId());
        if (user.getParentUserId() != null && !user.getParentUserId().isEmpty()) {
            userRepository.findById(user.getParentUserId()).ifPresent(parent ->
                dto.setParentFullName(parent.getFullName()));
        }
        return dto;
    }
}
