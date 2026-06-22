package com.reportweb.controller;

import com.reportweb.entity.Project;
import com.reportweb.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectsControllerListVisibilityTest {

  private static final String OWNER_ID = "owner-1";
  private static final String REVIEWER_ID = "reviewer-1";

  @Mock
  private ProjectRepository projectRepository;

  private ProjectsController controller;

  @BeforeEach
  void setUp() throws Exception {
    controller = new ProjectsController(
        projectRepository, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null);
  }

  @Test
  void listVisibleProjectsForUser_nonOwnerReviewer_returnsOnlyOwnedProjects() throws Exception {
    when(projectRepository.findByUserIdOrderByCreatedAtDesc(REVIEWER_ID)).thenReturn(List.of());

    List<Project> visible = invokeListVisibleProjectsForUser(false, false, REVIEWER_ID, REVIEWER_ID);

    assertTrue(visible.isEmpty());
    verify(projectRepository).findByUserIdOrderByCreatedAtDesc(REVIEWER_ID);
    verifyNoMoreInteractions(projectRepository);
  }

  @Test
  void listVisibleProjectsForUser_owner_returnsOwnedProjects() throws Exception {
    Project owned = new Project();
    owned.setId(10);
    owned.setUserId(OWNER_ID);
    owned.setProjectName("归属项目");
    when(projectRepository.findByUserIdOrderByCreatedAtDesc(OWNER_ID)).thenReturn(List.of(owned));

    List<Project> visible = invokeListVisibleProjectsForUser(false, false, OWNER_ID, OWNER_ID);

    assertEquals(1, visible.size());
    assertEquals(10, visible.get(0).getId());
    verify(projectRepository).findByUserIdOrderByCreatedAtDesc(OWNER_ID);
    verifyNoMoreInteractions(projectRepository);
  }

  @Test
  void listVisibleProjectsForUser_subUser_returnsOnlyInProgressOwnedProjects() throws Exception {
    Project inProgress = new Project();
    inProgress.setId(20);
    inProgress.setUserId(OWNER_ID);
    inProgress.setStatus("InProgress");
    when(projectRepository.findByUserIdAndStatusOrderByCreatedAtDesc(OWNER_ID, "InProgress"))
        .thenReturn(List.of(inProgress));

    List<Project> visible = invokeListVisibleProjectsForUser(false, true, "sub-1", OWNER_ID);

    assertEquals(1, visible.size());
    assertEquals(20, visible.get(0).getId());
    verify(projectRepository).findByUserIdAndStatusOrderByCreatedAtDesc(OWNER_ID, "InProgress");
    verifyNoMoreInteractions(projectRepository);
  }

  @SuppressWarnings("unchecked")
  private List<Project> invokeListVisibleProjectsForUser(
      boolean isAdmin, boolean isSubUser, String userId, String effectiveUserId) throws Exception {
    Method method = ProjectsController.class.getDeclaredMethod(
        "listVisibleProjectsForUser", boolean.class, boolean.class, String.class, String.class);
    method.setAccessible(true);
    return (List<Project>) method.invoke(controller, isAdmin, isSubUser, userId, effectiveUserId);
  }
}
