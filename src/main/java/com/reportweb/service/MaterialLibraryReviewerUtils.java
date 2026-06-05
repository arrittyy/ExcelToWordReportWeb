package com.reportweb.service;

import com.reportweb.entity.User;
import com.reportweb.security.UserRoleUtils;

import java.util.Arrays;
import java.util.List;

public final class MaterialLibraryReviewerUtils {

    public static final List<String> REVIEWER_NAMES = List.of(
            "杨希锐", "李艳军", "魏泉泉", "贾新杰", "王鹏飞", "王红宝", "高秀娜", "胡锋涛");

    private MaterialLibraryReviewerUtils() {
    }

    public static boolean canReview(User user) {
        if (user == null) {
            return false;
        }
        if (UserRoleUtils.isAdmin(user)) {
            return true;
        }
        return matchesReviewerName(user);
    }

    public static boolean matchesReviewerName(User user) {
        if (user == null) {
            return false;
        }
        for (String principal : principalNames(user)) {
            if (principal == null || principal.isBlank()) {
                continue;
            }
            String trimmed = principal.trim();
            for (String reviewer : REVIEWER_NAMES) {
                if (reviewer.equals(trimmed)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String[] principalNames(User user) {
        if (user == null) {
            return new String[0];
        }
        return Arrays.stream(new String[]{user.getFullName(), user.getUserName()})
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .toArray(String[]::new);
    }
}
