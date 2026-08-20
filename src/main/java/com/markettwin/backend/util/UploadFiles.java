package com.markettwin.backend.util;

import java.util.Locale;
import java.util.Set;

/**
 * 업로드 파일명 정규화와 확장자 검사.
 *
 * 업로드 경로가 세 군데인데(게시판 첨부, 상점 사진, presigned URL 발급) 어느 쪽도
 * 파일명이나 확장자를 확인하지 않고 있었다. 특히 presigned URL은 요청에 담긴
 * filename 을 S3 키에 그대로 이어 붙이고, content-type 은 ".pdf 로 끝나는가"로만
 * 갈라서 그 외에는 전부 video/mp4 로 서명했다. 확장자가 무엇이든 영상으로
 * 위장해 올릴 수 있었다는 뜻이다.
 *
 * 여기서 하는 일은 두 가지다.
 *   1) 파일명에서 경로·제어문자를 걷어내 S3 키와 다운로드 헤더에 안전한 형태로 만든다.
 *   2) 확장자를 허용 목록과 대조한다(차단 목록이 아니라 허용 목록이다 - 새 위험
 *      확장자가 생겨도 기본이 거부로 남는다).
 *
 * 실제 내용까지 검사하지는 않는다. 확장자만 맞춘 위장 파일은 여전히 통과하므로,
 * 파일을 열어 쓰는 기능이 생기면 그 시점에 별도 검증이 필요하다.
 */
public final class UploadFiles {

    private UploadFiles() {
    }

    /** 상점 외관 사진 등 이미지 전용 경로. */
    public static final Set<String> IMAGE_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic");

    /** CCTV 원본·클립 영상. */
    public static final Set<String> VIDEO_EXTENSIONS =
            Set.of("mp4", "mov", "avi", "mkv", "webm");

    /** 게시판 첨부로 오갈 만한 문서. */
    public static final Set<String> DOCUMENT_EXTENSIONS =
            Set.of("pdf", "hwp", "hwpx", "doc", "docx", "xls", "xlsx",
                   "ppt", "pptx", "txt", "csv", "zip");

    /** 확장자 → Content-Type. 목록에 없으면 호출부가 기본값을 정한다. */
    private static String mimeOf(String ext) {
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "mp4" -> "video/mp4";
            case "mov" -> "video/quicktime";
            case "avi" -> "video/x-msvideo";
            case "mkv" -> "video/x-matroska";
            case "webm" -> "video/webm";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "heic" -> "image/heic";
            case "txt" -> "text/plain";
            case "csv" -> "text/csv";
            case "zip" -> "application/zip";
            default -> "application/octet-stream";
        };
    }

    /** 파일명 최대 길이. S3 키 전체 한도(1024)에 접두사·UUID 몫을 남긴 값. */
    private static final int MAX_NAME_LENGTH = 150;

    /**
     * 파일명에서 경로와 제어문자를 걷어낸다.
     *
     * 브라우저에 따라 전체 경로("C:\Users\...\a.png")를 보내기도 하고, 악의적인
     * 요청은 "../" 를 섞어 보낸다. S3 키는 평면이라 상위 디렉터리로 빠져나가지는
     * 않지만, 이 이름이 다운로드 시 Content-Disposition 에 실려 나가므로 그대로 두면
     * 받는 쪽 파일 경로를 흔들 수 있다.
     *
     * @return 정리된 이름. 남는 글자가 없으면 "file"
     */
    public static String sanitizeName(String raw) {
        if (raw == null) {
            return "file";
        }
        // 경로 구분자 이후만 취한다(윈도·유닉스 양쪽).
        String name = raw.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        // 제어문자(널바이트·개행 포함)와 따옴표류를 제거한다.
        name = name.replaceAll("[\\p{Cntrl}\"'<>|?*:]", "");
        // 앞뒤 점·공백은 확장자 판정을 흐리고 윈도에서 문제를 만든다.
        name = name.strip();
        while (name.startsWith(".")) {
            name = name.substring(1);
        }
        if (name.length() > MAX_NAME_LENGTH) {
            String ext = extensionOf(name);
            int keep = MAX_NAME_LENGTH - (ext.isEmpty() ? 0 : ext.length() + 1);
            name = name.substring(0, Math.max(1, keep)) + (ext.isEmpty() ? "" : "." + ext);
        }
        return name.isBlank() ? "file" : name;
    }

    /** 소문자 확장자(점 제외). 없으면 빈 문자열. */
    public static String extensionOf(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** 허용 목록에 있는 확장자인지. */
    public static boolean hasAllowedExtension(String name, Set<String> allowed) {
        return allowed.contains(extensionOf(name));
    }

    /**
     * 허용 목록에 없으면 예외를 던진다.
     *
     * @throws IllegalArgumentException 확장자가 없거나 허용 목록 밖일 때
     */
    public static void requireAllowedExtension(String name, Set<String> allowed, String what) {
        String ext = extensionOf(name);
        if (ext.isEmpty()) {
            throw new IllegalArgumentException(what + ": 확장자가 없는 파일은 올릴 수 없습니다.");
        }
        if (!allowed.contains(ext)) {
            throw new IllegalArgumentException(
                    what + ": ." + ext + " 형식은 허용되지 않습니다. 허용 형식 - "
                            + String.join(", ", allowed.stream().sorted().toList()));
        }
    }

    /** 확장자에 맞는 Content-Type. 모르는 확장자는 octet-stream. */
    public static String contentTypeOf(String name) {
        return mimeOf(extensionOf(name));
    }
}
