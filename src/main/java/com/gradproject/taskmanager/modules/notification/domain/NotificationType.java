package com.gradproject.taskmanager.modules.notification.domain;


public enum NotificationType {
    
    TASK_CREATED,           
    TASK_ASSIGNED,          
    TASK_UNASSIGNED,        

    
    STATUS_CHANGED,         
    PRIORITY_CHANGED,       
    DUE_DATE_CHANGED,       

    
    COMMENT_ADDED,          
    COMMENT_REPLY,          
    MENTIONED,              

    
    ATTACHMENT_ADDED,       

    
    WATCHER_ADDED           
}
