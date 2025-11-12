package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.git.domain.*;
import com.gradproject.taskmanager.modules.git.domain.enums.GitProvider;
import com.gradproject.taskmanager.modules.git.domain.enums.LinkMethod;
import com.gradproject.taskmanager.modules.git.parser.BranchNameParser;
import com.gradproject.taskmanager.modules.git.parser.IssueReferenceParser;
import com.gradproject.taskmanager.modules.git.repository.GitBranchRepository;
import com.gradproject.taskmanager.modules.git.repository.GitCommitTaskRepository;
import com.gradproject.taskmanager.modules.git.repository.GitPrTaskRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.repository.ProjectRepository;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class GitLinkingServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private GitBranchRepository branchRepository;

    @Mock
    private GitCommitTaskRepository commitTaskRepository;

    @Mock
    private GitPrTaskRepository prTaskRepository;

    @Mock
    private IssueReferenceParser issueReferenceParser;

    @Mock
    private BranchNameParser branchNameParser;

    @InjectMocks
    private GitLinkingServiceImpl linkingService;

    private Organization organization;
    private Project project;
    private GitIntegration integration;
    private Task task;

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
        integration.setRepositoryFullName("owner/repo");

        task = new Task();
        task.setId(1L);
        task.setKey("PROJ-123");
        task.setProject(project);
        task.setOrganization(organization);
    }

    @Nested
    class LinkBranchToTask {

        @Test
        void shouldLinkBranchSuccessfully() {
            
            GitBranch branch = new GitBranch();
            branch.setId(1L);
            branch.setBranchName("feature/PROJ-123-add-login");
            branch.setGitIntegration(integration);

            when(branchNameParser.extractTaskReference("feature/PROJ-123-add-login"))
                .thenReturn("PROJ-123");
            when(taskRepository.findByOrganizationIdAndKey(1L, "PROJ-123"))
                .thenReturn(Optional.of(task));
            when(branchRepository.save(branch)).thenReturn(branch);

            
            Task result = linkingService.linkBranchToTask(branch);

            
            assertThat(result).isEqualTo(task);
            assertThat(branch.getTask()).isEqualTo(task);
            verify(branchRepository).save(branch);
        }

        @Test
        void shouldReturnNullWhenNoTaskReferenceFound() {
            
            GitBranch branch = new GitBranch();
            branch.setBranchName("feature/add-login");
            branch.setGitIntegration(integration);

            when(branchNameParser.extractTaskReference("feature/add-login"))
                .thenReturn(null);

            
            Task result = linkingService.linkBranchToTask(branch);

            
            assertThat(result).isNull();
            verify(branchRepository, never()).save(any());
        }

        @Test
        void shouldReturnNullWhenTaskNotFound() {
            
            GitBranch branch = new GitBranch();
            branch.setBranchName("feature/PROJ-999-nonexistent");
            branch.setGitIntegration(integration);

            when(branchNameParser.extractTaskReference("feature/PROJ-999-nonexistent"))
                .thenReturn("PROJ-999");
            when(taskRepository.findByOrganizationIdAndKey(1L, "PROJ-999"))
                .thenReturn(Optional.empty());

            
            Task result = linkingService.linkBranchToTask(branch);

            
            assertThat(result).isNull();
            verify(branchRepository, never()).save(any());
        }
    }

    @Nested
    class LinkCommitToTasks {

        @Test
        void shouldLinkCommitToMultipleTasks() {
            
            GitCommit commit = new GitCommit();
            commit.setId(1L);
            commit.setCommitSha("abc123");
            commit.setMessage("Fix PROJ-123 and PROJ-456");
            commit.setGitIntegration(integration);

            Task task2 = new Task();
            task2.setId(2L);
            task2.setKey("PROJ-456");

            when(issueReferenceParser.extractReferences("Fix PROJ-123 and PROJ-456"))
                .thenReturn(List.of("PROJ-123", "PROJ-456"));
            when(taskRepository.findByOrganizationIdAndKey(1L, "PROJ-123"))
                .thenReturn(Optional.of(task));
            when(taskRepository.findByOrganizationIdAndKey(1L, "PROJ-456"))
                .thenReturn(Optional.of(task2));
            when(commitTaskRepository.existsByGitCommitIdAndTaskId(anyLong(), anyLong()))
                .thenReturn(false);

            
            List<Task> result = linkingService.linkCommitToTasks(commit);

            
            assertThat(result).hasSize(2);
            assertThat(result).contains(task, task2);

            ArgumentCaptor<GitCommitTask> captor = ArgumentCaptor.forClass(GitCommitTask.class);
            verify(commitTaskRepository, times(2)).save(captor.capture());

            List<GitCommitTask> savedLinks = captor.getAllValues();
            assertThat(savedLinks.get(0).getLinkMethod()).isEqualTo(LinkMethod.COMMIT_MESSAGE);
            assertThat(savedLinks.get(1).getLinkMethod()).isEqualTo(LinkMethod.COMMIT_MESSAGE);
        }

        @Test
        void shouldSkipExistingLinks() {
            
            GitCommit commit = new GitCommit();
            commit.setId(1L);
            commit.setCommitSha("abc123");
            commit.setMessage("Fix PROJ-123");
            commit.setGitIntegration(integration);

            when(issueReferenceParser.extractReferences("Fix PROJ-123"))
                .thenReturn(List.of("PROJ-123"));
            when(taskRepository.findByOrganizationIdAndKey(1L, "PROJ-123"))
                .thenReturn(Optional.of(task));
            when(commitTaskRepository.existsByGitCommitIdAndTaskId(1L, 1L))
                .thenReturn(true);

            
            List<Task> result = linkingService.linkCommitToTasks(commit);

            
            assertThat(result).hasSize(1);
            assertThat(result).contains(task);
            verify(commitTaskRepository, never()).save(any());
        }

        @Test
        void shouldReturnEmptyListWhenNoTaskReferences() {
            
            GitCommit commit = new GitCommit();
            commit.setMessage("Regular commit without task references");
            commit.setGitIntegration(integration);

            when(issueReferenceParser.extractReferences(anyString()))
                .thenReturn(List.of());

            
            List<Task> result = linkingService.linkCommitToTasks(commit);

            
            assertThat(result).isEmpty();
            verify(commitTaskRepository, never()).save(any());
        }
    }

    @Nested
    class LinkPullRequestToTasks {

        @Test
        void shouldLinkPullRequestFromTitle() {
            
            GitPullRequest pr = new GitPullRequest();
            pr.setId(1L);
            pr.setPrNumber(42);
            pr.setPrTitle("Add PROJ-123: Add login feature");  
            pr.setPrDescription("This PR adds login functionality");
            pr.setGitIntegration(integration);

            when(issueReferenceParser.extractReferences("Add PROJ-123: Add login feature"))
                .thenReturn(List.of("PROJ-123"));
            when(issueReferenceParser.extractReferences("This PR adds login functionality"))
                .thenReturn(List.of());
            when(taskRepository.findByOrganizationIdAndKey(1L, "PROJ-123"))
                .thenReturn(Optional.of(task));
            when(prTaskRepository.existsByGitPullRequestIdAndTaskId(1L, 1L))
                .thenReturn(false);

            
            List<Task> result = linkingService.linkPullRequestToTasks(pr);

            
            assertThat(result).hasSize(1);
            assertThat(result).contains(task);

            ArgumentCaptor<GitPrTask> captor = ArgumentCaptor.forClass(GitPrTask.class);
            verify(prTaskRepository).save(captor.capture());

            GitPrTask savedLink = captor.getValue();
            assertThat(savedLink.getLinkMethod()).isEqualTo(LinkMethod.PR_TITLE);
            assertThat(savedLink.getClosesTask()).isFalse();
        }

        @Test
        void shouldLinkPullRequestFromDescription() {
            
            GitPullRequest pr = new GitPullRequest();
            pr.setId(1L);
            pr.setPrNumber(42);
            pr.setPrTitle("Add login feature");
            pr.setPrDescription("This fixes PROJ-123 by adding authentication");
            pr.setGitIntegration(integration);

            when(issueReferenceParser.extractReferences("Add login feature"))
                .thenReturn(List.of());
            when(issueReferenceParser.extractReferences("This fixes PROJ-123 by adding authentication"))
                .thenReturn(List.of("PROJ-123"));
            when(taskRepository.findByOrganizationIdAndKey(1L, "PROJ-123"))
                .thenReturn(Optional.of(task));
            when(prTaskRepository.existsByGitPullRequestIdAndTaskId(1L, 1L))
                .thenReturn(false);

            
            List<Task> result = linkingService.linkPullRequestToTasks(pr);

            
            assertThat(result).hasSize(1);

            ArgumentCaptor<GitPrTask> captor = ArgumentCaptor.forClass(GitPrTask.class);
            verify(prTaskRepository).save(captor.capture());

            GitPrTask savedLink = captor.getValue();
            assertThat(savedLink.getLinkMethod()).isEqualTo(LinkMethod.PR_DESCRIPTION);
            assertThat(savedLink.getClosesTask()).isTrue(); 
        }

        @Test
        void shouldSetClosesTaskWhenClosingKeywordDetected() {
            
            GitPullRequest pr = new GitPullRequest();
            pr.setId(1L);
            pr.setPrNumber(42);
            pr.setPrTitle("Closes PROJ-123");
            pr.setPrDescription(null);
            pr.setGitIntegration(integration);

            when(issueReferenceParser.extractReferences("Closes PROJ-123"))
                .thenReturn(List.of("PROJ-123"));
            when(taskRepository.findByOrganizationIdAndKey(1L, "PROJ-123"))
                .thenReturn(Optional.of(task));
            when(prTaskRepository.existsByGitPullRequestIdAndTaskId(1L, 1L))
                .thenReturn(false);

            
            List<Task> result = linkingService.linkPullRequestToTasks(pr);

            
            ArgumentCaptor<GitPrTask> captor = ArgumentCaptor.forClass(GitPrTask.class);
            verify(prTaskRepository).save(captor.capture());

            GitPrTask savedLink = captor.getValue();
            assertThat(savedLink.getClosesTask()).isTrue();
        }

        @Test
        void shouldHandleNullDescription() {
            
            GitPullRequest pr = new GitPullRequest();
            pr.setPrTitle("Some PR");
            pr.setPrDescription(null);
            pr.setGitIntegration(integration);

            when(issueReferenceParser.extractReferences("Some PR"))
                .thenReturn(List.of());

            
            List<Task> result = linkingService.linkPullRequestToTasks(pr);

            
            assertThat(result).isEmpty();
        }

        @Test
        void shouldDeduplicateTaskReferencesFromTitleAndDescription() {
            
            GitPullRequest pr = new GitPullRequest();
            pr.setId(1L);
            pr.setPrTitle("Fix PROJ-123");
            pr.setPrDescription("Closes PROJ-123");
            pr.setGitIntegration(integration);

            when(issueReferenceParser.extractReferences("Fix PROJ-123"))
                .thenReturn(List.of("PROJ-123"));
            when(issueReferenceParser.extractReferences("Closes PROJ-123"))
                .thenReturn(List.of("PROJ-123"));
            when(taskRepository.findByOrganizationIdAndKey(1L, "PROJ-123"))
                .thenReturn(Optional.of(task));
            when(prTaskRepository.existsByGitPullRequestIdAndTaskId(1L, 1L))
                .thenReturn(false);

            
            List<Task> result = linkingService.linkPullRequestToTasks(pr);

            
            assertThat(result).hasSize(1);
            verify(prTaskRepository, times(1)).save(any(GitPrTask.class));
        }
    }

    @Nested
    class ShouldCloseTask {

        @Test
        void shouldDetectClosesKeyword() {
            assertThat(linkingService.shouldCloseTask("Closes PROJ-123", "PROJ-123")).isTrue();
            assertThat(linkingService.shouldCloseTask("closes PROJ-123", "PROJ-123")).isTrue();
            assertThat(linkingService.shouldCloseTask("Close PROJ-123", "PROJ-123")).isTrue();
            assertThat(linkingService.shouldCloseTask("Closed PROJ-123", "PROJ-123")).isTrue();
        }

        @Test
        void shouldDetectFixesKeyword() {
            assertThat(linkingService.shouldCloseTask("Fixes PROJ-123", "PROJ-123")).isTrue();
            assertThat(linkingService.shouldCloseTask("fixes PROJ-123", "PROJ-123")).isTrue();
            assertThat(linkingService.shouldCloseTask("Fix PROJ-123", "PROJ-123")).isTrue();
            assertThat(linkingService.shouldCloseTask("Fixed PROJ-123", "PROJ-123")).isTrue();
        }

        @Test
        void shouldDetectResolvesKeyword() {
            assertThat(linkingService.shouldCloseTask("Resolves PROJ-123", "PROJ-123")).isTrue();
            assertThat(linkingService.shouldCloseTask("resolves PROJ-123", "PROJ-123")).isTrue();
            assertThat(linkingService.shouldCloseTask("Resolve PROJ-123", "PROJ-123")).isTrue();
            assertThat(linkingService.shouldCloseTask("Resolved PROJ-123", "PROJ-123")).isTrue();
        }

        @Test
        void shouldNotDetectWhenKeywordNotPresent() {
            assertThat(linkingService.shouldCloseTask("Updates PROJ-123", "PROJ-123")).isFalse();
            assertThat(linkingService.shouldCloseTask("References PROJ-123", "PROJ-123")).isFalse();
            assertThat(linkingService.shouldCloseTask("See PROJ-123", "PROJ-123")).isFalse();
        }

        @Test
        void shouldNotDetectWhenTaskKeyDoesNotMatch() {
            assertThat(linkingService.shouldCloseTask("Closes PROJ-123", "PROJ-456")).isFalse();
        }

        @Test
        void shouldHandleNullOrEmptyText() {
            assertThat(linkingService.shouldCloseTask(null, "PROJ-123")).isFalse();
            assertThat(linkingService.shouldCloseTask("", "PROJ-123")).isFalse();
        }
    }

    @Nested
    class ExtractProjectKey {

        @Test
        void shouldExtractProjectKey() {
            assertThat(linkingService.extractProjectKey("PROJ-123")).isEqualTo("PROJ");
            assertThat(linkingService.extractProjectKey("ABC-1")).isEqualTo("ABC");
            assertThat(linkingService.extractProjectKey("LONGKEY-999")).isEqualTo("LONGKEY");
        }

        @Test
        void shouldReturnNullForInvalidFormat() {
            assertThat(linkingService.extractProjectKey(null)).isNull();
            assertThat(linkingService.extractProjectKey("PROJ123")).isNull();
            assertThat(linkingService.extractProjectKey("")).isNull();
        }
    }

    @Nested
    class ValidateTaskBelongsToProject {

        @Test
        void shouldReturnTrueWhenTaskBelongsToProject() {
            
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));

            
            boolean result = linkingService.validateTaskBelongsToProject("PROJ-123", 100L);

            
            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalseWhenProjectKeyDoesNotMatch() {
            
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));

            
            boolean result = linkingService.validateTaskBelongsToProject("OTHER-123", 100L);

            
            assertThat(result).isFalse();
        }

        @Test
        void shouldReturnFalseWhenProjectNotFound() {
            
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());

            
            boolean result = linkingService.validateTaskBelongsToProject("PROJ-123", 999L);

            
            assertThat(result).isFalse();
        }
    }

    @Nested
    class FindTaskByKey {

        @Test
        void shouldFindTaskSuccessfully() {
            
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
            when(taskRepository.findByOrganizationIdAndKey(1L, "PROJ-123"))
                .thenReturn(Optional.of(task));

            
            Task result = linkingService.findTaskByKey("PROJ-123", 100L);

            
            assertThat(result).isEqualTo(task);
        }

        @Test
        void shouldReturnNullWhenProjectNotFound() {
            
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());

            
            Task result = linkingService.findTaskByKey("PROJ-123", 999L);

            
            assertThat(result).isNull();
        }

        @Test
        void shouldReturnNullWhenTaskKeyDoesNotMatchProject() {
            
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));

            
            Task result = linkingService.findTaskByKey("OTHER-123", 100L);

            
            assertThat(result).isNull();
            verify(taskRepository, never()).findByOrganizationIdAndKey(anyLong(), anyString());
        }

        @Test
        void shouldReturnNullWhenTaskNotFound() {
            
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
            when(taskRepository.findByOrganizationIdAndKey(1L, "PROJ-999"))
                .thenReturn(Optional.empty());

            
            Task result = linkingService.findTaskByKey("PROJ-999", 100L);

            
            assertThat(result).isNull();
        }
    }
}
