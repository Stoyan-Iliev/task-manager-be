package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.git.domain.GitCommit;
import com.gradproject.taskmanager.modules.git.domain.GitCommitTask;
import com.gradproject.taskmanager.modules.git.domain.GitIntegration;
import com.gradproject.taskmanager.modules.git.domain.SmartCommitExecution;
import com.gradproject.taskmanager.modules.git.domain.enums.GitProvider;
import com.gradproject.taskmanager.modules.git.domain.enums.LinkMethod;
import com.gradproject.taskmanager.modules.git.domain.enums.SmartCommitCommandType;
import com.gradproject.taskmanager.modules.git.parser.SmartCommitParser;
import com.gradproject.taskmanager.modules.git.repository.GitCommitTaskRepository;
import com.gradproject.taskmanager.modules.git.repository.SmartCommitExecutionRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.task.domain.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class SmartCommitServiceImplTest {

    @Mock
    private SmartCommitParser smartCommitParser;

    @Mock
    private GitCommitTaskRepository commitTaskRepository;

    @Mock
    private SmartCommitExecutionRepository executionRepository;

    @InjectMocks
    private SmartCommitServiceImpl smartCommitService;

    private Organization organization;
    private Project project;
    private GitIntegration integration;
    private GitCommit commit;
    private Task task;
    private GitCommitTask commitTask;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setId(1L);

        project = new Project();
        project.setId(100L);
        project.setKey("PROJ");
        project.setOrganization(organization);

        integration = new GitIntegration();
        integration.setId(1L);
        integration.setOrganization(organization);
        integration.setProject(project);
        integration.setProvider(GitProvider.GITHUB);

        commit = new GitCommit();
        commit.setId(1L);
        commit.setCommitSha("abc123def456");
        commit.setMessage("PROJ-123 #close #comment Fixed the bug");
        commit.setGitIntegration(integration);

        task = new Task();
        task.setId(1L);
        task.setKey("PROJ-123");
        task.setProject(project);
        task.setOrganization(organization);

        commitTask = new GitCommitTask();
        commitTask.setId(1L);
        commitTask.setGitCommit(commit);
        commitTask.setTask(task);
        commitTask.setLinkMethod(LinkMethod.COMMIT_MESSAGE);
    }

    @Nested
    class ProcessCommit {

        @Test
        void shouldProcessCommitWithMultipleCommands() {
            
            SmartCommitParser.SmartCommitCommand closeCommand =
                new SmartCommitParser.SmartCommitCommand(SmartCommitCommandType.TRANSITION, "close", null, "#close");
            SmartCommitParser.SmartCommitCommand commentCommand =
                new SmartCommitParser.SmartCommitCommand(SmartCommitCommandType.COMMENT, "Fixed the bug", null, "#comment Fixed the bug");

            when(smartCommitParser.parseCommands(commit.getMessage()))
                .thenReturn(List.of(closeCommand, commentCommand));
            when(commitTaskRepository.findByGitCommitId(1L))
                .thenReturn(List.of(commitTask));

            SmartCommitExecution mockExecution = new SmartCommitExecution();
            mockExecution.setId(1L);
            when(executionRepository.save(any(SmartCommitExecution.class)))
                .thenReturn(mockExecution);

            
            SmartCommitService.SmartCommitExecutionSummary summary =
                smartCommitService.processCommit(commit);

            
            assertThat(summary).isNotNull();
            assertThat(summary.totalCommands()).isEqualTo(2);
            assertThat(summary.successfulExecutions()).isZero(); 
            assertThat(summary.failedExecutions()).isEqualTo(2);
            assertThat(summary.results()).hasSize(2);

            verify(smartCommitParser, atLeastOnce()).parseCommands(commit.getMessage());
            verify(commitTaskRepository).findByGitCommitId(1L);
            verify(executionRepository, times(2)).save(any(SmartCommitExecution.class));
        }

        @Test
        void shouldReturnEmptyWhenNoCommandsFound() {
            
            when(smartCommitParser.parseCommands(commit.getMessage()))
                .thenReturn(List.of());

            
            SmartCommitService.SmartCommitExecutionSummary summary =
                smartCommitService.processCommit(commit);

            
            assertThat(summary.totalCommands()).isZero();
            assertThat(summary.successfulExecutions()).isZero();
            assertThat(summary.failedExecutions()).isZero();
            assertThat(summary.results()).isEmpty();

            verify(commitTaskRepository, never()).findByGitCommitId(anyLong());
        }

        @Test
        void shouldReturnFailureWhenNoLinkedTasks() {
            
            SmartCommitParser.SmartCommitCommand command =
                new SmartCommitParser.SmartCommitCommand(SmartCommitCommandType.TRANSITION, "close", null, "#close");

            when(smartCommitParser.parseCommands(commit.getMessage()))
                .thenReturn(List.of(command));
            when(commitTaskRepository.findByGitCommitId(1L))
                .thenReturn(List.of());

            
            SmartCommitService.SmartCommitExecutionSummary summary =
                smartCommitService.processCommit(commit);

            
            assertThat(summary.totalCommands()).isEqualTo(1);
            assertThat(summary.successfulExecutions()).isZero();
            assertThat(summary.failedExecutions()).isEqualTo(1);

            verify(executionRepository, never()).save(any());
        }
    }

    @Nested
    class ExecuteCommands {

        @Test
        void shouldExecuteAllCommandsForTask() {
            
            SmartCommitParser.SmartCommitCommand command1 =
                new SmartCommitParser.SmartCommitCommand(SmartCommitCommandType.TRANSITION, "close", null, "#close");
            SmartCommitParser.SmartCommitCommand command2 =
                new SmartCommitParser.SmartCommitCommand(SmartCommitCommandType.TIME, "2h", null, "#time 2h");

            when(smartCommitParser.parseCommands(commit.getMessage()))
                .thenReturn(List.of(command1, command2));

            SmartCommitExecution mockExecution = new SmartCommitExecution();
            mockExecution.setId(1L);
            when(executionRepository.save(any(SmartCommitExecution.class)))
                .thenReturn(mockExecution);

            
            List<SmartCommitService.CommandExecutionResult> results =
                smartCommitService.executeCommands(commit, task);

            
            assertThat(results).hasSize(2);
            assertThat(results.get(0).commandType()).isEqualTo("TRANSITION");
            assertThat(results.get(1).commandType()).isEqualTo("TIME");

            ArgumentCaptor<SmartCommitExecution> captor =
                ArgumentCaptor.forClass(SmartCommitExecution.class);
            verify(executionRepository, times(2)).save(captor.capture());

            List<SmartCommitExecution> executions = captor.getAllValues();
            assertThat(executions.get(0).getCommandType()).isEqualTo(SmartCommitCommandType.TRANSITION);
            assertThat(executions.get(0).getCommandText()).isEqualTo("#close");
            assertThat(executions.get(0).getGitCommit()).isEqualTo(commit);
            assertThat(executions.get(0).getTask()).isEqualTo(task);
        }

        @Test
        void shouldHandleEmptyCommands() {
            
            when(smartCommitParser.parseCommands(commit.getMessage()))
                .thenReturn(List.of());

            
            List<SmartCommitService.CommandExecutionResult> results =
                smartCommitService.executeCommands(commit, task);

            
            assertThat(results).isEmpty();
            verify(executionRepository, never()).save(any());
        }
    }

    @Nested
    class ExecuteCommand {

        @Test
        void shouldReturnFailureForTransitionCommand() {
            
            SmartCommitService.CommandExecutionResult result =
                smartCommitService.executeCommand("TRANSITION", "close", task, null);

            
            assertThat(result.success()).isFalse();
            assertThat(result.commandType()).isEqualTo("TRANSITION");
            assertThat(result.errorMessage()).contains("not yet implemented");
        }

        @Test
        void shouldReturnFailureForCommentCommand() {
            
            SmartCommitService.CommandExecutionResult result =
                smartCommitService.executeCommand("COMMENT", "Fixed the bug", task, null);

            
            assertThat(result.success()).isFalse();
            assertThat(result.commandType()).isEqualTo("COMMENT");
            assertThat(result.errorMessage()).contains("not yet implemented");
        }

        @Test
        void shouldReturnFailureForTimeCommand() {
            
            SmartCommitService.CommandExecutionResult result =
                smartCommitService.executeCommand("TIME", "2h", task, null);

            
            assertThat(result.success()).isFalse();
            assertThat(result.commandType()).isEqualTo("TIME");
            assertThat(result.errorMessage()).contains("not yet implemented");
        }

        @Test
        void shouldReturnFailureForAssignCommand() {
            
            SmartCommitService.CommandExecutionResult result =
                smartCommitService.executeCommand("ASSIGN", "@john", task, null);

            
            assertThat(result.success()).isFalse();
            assertThat(result.commandType()).isEqualTo("ASSIGN");
            assertThat(result.errorMessage()).contains("not yet implemented");
        }

        @Test
        void shouldReturnFailureForLabelCommand() {
            
            SmartCommitService.CommandExecutionResult result =
                smartCommitService.executeCommand("LABEL", "bug", task, null);

            
            assertThat(result.success()).isFalse();
            assertThat(result.commandType()).isEqualTo("LABEL");
            assertThat(result.errorMessage()).contains("not yet implemented");
        }

        @Test
        void shouldReturnFailureForUnknownCommandType() {
            
            SmartCommitService.CommandExecutionResult result =
                smartCommitService.executeCommand("INVALID", "value", task, null);

            
            assertThat(result.success()).isFalse();
            assertThat(result.commandType()).isEqualTo("INVALID");
            assertThat(result.errorMessage()).contains("Unknown command type");
        }

        @Test
        void shouldHandleExecutionException() {
            
            
            SmartCommitService.CommandExecutionResult result =
                smartCommitService.executeCommand("INVALID_TYPE", "value", task, null);

            
            assertThat(result.success()).isFalse();
            assertThat(result.errorMessage()).contains("Unknown command type");
        }
    }

    @Nested
    class CommandExecution {

        @Test
        void shouldCreateExecutionRecordWithFailureStatus() {
            
            SmartCommitParser.SmartCommitCommand command =
                new SmartCommitParser.SmartCommitCommand(
                    SmartCommitCommandType.TRANSITION,
                    "close",
                    null,
                    "#close"
                );

            when(smartCommitParser.parseCommands(commit.getMessage()))
                .thenReturn(List.of(command));

            SmartCommitExecution mockExecution = new SmartCommitExecution();
            mockExecution.setId(1L);
            when(executionRepository.save(any(SmartCommitExecution.class)))
                .thenReturn(mockExecution);

            
            List<SmartCommitService.CommandExecutionResult> results =
                smartCommitService.executeCommands(commit, task);

            
            ArgumentCaptor<SmartCommitExecution> captor =
                ArgumentCaptor.forClass(SmartCommitExecution.class);
            verify(executionRepository).save(captor.capture());

            SmartCommitExecution execution = captor.getValue();
            assertThat(execution.getExecuted()).isFalse();
            assertThat(execution.getExecutionError()).isNotNull();
            assertThat(execution.getExecutionError()).contains("not yet implemented");
        }

        @Test
        void shouldStoreOriginalCommandText() {
            
            SmartCommitParser.SmartCommitCommand command =
                new SmartCommitParser.SmartCommitCommand(
                    SmartCommitCommandType.COMMENT,
                    "This is my comment",
                    null,
                    "#comment This is my comment"
                );

            when(smartCommitParser.parseCommands(commit.getMessage()))
                .thenReturn(List.of(command));

            SmartCommitExecution mockExecution = new SmartCommitExecution();
            when(executionRepository.save(any(SmartCommitExecution.class)))
                .thenReturn(mockExecution);

            
            smartCommitService.executeCommands(commit, task);

            
            ArgumentCaptor<SmartCommitExecution> captor =
                ArgumentCaptor.forClass(SmartCommitExecution.class);
            verify(executionRepository).save(captor.capture());

            SmartCommitExecution execution = captor.getValue();
            assertThat(execution.getCommandText()).isEqualTo("#comment This is my comment");
            assertThat(execution.getCommandType()).isEqualTo(SmartCommitCommandType.COMMENT);
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void shouldHandleMixedCaseCommandTypes() {
            
            SmartCommitService.CommandExecutionResult result1 =
                smartCommitService.executeCommand("transition", "close", task, null);
            SmartCommitService.CommandExecutionResult result2 =
                smartCommitService.executeCommand("TRANSITION", "close", task, null);
            SmartCommitService.CommandExecutionResult result3 =
                smartCommitService.executeCommand("TrAnSiTiOn", "close", task, null);

            
            assertThat(result1.commandType()).isEqualTo("TRANSITION");
            assertThat(result2.commandType()).isEqualTo("TRANSITION");
            assertThat(result3.commandType()).isEqualTo("TRANSITION");
        }

        @Test
        void shouldHandleEmptyCommandValue() {
            
            SmartCommitService.CommandExecutionResult result =
                smartCommitService.executeCommand("COMMENT", "", task, null);

            
            assertThat(result).isNotNull();
            assertThat(result.success()).isFalse();
        }

        @Test
        void shouldHandleNullCommandValue() {
            
            SmartCommitService.CommandExecutionResult result =
                smartCommitService.executeCommand("COMMENT", null, task, null);

            
            assertThat(result).isNotNull();
            assertThat(result.success()).isFalse();
        }

        @Test
        void shouldHandleMultipleTasksWithSameCommands() {
            
            Task task2 = new Task();
            task2.setId(2L);
            task2.setKey("PROJ-456");

            GitCommitTask commitTask2 = new GitCommitTask();
            commitTask2.setGitCommit(commit);
            commitTask2.setTask(task2);

            SmartCommitParser.SmartCommitCommand command =
                new SmartCommitParser.SmartCommitCommand(
                    SmartCommitCommandType.TRANSITION,
                    "close",
                    null,
                    "#close"
                );

            when(smartCommitParser.parseCommands(commit.getMessage()))
                .thenReturn(List.of(command));
            when(commitTaskRepository.findByGitCommitId(1L))
                .thenReturn(List.of(commitTask, commitTask2));

            SmartCommitExecution mockExecution = new SmartCommitExecution();
            mockExecution.setId(1L);
            when(executionRepository.save(any(SmartCommitExecution.class)))
                .thenReturn(mockExecution);

            
            SmartCommitService.SmartCommitExecutionSummary summary =
                smartCommitService.processCommit(commit);

            
            assertThat(summary.totalCommands()).isEqualTo(1);
            assertThat(summary.results()).hasSize(2); 
            verify(executionRepository, times(2)).save(any(SmartCommitExecution.class));
        }
    }
}
