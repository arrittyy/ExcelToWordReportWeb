package com.reportweb.security;

import com.reportweb.entity.Project;
import com.reportweb.entity.User;
import com.reportweb.repository.ProjectRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 项目读权限：管理员、归属人（及子账号进行中规则）、进行中且被分配编写/审核/批准/负责人。
 */
public final class ProjectAccess {

    public static final String PROJECT_STATUS_IN_PROGRESS = "InProgress";

    private ProjectAccess() {
    }

    public static String effectiveUserId(User currentUser) {
        if (currentUser == null) {
            return null;
        }
        return UserRoleUtils.isSubUser(currentUser) && currentUser.getParentUserId() != null
                ? currentUser.getParentUserId()
                : currentUser.getId();
    }

    public static String[] principalNames(User currentUser) {
        if (currentUser == null) {
            return new String[0];
        }
        List<String> names = new ArrayList<>(2);
        String full = currentUser.getFullName() != null ? currentUser.getFullName().trim() : "";
        if (!full.isEmpty()) {
            names.add(full);
        }
        String userName = currentUser.getUserName() != null ? currentUser.getUserName().trim() : "";
        if (!userName.isEmpty() && names.stream().noneMatch(n -> eqName(n, userName))) {
            names.add(userName);
        }
        return names.toArray(new String[0]);
    }

    public static boolean eqName(String left, String right) {
        return left != null && right != null && left.trim().equals(right.trim());
    }

    public static boolean matchesAnyName(String roleName, String... principalNames) {
        if (roleName == null || roleName.isBlank() || principalNames == null || principalNames.length == 0) {
            return false;
        }
        for (String n : principalNames) {
            if (eqName(roleName, n)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAssignedApprovalRole(Project project, String... principalNames) {
        if (project == null || principalNames == null || principalNames.length == 0) {
            return false;
        }
        return matchesAnyName(project.getWriterNdt(), principalNames)
                || matchesAnyName(project.getReviewerNdt(), principalNames)
                || matchesAnyName(project.getApproverNdt(), principalNames)
                || matchesAnyName(project.getWriterChem(), principalNames)
                || matchesAnyName(project.getReviewerChem(), principalNames)
                || matchesAnyName(project.getApproverChem(), principalNames)
                || matchesAnyName(project.getResponsiblePerson(), principalNames);
    }

    public static boolean isProjectResponsible(Project project, String... principalNames) {
        return project != null && matchesAnyName(project.getResponsiblePerson(), principalNames);
    }

    public static boolean canAccessProject(User currentUser, Project project) {
        if (currentUser == null) {
            return false;
        }
        return canAccessProject(
                project,
                UserRoleUtils.isAdmin(currentUser),
                UserRoleUtils.isSubUser(currentUser),
                effectiveUserId(currentUser),
                principalNames(currentUser));
    }

    public static boolean canAccessProject(
            Project project,
            boolean isAdmin,
            boolean isSubUser,
            String effectiveUserId,
            String... principalNames) {
        if (project == null) {
            return false;
        }
        if (isAdmin) {
            return true;
        }
        if (effectiveUserId != null && effectiveUserId.equals(project.getUserId())) {
            if (!isSubUser) {
                return true;
            }
            return PROJECT_STATUS_IN_PROGRESS.equals(project.getStatus());
        }
        return PROJECT_STATUS_IN_PROGRESS.equals(project.getStatus())
                && isAssignedApprovalRole(project, principalNames);
    }

    public static Project findProjectIfAccessible(ProjectRepository projectRepository, User currentUser, Integer id) {
        if (projectRepository == null || currentUser == null || id == null) {
            return null;
        }
        Project project = projectRepository.findById(id).orElse(null);
        if (!canAccessProject(currentUser, project)) {
            return null;
        }
        return project;
    }

    public static boolean isOwnedByEffectiveUser(Project project, String effectiveUserId) {
        return project != null && effectiveUserId != null && Objects.equals(project.getUserId(), effectiveUserId);
    }
}
