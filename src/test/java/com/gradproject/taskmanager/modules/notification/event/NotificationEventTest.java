package com.gradproject.taskmanager.modules.notification.event;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.domain.TaskStatus;
import com.gradproject.taskmanager.modules.task.domain.Comment;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.domain.TaskPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


class NotificationEventTest {

    private Organization organization;
    private Project project;
    private Task task;
    private User actor;
    private User assignee;
    private User watcher;
    private Object source;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setId(1L);
        organization.setName("Test Org");

        project = new Project();
        project.setId(1L);
        project.setKey("PROJ");
        project.setOrganization(organization);

        task = new Task();
        task.setId(1L);
        task.setKey("PROJ-123");
        task.setTitle("Test Task");
        task.setOrganization(organization);
        task.setProject(project);

        actor = new User();
        actor.setId(1);
        actor.setUsername("actor");
        actor.setEmail("actor@example.com");

        assignee = new User();
        assignee.setId(2);
        assignee.setUsername("assignee");
        assignee.setEmail("assignee@example.com");

        watcher = new User();
        watcher.setId(3);
        watcher.setUsername("watcher");
        watcher.setEmail("watcher@example.com");

        source = new Object();
    }

    

    @Test
    void taskCreatedEvent_hasCorrectType() {
        
        TaskCreatedEvent event = new TaskCreatedEvent(source, task, actor);

        
        assertThat(event.getType()).isEqualTo(NotificationType.TASK_CREATED);
    }

    @Test
    void taskCreatedEvent_generatesCorrectMessage() {
        
        TaskCreatedEvent event = new TaskCreatedEvent(source, task, actor);

        
        assertThat(event.getMessage()).isEqualTo("actor created task PROJ-123");
    }

    @Test
    void taskCreatedEvent_generatesCorrectTitle() {
        
        TaskCreatedEvent event = new TaskCreatedEvent(source, task, actor);

        
        assertThat(event.getTitle()).isEqualTo("New Task");
    }

    @Test
    void taskCreatedEvent_containsTaskAndActor() {
        
        TaskCreatedEvent event = new TaskCreatedEvent(source, task, actor);

        
        assertThat(event.getTask()).isEqualTo(task);
        assertThat(event.getActor()).isEqualTo(actor);
    }

    

    @Test
    void taskAssignedEvent_hasCorrectType() {
        
        TaskAssignedEvent event = new TaskAssignedEvent(source, task, assignee, actor);

        
        assertThat(event.getType()).isEqualTo(NotificationType.TASK_ASSIGNED);
    }

    @Test
    void taskAssignedEvent_generatesCorrectMessage() {
        
        TaskAssignedEvent event = new TaskAssignedEvent(source, task, assignee, actor);

        
        assertThat(event.getMessage()).isEqualTo("actor assigned PROJ-123 to assignee");
    }

    @Test
    void taskAssignedEvent_generatesCorrectTitle() {
        
        TaskAssignedEvent event = new TaskAssignedEvent(source, task, assignee, actor);

        
        assertThat(event.getTitle()).isEqualTo("Task Assigned");
    }

    @Test
    void taskAssignedEvent_containsAssignee() {
        
        TaskAssignedEvent event = new TaskAssignedEvent(source, task, assignee, actor);

        
        assertThat(event.getAssignee()).isEqualTo(assignee);
    }

    

    @Test
    void taskUnassignedEvent_hasCorrectType() {
        
        TaskUnassignedEvent event = new TaskUnassignedEvent(source, task, assignee, actor);

        
        assertThat(event.getType()).isEqualTo(NotificationType.TASK_UNASSIGNED);
    }

    @Test
    void taskUnassignedEvent_generatesCorrectMessage() {
        
        TaskUnassignedEvent event = new TaskUnassignedEvent(source, task, assignee, actor);

        
        assertThat(event.getMessage()).isEqualTo("actor unassigned PROJ-123 from assignee");
    }

    @Test
    void taskUnassignedEvent_generatesCorrectTitle() {
        
        TaskUnassignedEvent event = new TaskUnassignedEvent(source, task, assignee, actor);

        
        assertThat(event.getTitle()).isEqualTo("Task Unassigned");
    }

    @Test
    void taskUnassignedEvent_containsPreviousAssignee() {
        
        TaskUnassignedEvent event = new TaskUnassignedEvent(source, task, assignee, actor);

        
        assertThat(event.getPreviousAssignee()).isEqualTo(assignee);
    }

    

    @Test
    void taskStatusChangedEvent_hasCorrectType() {
        
        TaskStatus oldStatus = createStatus(1L, "To Do");
        TaskStatus newStatus = createStatus(2L, "In Progress");

        
        TaskStatusChangedEvent event = new TaskStatusChangedEvent(source, task, oldStatus, newStatus, actor);

        
        assertThat(event.getType()).isEqualTo(NotificationType.STATUS_CHANGED);
    }

    @Test
    void taskStatusChangedEvent_generatesCorrectMessage() {
        
        TaskStatus oldStatus = createStatus(1L, "To Do");
        TaskStatus newStatus = createStatus(2L, "In Progress");

        
        TaskStatusChangedEvent event = new TaskStatusChangedEvent(source, task, oldStatus, newStatus, actor);

        
        assertThat(event.getMessage()).isEqualTo("actor changed status of PROJ-123 from To Do to In Progress");
    }

    @Test
    void taskStatusChangedEvent_generatesCorrectTitle() {
        
        TaskStatus oldStatus = createStatus(1L, "To Do");
        TaskStatus newStatus = createStatus(2L, "Done");

        
        TaskStatusChangedEvent event = new TaskStatusChangedEvent(source, task, oldStatus, newStatus, actor);

        
        assertThat(event.getTitle()).isEqualTo("Status Changed");
    }

    @Test
    void taskStatusChangedEvent_containsOldAndNewStatus() {
        
        TaskStatus oldStatus = createStatus(1L, "To Do");
        TaskStatus newStatus = createStatus(2L, "In Progress");

        
        TaskStatusChangedEvent event = new TaskStatusChangedEvent(source, task, oldStatus, newStatus, actor);

        
        assertThat(event.getOldStatus()).isEqualTo(oldStatus);
        assertThat(event.getNewStatus()).isEqualTo(newStatus);
    }

    

    @Test
    void taskPriorityChangedEvent_hasCorrectType() {
        
        TaskPriorityChangedEvent event = new TaskPriorityChangedEvent(
                source, task, TaskPriority.LOW, TaskPriority.HIGH, actor);

        
        assertThat(event.getType()).isEqualTo(NotificationType.PRIORITY_CHANGED);
    }

    @Test
    void taskPriorityChangedEvent_generatesCorrectMessage() {
        
        TaskPriorityChangedEvent event = new TaskPriorityChangedEvent(
                source, task, TaskPriority.MEDIUM, TaskPriority.HIGHEST, actor);

        
        assertThat(event.getMessage()).isEqualTo("actor changed priority of PROJ-123 from MEDIUM to HIGHEST");
    }

    @Test
    void taskPriorityChangedEvent_generatesCorrectTitle() {
        
        TaskPriorityChangedEvent event = new TaskPriorityChangedEvent(
                source, task, TaskPriority.LOW, TaskPriority.HIGH, actor);

        
        assertThat(event.getTitle()).isEqualTo("Priority Changed");
    }

    @Test
    void taskPriorityChangedEvent_containsOldAndNewPriority() {
        
        TaskPriorityChangedEvent event = new TaskPriorityChangedEvent(
                source, task, TaskPriority.LOW, TaskPriority.HIGH, actor);

        
        assertThat(event.getOldPriority()).isEqualTo(TaskPriority.LOW);
        assertThat(event.getNewPriority()).isEqualTo(TaskPriority.HIGH);
    }

    

    @Test
    void taskDueDateChangedEvent_hasCorrectType() {
        
        LocalDate oldDate = LocalDate.of(2025, 11, 1);
        LocalDate newDate = LocalDate.of(2025, 12, 1);

        
        TaskDueDateChangedEvent event = new TaskDueDateChangedEvent(source, task, oldDate, newDate, actor);

        
        assertThat(event.getType()).isEqualTo(NotificationType.DUE_DATE_CHANGED);
    }

    @Test
    void taskDueDateChangedEvent_generatesCorrectMessage() {
        
        LocalDate oldDate = LocalDate.of(2025, 11, 1);
        LocalDate newDate = LocalDate.of(2025, 12, 1);

        
        TaskDueDateChangedEvent event = new TaskDueDateChangedEvent(source, task, oldDate, newDate, actor);

        
        assertThat(event.getMessage()).isEqualTo("actor changed due date of PROJ-123 from Nov 1, 2025 to Dec 1, 2025");
    }

    @Test
    void taskDueDateChangedEvent_handlesNullOldDate() {
        
        LocalDate newDate = LocalDate.of(2025, 12, 1);

        
        TaskDueDateChangedEvent event = new TaskDueDateChangedEvent(source, task, null, newDate, actor);

        
        assertThat(event.getMessage()).contains("None").contains("Dec 1, 2025");
    }

    @Test
    void taskDueDateChangedEvent_handlesNullNewDate() {
        
        LocalDate oldDate = LocalDate.of(2025, 11, 1);

        
        TaskDueDateChangedEvent event = new TaskDueDateChangedEvent(source, task, oldDate, null, actor);

        
        assertThat(event.getMessage()).contains("Nov 1, 2025").contains("None");
    }

    @Test
    void taskDueDateChangedEvent_generatesCorrectTitle() {
        
        LocalDate oldDate = LocalDate.of(2025, 11, 1);
        LocalDate newDate = LocalDate.of(2025, 12, 1);

        
        TaskDueDateChangedEvent event = new TaskDueDateChangedEvent(source, task, oldDate, newDate, actor);

        
        assertThat(event.getTitle()).isEqualTo("Due Date Changed");
    }

    

    @Test
    void commentAddedEvent_hasCorrectType() {
        
        Comment comment = createComment("Test comment");

        
        CommentAddedEvent event = new CommentAddedEvent(source, task, comment, actor);

        
        assertThat(event.getType()).isEqualTo(NotificationType.COMMENT_ADDED);
    }

    @Test
    void commentAddedEvent_generatesCorrectMessage() {
        
        Comment comment = createComment("This is a comment");

        
        CommentAddedEvent event = new CommentAddedEvent(source, task, comment, actor);

        
        assertThat(event.getMessage()).isEqualTo("actor commented on PROJ-123: This is a comment");
    }

    @Test
    void commentAddedEvent_truncatesLongComment() {
        
        Comment comment = createComment("A".repeat(150));

        
        CommentAddedEvent event = new CommentAddedEvent(source, task, comment, actor);

        
        assertThat(event.getMessage()).contains("...");
        assertThat(event.getMessage().length()).isLessThan(150);
    }

    @Test
    void commentAddedEvent_generatesCorrectTitle() {
        
        Comment comment = createComment("Test");

        
        CommentAddedEvent event = new CommentAddedEvent(source, task, comment, actor);

        
        assertThat(event.getTitle()).isEqualTo("New Comment");
    }

    @Test
    void commentAddedEvent_containsComment() {
        
        Comment comment = createComment("Test");

        
        CommentAddedEvent event = new CommentAddedEvent(source, task, comment, actor);

        
        assertThat(event.getComment()).isEqualTo(comment);
    }

    

    @Test
    void mentionedEvent_hasCorrectType() {
        
        Comment comment = createComment("@watcher check this out");
        Set<String> mentions = Set.of("watcher");

        
        MentionedEvent event = new MentionedEvent(source, task, comment, mentions, actor);

        
        assertThat(event.getType()).isEqualTo(NotificationType.MENTIONED);
    }

    @Test
    void mentionedEvent_generatesCorrectMessage() {
        
        Comment comment = createComment("@watcher look at this");
        Set<String> mentions = Set.of("watcher");

        
        MentionedEvent event = new MentionedEvent(source, task, comment, mentions, actor);

        
        assertThat(event.getMessage()).isEqualTo("actor mentioned you in PROJ-123: @watcher look at this");
    }

    @Test
    void mentionedEvent_generatesCorrectTitle() {
        
        Comment comment = createComment("@watcher test");
        Set<String> mentions = Set.of("watcher");

        
        MentionedEvent event = new MentionedEvent(source, task, comment, mentions, actor);

        
        assertThat(event.getTitle()).isEqualTo("You Were Mentioned");
    }

    @Test
    void mentionedEvent_containsMentionedUsernames() {
        
        Comment comment = createComment("@user1 @user2 check this");
        Set<String> mentions = Set.of("user1", "user2");

        
        MentionedEvent event = new MentionedEvent(source, task, comment, mentions, actor);

        
        assertThat(event.getMentionedUsernames()).containsExactlyInAnyOrder("user1", "user2");
    }

    

    @Test
    void watcherAddedEvent_hasCorrectType() {
        
        WatcherAddedEvent event = new WatcherAddedEvent(source, task, watcher, actor);

        
        assertThat(event.getType()).isEqualTo(NotificationType.WATCHER_ADDED);
    }

    @Test
    void watcherAddedEvent_generatesCorrectMessage() {
        
        WatcherAddedEvent event = new WatcherAddedEvent(source, task, watcher, actor);

        
        assertThat(event.getMessage()).isEqualTo("actor added you as a watcher to PROJ-123");
    }

    @Test
    void watcherAddedEvent_generatesCorrectTitle() {
        
        WatcherAddedEvent event = new WatcherAddedEvent(source, task, watcher, actor);

        
        assertThat(event.getTitle()).isEqualTo("Added as Watcher");
    }

    @Test
    void watcherAddedEvent_containsWatcher() {
        
        WatcherAddedEvent event = new WatcherAddedEvent(source, task, watcher, actor);

        
        assertThat(event.getWatcher()).isEqualTo(watcher);
    }

    

    private TaskStatus createStatus(Long id, String name) {
        TaskStatus status = new TaskStatus();
        status.setId(id);
        status.setName(name);
        status.setProject(project);
        return status;
    }

    private Comment createComment(String content) {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setContent(content);
        comment.setTask(task);
        comment.setUser(actor);
        comment.setCreatedAt(LocalDateTime.now());
        return comment;
    }
}
