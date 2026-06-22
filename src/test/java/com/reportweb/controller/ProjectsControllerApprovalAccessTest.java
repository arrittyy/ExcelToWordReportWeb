package com.reportweb.controller;

import com.reportweb.entity.Project;
import com.reportweb.entity.User;
import com.reportweb.security.ProjectAccess;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectsControllerApprovalAccessTest {

    private static ProjectsController controller() {
        return new ProjectsController(
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }

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
    void isProjectResponsible_trimmedName_matchTrue() {
        Project p = new Project();
        p.setResponsiblePerson("  赵六 ");
        assertTrue(ProjectAccess.isProjectResponsible(p, "赵六"));
    }

    @Test
    void canSubmitAtWriterStep_responsiblePersonAllowed() throws Exception {
        Method m = ProjectsController.class.getDeclaredMethod(
                "canSubmitAtWriterStep", Project.class, String.class, String[].class);
        m.setAccessible(true);

        Project p = new Project();
        p.setWriterNdt("编写人甲");
        p.setResponsiblePerson("负责人乙");
        p.setApprovalStepNdt(0);

        boolean ok = (boolean) m.invoke(null, p, "ndt", new String[]{"负责人乙"});
        assertTrue(ok);
    }

    @Test
    void canSubmitAtWriterStep_nonWriterAndNonResponsibleDenied() throws Exception {
        Method m = ProjectsController.class.getDeclaredMethod(
                "canSubmitAtWriterStep", Project.class, String.class, String[].class);
        m.setAccessible(true);

        Project p = new Project();
        p.setWriterChem("编写人甲");
        p.setResponsiblePerson("负责人乙");
        p.setApprovalStepChem(0);

        boolean ok = (boolean) m.invoke(null, p, "chem", new String[]{"其他人"});
        assertFalse(ok);
    }

    @Test
    void canRollbackApproval_mainAccountOwner_allowed() throws Exception {
        Method m = ProjectsController.class.getDeclaredMethod(
                "canRollbackApproval", Project.class, User.class, String[].class);
        m.setAccessible(true);

        Project p = new Project();
        p.setUserId("owner-1");
        User owner = new User();
        owner.setId("owner-1");
        owner.setRole("USER");

        assertTrue((boolean) m.invoke(null, p, owner, new String[]{"owner"}));
    }

    @Test
    void canRollbackApproval_responsiblePersonMainAccount_allowed() throws Exception {
        Method m = ProjectsController.class.getDeclaredMethod(
                "canRollbackApproval", Project.class, User.class, String[].class);
        m.setAccessible(true);

        Project p = new Project();
        p.setUserId("owner-1");
        p.setResponsiblePerson("负责人甲");
        User responsible = new User();
        responsible.setId("other-id");
        responsible.setRole("USER");

        assertTrue((boolean) m.invoke(null, p, responsible, new String[]{"负责人甲"}));
    }

    @Test
    void canRollbackApproval_subUser_denied() throws Exception {
        Method m = ProjectsController.class.getDeclaredMethod(
                "canRollbackApproval", Project.class, User.class, String[].class);
        m.setAccessible(true);

        Project p = new Project();
        p.setUserId("owner-1");
        User sub = new User();
        sub.setId("sub-1");
        sub.setRole("SUB_USER");
        sub.setParentUserId("owner-1");

        assertFalse((boolean) m.invoke(null, p, sub, new String[]{"子账号"}));
    }

    @Test
    void canRollbackApproval_nonOwnerNonResponsible_denied() throws Exception {
        Method m = ProjectsController.class.getDeclaredMethod(
                "canRollbackApproval", Project.class, User.class, String[].class);
        m.setAccessible(true);

        Project p = new Project();
        p.setUserId("owner-1");
        p.setResponsiblePerson("负责人甲");
        User other = new User();
        other.setId("owner-2");
        other.setRole("USER");

        assertFalse((boolean) m.invoke(null, p, other, new String[]{"其他人"}));
    }

    @Test
    void resetApprovalTrack_ndtOnly_clearsNdtAndReopensCompletedProject() throws Exception {
        Method m = ProjectsController.class.getDeclaredMethod("resetApprovalTrack", Project.class, String.class);
        m.setAccessible(true);

        Project p = new Project();
        p.setStatus("Completed");
        p.setApprovalStepNdt(3);
        p.setWriterNdt("编写甲");
        p.setReviewerNdt("审核乙");
        p.setApproverNdt("批准丙");
        p.setRejectionStepNdt(1);
        p.setApprovalStepChem(3);
        p.setWriterChem("编写A");

        m.invoke(null, p, "ndt");

        assertEquals(0, p.getApprovalStepNdt());
        assertEquals(null, p.getRejectionStepNdt());
        assertEquals(null, p.getWriterNdt());
        assertEquals(null, p.getReviewerNdt());
        assertEquals(null, p.getApproverNdt());
        assertEquals(3, p.getApprovalStepChem());
        assertEquals("编写A", p.getWriterChem());
        assertEquals("InProgress", p.getStatus());
    }

    @Test
    void trackNeedsRollback_ndtWithPersonnel_true() throws Exception {
        Method m = ProjectsController.class.getDeclaredMethod("trackNeedsRollback", Project.class, String.class);
        m.setAccessible(true);

        Project p = new Project();
        p.setApprovalStepNdt(0);
        p.setWriterNdt("编写甲");

        assertTrue((boolean) m.invoke(null, p, "ndt"));
    }

    @Test
    void trackNeedsRollback_ndtEmpty_false() throws Exception {
        Method m = ProjectsController.class.getDeclaredMethod("trackNeedsRollback", Project.class, String.class);
        m.setAccessible(true);

        Project p = new Project();
        p.setApprovalStepNdt(0);

        assertFalse((boolean) m.invoke(null, p, "ndt"));
    }
}
