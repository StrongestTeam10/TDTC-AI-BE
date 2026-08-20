package com.markettwin.backend.exception;

// 게시판 기능
public class AttachmentNotFoundException extends RuntimeException {
    public AttachmentNotFoundException(Long attachmentId) {
        super("첨부파일을 찾을 수 없습니다: " + attachmentId);
    }
}
