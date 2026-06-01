package com.reportweb.controller;

import com.reportweb.dto.AuthDTOs;
import com.reportweb.entity.User;
import com.reportweb.repository.UserRepository;
import com.reportweb.security.UserRoleUtils;
import com.reportweb.security.AspNetIdentityPasswordEncoder;
import com.reportweb.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final AspNetIdentityPasswordEncoder aspNetIdentityPasswordEncoder;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthDTOs.LoginRequest loginRequest) {
        try {
            log.debug("Login attempt for username: {}", loginRequest.getUsername());

            // 查找用户：先按用户名，再按规范化用户名（不区分大小写，便于子账号等登录）
            String loginName = loginRequest.getUsername();
            User user = userRepository.findByUserName(loginName)
                    .orElseGet(() -> userRepository.findByNormalizedUserName(loginName != null ? loginName.toUpperCase() : "")
                            .orElse(null));

            if (user == null) {
                log.warn("User not found: {}", loginName);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "用户名或密码错误");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            // 验证密码 - 优先使用 ASP.NET Identity 兼容编码器
            boolean passwordMatches = false;
            String storedPassword = user.getPasswordHash();
            
            if (storedPassword != null && !storedPassword.isEmpty()) {
                // 记录密码哈希格式（仅前20个字符，用于调试）
                String hashPrefix = storedPassword.length() > 20 ? storedPassword.substring(0, 20) + "..." : storedPassword;
                log.debug("Attempting password verification for user: {}, hash format: {}", loginRequest.getUsername(), hashPrefix);
                
                // 尝试使用 ASP.NET Identity 格式验证
                passwordMatches = aspNetIdentityPasswordEncoder.matches(loginRequest.getPassword(), storedPassword);
                
                // 如果 ASP.NET Identity 验证失败，尝试 BCrypt（用于新用户）
                if (!passwordMatches) {
                    log.debug("ASP.NET Identity verification failed, trying BCrypt for user: {}", loginRequest.getUsername());
                    
                    // 测试：验证已知的 BCrypt 哈希值（密码：123456）
                    String knownHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi";
                    boolean testMatch = passwordEncoder.matches("123456", knownHash);
                    log.debug("Test: Does '123456' match known hash? {}", testMatch);
                    log.debug("Test: Does '123456' match stored hash? {}", passwordEncoder.matches("123456", storedPassword));
                    log.debug("Test: Stored hash length: {}, Expected: 60", storedPassword.length());
                    log.debug("Test: Stored hash equals known hash? {}", storedPassword.equals(knownHash));
                    
                    passwordMatches = passwordEncoder.matches(loginRequest.getPassword(), storedPassword);
                    if (passwordMatches) {
                        log.debug("BCrypt verification succeeded for user: {}", loginRequest.getUsername());
                    } else {
                        log.debug("BCrypt verification also failed for user: {}", loginRequest.getUsername());
                        log.debug("Input password length: {}", loginRequest.getPassword() != null ? loginRequest.getPassword().length() : 0);
                    }
                } else {
                    log.debug("ASP.NET Identity verification succeeded for user: {}", loginRequest.getUsername());
                }
            } else {
                log.warn("User {} has no password hash stored", loginRequest.getUsername());
            }

            if (!passwordMatches) {
                log.warn("Invalid password for user: {}", loginRequest.getUsername());
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "用户名或密码错误");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            // 生成 JWT token（直接使用用户名，因为我们已经手动验证了密码）
            String token = jwtTokenProvider.generateToken(loginRequest.getUsername());

            // 构建响应：子账号返回 parentUserId 且 role 为 SUB_USER
            AuthDTOs.LoginResponse response = new AuthDTOs.LoginResponse();
            response.setToken(token);
            response.setUserId(user.getId());
            response.setUsername(user.getUserName());
            response.setFullName(user.getFullName());
            response.setDepartment(user.getDepartment());
            response.setEmail(user.getEmail());
            if (user.getParentUserId() != null && !user.getParentUserId().isEmpty()) {
                response.setRole(UserRoleUtils.ROLE_SUB_USER);
                response.setParentUserId(user.getParentUserId());
            } else {
                response.setRole(user.getRole() != null ? user.getRole() : "USER");
            }

            log.info("User logged in successfully: {}", loginRequest.getUsername());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during login", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "登录失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 临时测试端点：生成 123456 的正确 BCrypt 哈希值
    @GetMapping("/test/generate-hash")
    public ResponseEntity<?> generateHash() {
        String password = "123456";
        String hash = passwordEncoder.encode(password);
        log.info("Generated BCrypt hash for '123456': {}", hash);
        Map<String, String> response = new HashMap<>();
        response.put("password", password);
        response.put("hash", hash);
        response.put("verification", String.valueOf(passwordEncoder.matches(password, hash)));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthDTOs.RegisterRequest registerRequest) {
        try {
            log.debug("Registration attempt for username: {}", registerRequest.getUsername());

            // 检查用户名是否已存在
            if (userRepository.existsByUserName(registerRequest.getUsername())) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "用户名已存在");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // 检查邮箱是否已存在
            if (userRepository.existsByEmail(registerRequest.getEmail())) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "邮箱已被注册");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // 创建新用户
            User newUser = new User();
            newUser.setId(java.util.UUID.randomUUID().toString());
            newUser.setUserName(registerRequest.getUsername());
            newUser.setNormalizedUserName(registerRequest.getUsername().toUpperCase());
            newUser.setEmail(registerRequest.getEmail());
            newUser.setNormalizedEmail(registerRequest.getEmail().toUpperCase());
            newUser.setFullName(registerRequest.getFullName());
            newUser.setDepartment(registerRequest.getDepartment());
            newUser.setRole("USER"); // 新注册的用户默认为普通用户
            
            // 使用 BCrypt 编码密码（新用户使用标准 BCrypt）
            newUser.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));

            // 保存用户
            userRepository.save(newUser);

            log.info("User registered successfully: {}", registerRequest.getUsername());
            
            Map<String, String> successResponse = new HashMap<>();
            successResponse.put("message", "注册成功");
            return ResponseEntity.status(HttpStatus.CREATED).body(successResponse);

        } catch (Exception e) {
            log.error("Error during registration", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "注册失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody AuthDTOs.ChangePasswordRequest changePasswordRequest) {
        try {
            // 获取当前登录用户
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String username;
            
            if (principal instanceof UserDetails) {
                username = ((UserDetails) principal).getUsername();
            } else {
                username = principal.toString();
            }
            
            log.debug("Change password request for user: {}", username);

            // 查找用户
            User user = userRepository.findByUserName(username)
                    .orElse(null);

            if (user == null) {
                log.warn("User not found: {}", username);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "用户不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // 验证当前密码
            boolean currentPasswordMatches = false;
            String storedPassword = user.getPasswordHash();
            
            if (storedPassword != null && !storedPassword.isEmpty()) {
                // 尝试使用 ASP.NET Identity 格式验证
                currentPasswordMatches = aspNetIdentityPasswordEncoder.matches(changePasswordRequest.getCurrentPassword(), storedPassword);
                
                // 如果 ASP.NET Identity 验证失败，尝试 BCrypt
                if (!currentPasswordMatches) {
                    currentPasswordMatches = passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), storedPassword);
                }
            }

            if (!currentPasswordMatches) {
                log.warn("Invalid current password for user: {}", username);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "当前密码错误");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // 检查新密码是否与当前密码相同
            if (changePasswordRequest.getCurrentPassword().equals(changePasswordRequest.getNewPassword())) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "新密码不能与当前密码相同");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // 更新密码（使用 BCrypt）
            user.setPasswordHash(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
            userRepository.save(user);

            log.info("Password changed successfully for user: {}", username);
            
            Map<String, String> successResponse = new HashMap<>();
            successResponse.put("message", "密码修改成功");
            return ResponseEntity.ok(successResponse);

        } catch (Exception e) {
            log.error("Error during password change", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "密码修改失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}

