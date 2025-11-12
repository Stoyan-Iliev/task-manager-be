package com.gradproject.taskmanager.modules.task.dto;


public record AttachmentDownload(
        String filename,
        String mimeType,
        byte[] data
) {
    
    public long getFileSize() {
        return data != null ? data.length : 0;
    }

    
    public boolean isEmpty() {
        return data == null || data.length == 0;
    }
}
