package com.reportweb.security;

import com.reportweb.entity.Project;
import com.reportweb.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectAccessTest {

    @Test
    void canAccessProject_assignedRoleInProgress_isAllowed() {
        Project p = new Project();
        p.setUserId("owner-1");
        p.setStatus("InProgress");
        p.setReviewerNdt("张三");

        assertTrue(ProjectAccess.canAccessProject(p, false, false, "other-owner", "张三"));
    }

    @Test
    void canAccessProject_unassignedNonOwner_isDenied() {
        Project p = new Project();
        p.setUserId("owner-1");
        p.setStatus("InProgress");
        p.setWriterNdt("李四");

        assertFalse(ProjectAccess.canAccessProject(p, false, false, "other-owner", "王五"));
    }

    @Test
    void canAccessProject_completedAssignedRole_isDenied() {
        Project p = new Project();
        p.setUserId("owner-1");
        p.setStatus("Completed");
        p.setReviewerNdt("张三");

        assertFalse(ProjectAccess.canAccessProject(p, false, false, "other-owner", "张三"));
    }

    @Test
    void isProjectResponsible_trimmedName_matchTrue() {
        Project p = new Project();
        p.setResponsiblePerson("  赵六 ");
        assertTrue(ProjectAccess.isProjectResponsible(p, "赵六"));
    }

    @Test
    void canAccessProject_ownerMainAccount_anyStatus() {
        Project p = new Project();
        p.setUserId("owner-1");
        p.setStatus("Completed");

        User owner = new User();
        owner.setId("owner-1");
        owner.setRole("USER");

        assertTrue(ProjectAccess.canAccessProject(owner, p));
    }

    @Test
    void canAccessProject_assignedReviewerViaUser() {
        Project p = new Project();
        p.setUserId("owner-1");
        p.setStatus("InProgress");
        p.setApproverChem("审核员乙");

        User reviewer = new User();
        reviewer.setId("user-b");
        reviewer.setRole("USER");
        reviewer.setFullName("审核员乙");

        assertTrue(ProjectAccess.canAccessProject(reviewer, p));
    }
}
