package com.gradproject.taskmanager.modules.task.service;

import com.gradproject.taskmanager.modules.activity.service.ActivityLogService;
import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.domain.WorkLog;
import com.gradproject.taskmanager.modules.task.domain.WorkLogSource;
import com.gradproject.taskmanager.modules.task.dto.WorkLogRequest;
import com.gradproject.taskmanager.modules.task.dto.WorkLogResponse;
import com.gradproject.taskmanager.modules.task.dto.WorkLogSummary;
import com.gradproject.taskmanager.modules.task.repository.TaskRepository;
import com.gradproject.taskmanager.modules.task.repository.WorkLogRepository;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import com.gradproject.taskmanager.shared.exception.UnauthorizedException;
import com.gradproject.taskmanager.shared.security.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class WorkLogServiceTest {

    @Mock
    private WorkLogRepository workLogRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionService permissionService;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private WorkLogService workLogService;

    private User user;
    private Organization organization;
    private Project project;
    private Task task;
    private WorkLog workLog;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");

        organization = new Organization();
        organization.setId(1L);

        project = new Project();
        project.setId(1L);
        project.setOrganization(organization);

        task = Task.builder()
                .id(1L)
                .key("TEST-1")
                .title("Test Task")
                .organization(organization)
                .project(project)
                .loggedHours(BigDecimal.ZERO)
                .build();

        workLog = WorkLog.builder()
                .id(1L)
                .task(task)
                .author(user)
                .timeSpentMinutes(120)
                .workDate(LocalDate.now())
                .description("Did some work")
                .source(WorkLogSource.MANUAL)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== Log Time Tests ====================

    @Test
    void logTime_shouldCreateWorkLog() {
        // Given
        WorkLogRequest request = new WorkLogRequest(120, LocalDate.now(), "Did some work");

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(workLogRepository.save(any(WorkLog.class))).thenAnswer(inv -> {
            WorkLog wl = inv.getArgument(0);
            wl.setId(1L);
            wl.setCreatedAt(LocalDateTime.now());
            return wl;
        });
        when(workLogRepository.getTotalTimeSpentForTask(1L)).thenReturn(120);

        // When
        WorkLogResponse response = workLogService.logTime(1L, request, 1);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.taskId()).isEqualTo(1L);
        assertThat(response.timeSpentMinutes()).isEqualTo(120);
        assertThat(response.source()).isEqualTo(WorkLogSource.MANUAL);

        verify(workLogRepository).save(any(WorkLog.class));
        verify(activityLogService).logWorkLogged(eq(task), any(WorkLog.class), eq(user));
    }

    @Test
    void logTime_shouldThrowWhenTaskNotFound() {
        // Given
        WorkLogRequest request = new WorkLogRequest(60, null, null);
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> workLogService.logTime(999L, request, 1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task");
    }

    @Test
    void logTime_shouldThrowWhenUserNotFound() {
        // Given
        WorkLogRequest request = new WorkLogRequest(60, null, null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> workLogService.logTime(1L, request, 999))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    void logTime_shouldThrowWhenNoAccess() {
        // Given
        WorkLogRequest request = new WorkLogRequest(60, null, null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> workLogService.logTime(1L, request, 1))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void logTimeFromSmartCommit_shouldUseSmartCommitSource() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(workLogRepository.save(any(WorkLog.class))).thenAnswer(inv -> {
            WorkLog wl = inv.getArgument(0);
            wl.setId(1L);
            wl.setCreatedAt(LocalDateTime.now());
            return wl;
        });
        when(workLogRepository.getTotalTimeSpentForTask(1L)).thenReturn(150);

        // When
        WorkLogResponse response = workLogService.logTimeFromSmartCommit(1L, 150, "2h 30m", 1);

        // Then
        assertThat(response.source()).isEqualTo(WorkLogSource.SMART_COMMIT);
        assertThat(response.timeSpentMinutes()).isEqualTo(150);

        ArgumentCaptor<WorkLog> captor = ArgumentCaptor.forClass(WorkLog.class);
        verify(workLogRepository).save(captor.capture());
        assertThat(captor.getValue().getSource()).isEqualTo(WorkLogSource.SMART_COMMIT);
    }

    // ==================== Update Tests ====================

    @Test
    void updateWorkLog_shouldUpdateSuccessfully() {
        // Given
        WorkLogRequest request = new WorkLogRequest(180, LocalDate.now(), "Updated work");

        when(workLogRepository.findById(1L)).thenReturn(Optional.of(workLog));
        when(workLogRepository.save(any(WorkLog.class))).thenReturn(workLog);
        when(workLogRepository.getTotalTimeSpentForTask(1L)).thenReturn(180);

        // When
        WorkLogResponse response = workLogService.updateWorkLog(1L, request, 1);

        // Then
        assertThat(response).isNotNull();
        verify(workLogRepository).save(any(WorkLog.class));
    }

    @Test
    void updateWorkLog_shouldThrowWhenNotOwner() {
        // Given
        WorkLogRequest request = new WorkLogRequest(180, null, null);
        when(workLogRepository.findById(1L)).thenReturn(Optional.of(workLog));

        // When/Then (different user ID)
        assertThatThrownBy(() -> workLogService.updateWorkLog(1L, request, 999))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("your own");
    }

    // ==================== Delete Tests ====================

    @Test
    void deleteWorkLog_shouldDeleteSuccessfully() {
        // Given
        when(workLogRepository.findById(1L)).thenReturn(Optional.of(workLog));
        when(workLogRepository.getTotalTimeSpentForTask(1L)).thenReturn(0);

        // When
        workLogService.deleteWorkLog(1L, 1);

        // Then
        verify(activityLogService).logWorkLogDeleted(task, workLog, user);
        verify(workLogRepository).delete(workLog);
    }

    @Test
    void deleteWorkLog_shouldThrowWhenNotOwner() {
        // Given
        when(workLogRepository.findById(1L)).thenReturn(Optional.of(workLog));

        // When/Then
        assertThatThrownBy(() -> workLogService.deleteWorkLog(1L, 999))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ==================== Get Tests ====================

    @Test
    void getTaskWorkLogs_shouldReturnWorkLogs() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(workLogRepository.findByTaskIdOrderByWorkDateDescCreatedAtDesc(1L))
                .thenReturn(List.of(workLog));

        // When
        List<WorkLogResponse> responses = workLogService.getTaskWorkLogs(1L, 1);

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).timeSpentMinutes()).isEqualTo(120);
    }

    @Test
    void getTaskWorkLogSummary_shouldReturnSummary() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(workLogRepository.getTotalTimeSpentForTask(1L)).thenReturn(150);
        when(workLogRepository.countByTaskId(1L)).thenReturn(3L);

        // When
        WorkLogSummary summary = workLogService.getTaskWorkLogSummary(1L, 1);

        // Then
        assertThat(summary.taskId()).isEqualTo(1L);
        assertThat(summary.totalTimeSpentMinutes()).isEqualTo(150);
        assertThat(summary.totalTimeSpentFormatted()).isEqualTo("2h 30m");
        assertThat(summary.logCount()).isEqualTo(3);
    }

    @Test
    void getMyWorkLogs_shouldReturnUserWorkLogs() {
        // Given
        when(workLogRepository.findByAuthorIdOrderByWorkDateDescCreatedAtDesc(1))
                .thenReturn(List.of(workLog));

        // When
        List<WorkLogResponse> responses = workLogService.getMyWorkLogs(1);

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).taskKey()).isEqualTo("TEST-1");
    }

    // ==================== Time Formatting Tests ====================

    @Test
    void logTime_shouldFormatTimeCorrectly() {
        // Given - 2 hours 30 minutes
        WorkLogRequest request = new WorkLogRequest(150, LocalDate.now(), null);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(workLogRepository.save(any(WorkLog.class))).thenAnswer(inv -> {
            WorkLog wl = inv.getArgument(0);
            wl.setId(1L);
            wl.setCreatedAt(LocalDateTime.now());
            return wl;
        });
        when(workLogRepository.getTotalTimeSpentForTask(1L)).thenReturn(150);

        // When
        WorkLogResponse response = workLogService.logTime(1L, request, 1);

        // Then
        assertThat(response.timeSpentFormatted()).isEqualTo("2h 30m");
    }

    @Test
    void logTime_shouldUpdateTaskLoggedHours() {
        // Given
        WorkLogRequest request = new WorkLogRequest(120, LocalDate.now(), null);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(workLogRepository.save(any(WorkLog.class))).thenAnswer(inv -> {
            WorkLog wl = inv.getArgument(0);
            wl.setId(1L);
            wl.setCreatedAt(LocalDateTime.now());
            return wl;
        });
        when(workLogRepository.getTotalTimeSpentForTask(1L)).thenReturn(120);

        // When
        workLogService.logTime(1L, request, 1);

        // Then
        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getLoggedHours()).isEqualByComparingTo(new BigDecimal("2.00"));
    }
}
