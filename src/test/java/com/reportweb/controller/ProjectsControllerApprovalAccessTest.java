package com.reportweb.controller;

import com.reportweb.entity.Project;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectsControllerApprovalAccessTest {

    private static ProjectsController controller() {
        return new ProjectsController(
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }

    @Test
    void canAccessProject_assignedRoleInProgress_isAllowed() throws Exception {
        ProjectsController c = controller();
        Method m = ProjectsController.class.getDeclaredMethod(
                "canAccessProject", Project.class, boolean.class, boolean.class, String.class, String[].class);
        m.setAccessible(true);

        Project p = new Project();
        p.setUserId("owner-1");
        p.setStatus("InProgress");
        p.setReviewerNdt("张三");

        boolean ok = (boolean) m.invoke(c, p, false, false, "other-owner", new String[]{"张三"});
        assertTrue(ok);
    }

    @Test
    void canAccessProject_unassignedNonOwner_isDenied() throws Exception {
        ProjectsController c = controller();
        Method m = ProjectsController.class.getDeclaredMethod(
                "canAccessProject", Project.class, boolean.class, boolean.class, String.class, String[].class);
        m.setAccessible(true);

        Project p = new Project();
        p.setUserId("owner-1");
        p.setStatus("InProgress");
        p.setWriterNdt("李四");

        boolean ok = (boolean) m.invoke(c, p, false, false, "other-owner", new String[]{"王五"});
        assertFalse(ok);
    }

    @Test
    void isProjectResponsible_trimmedName_matchTrue() throws Exception {
        Method m = ProjectsController.class.getDeclaredMethod(
                "isProjectResponsible", Project.class, String[].class);
        m.setAccessible(true);

        Project p = new Project();
        p.setResponsiblePerson("  赵六 ");
        boolean ok = (boolean) m.invoke(null, p, new String[]{"赵六"});
        assertTrue(ok);
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
}
