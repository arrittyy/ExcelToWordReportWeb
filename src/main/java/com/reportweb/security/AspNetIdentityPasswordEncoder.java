package com.reportweb.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@Slf4j
public class AspNetIdentityPasswordEncoder implements PasswordEncoder {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final int SALT_SIZE = 16;
    private static final int ITERATIONS = 10000;

    @Override
    public String encode(CharSequence rawPassword) {
        // 生成随机盐
        byte[] salt = new byte[SALT_SIZE];
        new SecureRandom().nextBytes(salt);

        // 使用 PBKDF2 生成哈希
        byte[] hash = pbkdf2(rawPassword.toString().toCharArray(), salt, ITERATIONS, 32);

        // 组合盐和哈希
        byte[] combined = new byte[salt.length + hash.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(hash, 0, combined, salt.length, hash.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        try {
            // 检查是否为有效的 Base64 字符串（ASP.NET Identity 格式）
            if (encodedPassword == null || encodedPassword.isEmpty()) {
                return false;
            }
            
            // 尝试解码 Base64，如果失败说明不是 ASP.NET Identity 格式
            byte[] combined;
            try {
                combined = Base64.getDecoder().decode(encodedPassword);
            } catch (IllegalArgumentException e) {
                // 不是有效的 Base64 格式，可能是 BCrypt 或其他格式
                log.debug("Password hash is not in ASP.NET Identity format (not Base64), skipping ASP.NET Identity verification");
                return false;
            }
            
            if (combined.length < SALT_SIZE + 32) {
                log.debug("Invalid ASP.NET Identity password format: too short (length: {})", combined.length);
                return false;
            }

            // 提取盐和哈希
            byte[] salt = new byte[SALT_SIZE];
            byte[] hash = new byte[32];
            System.arraycopy(combined, 0, salt, 0, SALT_SIZE);
            System.arraycopy(combined, SALT_SIZE, hash, 0, 32);

            // 计算输入密码的哈希
            byte[] computedHash = pbkdf2(rawPassword.toString().toCharArray(), salt, ITERATIONS, 32);

            // 比较哈希值
            return constantTimeEquals(hash, computedHash);
        } catch (Exception e) {
            log.debug("Error verifying password with ASP.NET Identity format: {}", e.getMessage());
            return false;
        }
    }

    private byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) {
        try {
            // 使用 HMAC-SHA256 实现 PBKDF2
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec keySpec = new SecretKeySpec(new String(password).getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(keySpec);

            byte[] result = new byte[keyLength];
            byte[] u = new byte[mac.getMacLength() + salt.length + 4];
            System.arraycopy(salt, 0, u, 0, salt.length);

            for (int i = 0; i < keyLength; i += mac.getMacLength()) {
                u[salt.length] = (byte) ((i / mac.getMacLength() + 1) >> 24);
                u[salt.length + 1] = (byte) ((i / mac.getMacLength() + 1) >> 16);
                u[salt.length + 2] = (byte) ((i / mac.getMacLength() + 1) >> 8);
                u[salt.length + 3] = (byte) (i / mac.getMacLength() + 1);

                byte[] t = mac.doFinal(u);
                byte[] t1 = t;
                for (int j = 1; j < iterations; j++) {
                    t1 = mac.doFinal(t1);
                    for (int k = 0; k < t.length; k++) {
                        t[k] ^= t1[k];
                    }
                }

                int copyLength = Math.min(t.length, keyLength - i);
                System.arraycopy(t, 0, result, i, copyLength);
            }

            return result;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error in PBKDF2", e);
        }
    }

    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}


