package com.markettwin.backend.exception;

// 게시판 첨부파일 기능
public class FileStorageException extends RuntimeException {
    public FileStorageException(String message) {
        super(message);
    }
}
