package com.gradproject.taskmanager.modules.task.service;

import com.gradproject.taskmanager.modules.activity.domain.ActionType;
import com.gradproject.taskmanager.modules.activity.domain.EntityType;
import com.gradproject.taskmanager.modules.activity.service.ActivityLogService;
import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.notification.event.WatcherAddedEvent;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.domain.TaskWatcher;
import com.gradproject.taskmanager.modules.task.repository.TaskRepository;
import com.gradproject.taskmanager.modules.task.repository.TaskWatcherRepository;
import com.gradproject.taskmanager.shared.dto.PageResponse;
import com.gradproject.taskmanager.shared.dto.TaskSummary;
import com.gradproject.taskmanager.shared.dto.UserSummary;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import com.gradproject.taskmanager.shared.exception.UnauthorizedException;
import com.gradproject.taskmanager.shared.security.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class TaskWatcherServiceImplTest {

    @Mock
    private TaskWatcherRepository watcherRepo;

    @Mock
    private TaskRepository taskRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private PermissionService permissionService;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TaskWatcherServiceImpl watcherService;

    private Task task;
    private User user;
    private User addedByUser;
    private Organization organization;
    private Project project;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setId(1L);

        project = new Project();
        project.setId(1L);
        project.setOrganization(organization);

        
        com.gradproject.taskmanager.modules.project.domain.TaskStatus status =
            new com.gradproject.taskmanager.modules.project.domain.TaskStatus();
        status.setId(1L);
        status.setName("To Do");
        status.setProject(project);

        task = new Task();
        task.setId(1L);
        task.setKey("PROJ-123");
        task.setOrganization(organization);
        task.setProject(project);
        task.setStatus(status);
        task.setPriority(com.gradproject.taskmanager.modules.task.domain.TaskPriority.MEDIUM);

        user = new User();
        user.setId(1);
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        addedByUser = new User();
        addedByUser.setId(2);
        addedByUser.setUsername("admin");
        addedByUser.setEmail("admin@example.com");
    }

    

    @Test
    void addWatcher_success() {
        
        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(userRepo.findById(1)).thenReturn(Optional.of(user));
        when(userRepo.findById(2)).thenReturn(Optional.of(addedByUser));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(watcherRepo.existsByTaskIdAndUserId(1L, 1)).thenReturn(false);

        
        watcherService.addWatcher(1L, 1, 2);

        
        verify(watcherRepo).save(argThat(watcher ->
                watcher.getTask().equals(task) &&
                watcher.getUser().equals(user) &&
                watcher.getAddedBy().equals(2)
        ));
        verify(activityLogService).logActivityWithMetadata(
                eq(organization),
                eq(project),
                eq(task),
                eq(EntityType.TASK),
                eq(1L),
                eq(ActionType.WATCHER_ADDED),
                eq(addedByUser),
                any(Map.class)
        );
        verify(eventPublisher).publishEvent(any(WatcherAddedEvent.class));
    }

    @Test
    void addWatcher_taskNotFound_throwsException() {
        
        when(taskRepo.findById(1L)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> watcherService.addWatcher(1L, 1, 2))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task");
    }

    @Test
    void addWatcher_userNotFound_throwsException() {
        
        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(userRepo.findById(1)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> watcherService.addWatcher(1L, 1, 2))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    void addWatcher_userCannotAccessTask_throwsException() {
        
        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(userRepo.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(false);

        
        assertThatThrownBy(() -> watcherService.addWatcher(1L, 1, 2))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("cannot access");
    }

    @Test
    void addWatcher_alreadyWatching_idempotent() {
        
        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(userRepo.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(watcherRepo.existsByTaskIdAndUserId(1L, 1)).thenReturn(true);

        
        watcherService.addWatcher(1L, 1, 2);

        
        verify(watcherRepo, never()).save(any());
        verify(activityLogService, never()).logActivityWithMetadata(any(), any(), any(), any(), any(), any(), any(), any());
    }

    

    @Test
    void removeWatcher_selfRemoval_success() {
        
        when(watcherRepo.existsByTaskIdAndUserId(1L, 1)).thenReturn(true);
        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(userRepo.findById(1)).thenReturn(Optional.of(user));

        
        watcherService.removeWatcher(1L, 1, 1);

        
        verify(watcherRepo).deleteByTaskIdAndUserId(1L, 1);
        verify(activityLogService).logActivityWithMetadata(
                eq(organization),
                eq(project),
                eq(task),
                eq(EntityType.TASK),
                eq(1L),
                eq(ActionType.WATCHER_REMOVED),
                eq(user),
                any(Map.class)
        );
    }

    @Test
    void removeWatcher_adminRemovingOther_success() {
        
        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(userRepo.findById(2)).thenReturn(Optional.of(addedByUser));
        when(permissionService.canManageMembers(addedByUser, project)).thenReturn(true);
        when(watcherRepo.existsByTaskIdAndUserId(1L, 1)).thenReturn(true);
        when(userRepo.findById(1)).thenReturn(Optional.of(user));

        
        watcherService.removeWatcher(1L, 1, 2);

        
        verify(watcherRepo).deleteByTaskIdAndUserId(1L, 1);
    }

    @Test
    void removeWatcher_nonAdminRemovingOther_throwsException() {
        
        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(userRepo.findById(2)).thenReturn(Optional.of(addedByUser));
        when(permissionService.canManageMembers(addedByUser, project)).thenReturn(false);

        
        assertThatThrownBy(() -> watcherService.removeWatcher(1L, 1, 2))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Only admins");
    }

    @Test
    void removeWatcher_notWatching_idempotent() {
        
        when(watcherRepo.existsByTaskIdAndUserId(1L, 1)).thenReturn(false);

        
        watcherService.removeWatcher(1L, 1, 1);

        
        verify(watcherRepo, never()).deleteByTaskIdAndUserId(anyLong(), anyInt());
    }

    

    @Test
    void isWatching_returnsTrue() {
        
        when(watcherRepo.existsByTaskIdAndUserId(1L, 1)).thenReturn(true);

        
        boolean result = watcherService.isWatching(1L, 1);

        
        assertThat(result).isTrue();
    }

    @Test
    void isWatching_returnsFalse() {
        
        when(watcherRepo.existsByTaskIdAndUserId(1L, 1)).thenReturn(false);

        
        boolean result = watcherService.isWatching(1L, 1);

        
        assertThat(result).isFalse();
    }

    @Test
    void getTaskWatchers_success() {
        
        TaskWatcher watcher1 = TaskWatcher.builder()
                .task(task)
                .user(user)
                .addedBy(2)
                .addedAt(LocalDateTime.now())
                .build();

        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(userRepo.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(watcherRepo.findByTaskId(1L)).thenReturn(List.of(watcher1));

        
        List<UserSummary> result = watcherService.getTaskWatchers(1L, 1);

        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(user.getId());
        assertThat(result.get(0).username()).isEqualTo(user.getUsername());
    }

    @Test
    void getWatcherCount_returnsCorrectCount() {
        
        when(watcherRepo.countByTaskId(1L)).thenReturn(5);

        
        int count = watcherService.getWatcherCount(1L);

        
        assertThat(count).isEqualTo(5);
    }

    @Test
    void getWatchedTasks_returnsPaginatedResults() {
        
        TaskWatcher watcher = TaskWatcher.builder()
                .task(task)
                .user(user)
                .addedBy(1)
                .addedAt(LocalDateTime.now())
                .build();

        Page<TaskWatcher> page = new PageImpl<>(List.of(watcher), PageRequest.of(0, 10), 1);
        when(watcherRepo.findByUserIdOrderByAddedAtDesc(eq(1), any(Pageable.class))).thenReturn(page);

        
        PageResponse<TaskSummary> result = watcherService.getWatchedTasks(1, PageRequest.of(0, 10));

        
        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.pageNumber()).isEqualTo(0);
    }

    

    @Test
    void autoWatchOnCreate_addsWatcher() {
        
        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(userRepo.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(watcherRepo.existsByTaskIdAndUserId(1L, 1)).thenReturn(false);

        
        watcherService.autoWatchOnCreate(task, user);

        
        verify(watcherRepo).save(any(TaskWatcher.class));
    }

    @Test
    void autoWatchOnAssign_addsWatcher() {
        
        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(userRepo.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(watcherRepo.existsByTaskIdAndUserId(1L, 1)).thenReturn(false);

        
        watcherService.autoWatchOnAssign(task, user);

        
        verify(watcherRepo).save(any(TaskWatcher.class));
    }

    @Test
    void autoWatchOnAssign_nullAssignee_doesNothing() {
        
        watcherService.autoWatchOnAssign(task, null);

        
        verify(watcherRepo, never()).save(any());
    }

    @Test
    void autoWatchOnComment_addsWatcher() {
        
        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(userRepo.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(watcherRepo.existsByTaskIdAndUserId(1L, 1)).thenReturn(false);

        
        watcherService.autoWatchOnComment(task, user);

        
        verify(watcherRepo).save(any(TaskWatcher.class));
    }

    @Test
    void autoWatchOnMention_addsWatchersForValidUsers() {
        
        Set<String> mentions = Set.of("testuser", "nonexistent");
        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepo.findByUsername("nonexistent")).thenReturn(Optional.empty());
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(userRepo.findById(1)).thenReturn(Optional.of(user));
        when(watcherRepo.existsByTaskIdAndUserId(1L, 1)).thenReturn(false);

        
        User reporter = new User();
        reporter.setId(3);
        task.setReporter(reporter);
        when(userRepo.findById(3)).thenReturn(Optional.of(reporter));

        
        watcherService.autoWatchOnMention(task, mentions);

        
        verify(watcherRepo, times(1)).save(any(TaskWatcher.class));
    }

    

    @Test
    void extractMentions_findsMultipleMentions() {
        
        String content = "Hey @john and @jane, please review this @mike";

        
        List<String> mentions = watcherService.extractMentions(content);

        
        assertThat(mentions).containsExactlyInAnyOrder("john", "jane", "mike");
    }

    @Test
    void extractMentions_handlesDuplicates() {
        
        String content = "@john mentioned @john again";

        
        List<String> mentions = watcherService.extractMentions(content);

        
        assertThat(mentions).containsExactly("john");
    }

    @Test
    void extractMentions_emptyContent_returnsEmpty() {
        
        List<String> mentions = watcherService.extractMentions("");

        
        assertThat(mentions).isEmpty();
    }

    @Test
    void extractMentions_nullContent_returnsEmpty() {
        
        List<String> mentions = watcherService.extractMentions(null);

        
        assertThat(mentions).isEmpty();
    }

    @Test
    void extractMentions_handlesSpecialCharacters() {
        
        String content = "@user-name @user_123 @CamelCase";

        
        List<String> mentions = watcherService.extractMentions(content);

        
        assertThat(mentions).containsExactlyInAnyOrder("user-name", "user_123", "CamelCase");
    }

    

    @Test
    void addWatchers_addsMultipleWatchers() {
        
        List<Integer> userIds = List.of(1, 2, 3);
        when(taskRepo.findById(anyLong())).thenReturn(Optional.of(task));
        when(userRepo.findById(anyInt())).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(any(), any())).thenReturn(true);
        when(watcherRepo.existsByTaskIdAndUserId(anyLong(), anyInt())).thenReturn(false);

        
        watcherService.addWatchers(1L, userIds, 1);

        
        verify(watcherRepo, times(3)).save(any(TaskWatcher.class));
    }

    @Test
    void addWatchers_continuesOnError() {
        
        List<Integer> userIds = List.of(1, 2);
        when(taskRepo.findById(1L))
                .thenReturn(Optional.of(task))
                .thenThrow(new ResourceNotFoundException("Task", 1L));

        
        watcherService.addWatchers(1L, userIds, 1);

        
        verify(taskRepo, times(2)).findById(1L);
    }

    @Test
    void removeWatchers_removesMultipleWatchers() {
        
        List<Integer> userIds = List.of(1, 2, 3);

        
        when(watcherRepo.existsByTaskIdAndUserId(eq(1L), eq(1))).thenReturn(true);
        when(watcherRepo.existsByTaskIdAndUserId(eq(1L), eq(2))).thenReturn(true);
        when(watcherRepo.existsByTaskIdAndUserId(eq(1L), eq(3))).thenReturn(true);

        
        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));

        
        User user1 = new User();
        user1.setId(1);
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");

        User user2 = new User();
        user2.setId(2);
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");

        User user3 = new User();
        user3.setId(3);
        user3.setUsername("user3");
        user3.setEmail("user3@example.com");

        when(userRepo.findById(1)).thenReturn(Optional.of(user1));
        when(userRepo.findById(2)).thenReturn(Optional.of(user2));
        when(userRepo.findById(3)).thenReturn(Optional.of(user3));

        
        watcherService.removeWatcher(1L, 1, 1);
        watcherService.removeWatcher(1L, 2, 2);
        watcherService.removeWatcher(1L, 3, 3);

        
        verify(watcherRepo).deleteByTaskIdAndUserId(1L, 1);
        verify(watcherRepo).deleteByTaskIdAndUserId(1L, 2);
        verify(watcherRepo).deleteByTaskIdAndUserId(1L, 3);
    }
}
