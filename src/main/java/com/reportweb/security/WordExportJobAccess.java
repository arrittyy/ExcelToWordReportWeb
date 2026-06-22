package com.reportweb.security;

import com.reportweb.entity.Project;
import com.reportweb.entity.User;
import com.reportweb.entity.WordExportJob;

import java.util.Objects;


/**
 * Word 导出任务读权限：与项目归属一致，主账号可访问子账号在其项目上创建的任务。
 */
public final class WordExportJobAccess {

    private WordExportJobAccess() {
    }

    public static boolean canAccess(User currentUser, WordExportJob job, Project project) {
        if (currentUser == null || job == null) {
            return false;
        }
        if (UserRoleUtils.isAdmin(currentUser)) {
            return true;
        }
        String effectiveUserId = ProjectAccess.effectiveUserId(currentUser);
        if (Objects.equals(job.getCreatorUserId(), currentUser.getId())
                || Objects.equals(job.getCreatorUserId(), effectiveUserId)) {
            return true;
        }
        return canReadProject(currentUser, project);
    }

    /** 与 {@code ProjectAccess} 读权限一致（含审批角色）。 */
    public static boolean canReadProject(User currentUser, Project project) {
        return ProjectAccess.canAccessProject(currentUser, project);
    }
}
