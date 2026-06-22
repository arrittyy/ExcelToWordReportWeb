package com.reportweb.security;

import com.reportweb.entity.Project;
import com.reportweb.entity.User;
import com.reportweb.entity.WordExportJob;
import com.reportweb.entity.WordExportJobType;
import com.reportweb.entity.WordExportJobStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordExportJobAccessTest {

  private static final String PARENT_ID = "a819d51b-9e0b-4319-9d43-5d152436ea85";
  private static final String SUB_ID = "5bcea334-10f9-42bc-a9a7-fa5540695d05";
  private static final String OTHER_ID = "other-user-id";

  @Test
  void parentCanAccessSubCreatedJobOnOwnedProject() {
    User parent = user(PARENT_ID, "USER", null);
    User sub = user(SUB_ID, "SUB_USER", PARENT_ID);
    Project project = project(48, PARENT_ID, "InProgress");
    WordExportJob job = job(SUB_ID, 48);

    assertTrue(WordExportJobAccess.canAccess(parent, job, project));
    assertTrue(WordExportJobAccess.canAccess(sub, job, project));
  }

  @Test
  void parentCanAccessOwnCreatedJob() {
    User parent = user(PARENT_ID, "USER", null);
    Project project = project(48, PARENT_ID, "InProgress");
    WordExportJob job = job(PARENT_ID, 48);

    assertTrue(WordExportJobAccess.canAccess(parent, job, project));
  }

  @Test
  void unrelatedUserCannotAccessJob() {
    User other = user(OTHER_ID, "USER", null);
    Project project = project(48, PARENT_ID, "InProgress");
    WordExportJob job = job(SUB_ID, 48);

    assertFalse(WordExportJobAccess.canAccess(other, job, project));
  }

  @Test
  void subCannotReadCompletedProject() {
    User sub = user(SUB_ID, "SUB_USER", PARENT_ID);
    Project project = project(48, PARENT_ID, "Completed");
    assertFalse(WordExportJobAccess.canReadProject(sub, project));
  }

  @Test
  void assignedApprovalRoleCanReadInProgressProject() {
    User reviewer = user("reviewer-1", "USER", null);
    reviewer.setFullName("张三");
    Project project = project(48, PARENT_ID, "InProgress");
    project.setReviewerNdt("张三");
    assertTrue(WordExportJobAccess.canReadProject(reviewer, project));
  }

  @Test
  void assignedApprovalRoleCannotReadCompletedProject() {
    User reviewer = user("reviewer-1", "USER", null);
    reviewer.setFullName("张三");
    Project project = project(48, PARENT_ID, "Completed");
    project.setReviewerNdt("张三");
    assertFalse(WordExportJobAccess.canReadProject(reviewer, project));
  }

  private static User user(String id, String role, String parentUserId) {
    User u = new User();
    u.setId(id);
    u.setRole(role);
    u.setParentUserId(parentUserId);
    return u;
  }

  private static Project project(int id, String userId, String status) {
    Project p = new Project();
    p.setId(id);
    p.setUserId(userId);
    p.setStatus(status);
    return p;
  }

  private static WordExportJob job(String creatorUserId, int projectId) {
    WordExportJob j = new WordExportJob();
    j.setId("job-1");
    j.setCreatorUserId(creatorUserId);
    j.setProjectId(projectId);
    j.setType(WordExportJobType.SUMMARY);
    j.setStatus(WordExportJobStatus.SUCCEEDED);
    return j;
  }
}
