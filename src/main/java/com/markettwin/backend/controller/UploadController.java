package com.markettwin.backend.controller;

import com.markettwin.backend.service.VideoS3Service;
import com.markettwin.backend.util.UploadFiles;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class UploadController {

    private final VideoS3Service videoS3Service;

    @GetMapping("/presigned-url")
    public ResponseEntity<Map<String, String>> getPresignedUrl(
            @RequestParam String filename,
            @RequestParam(defaultValue = "raw-videos") String folder) {

        //  보안 검증: 원본영상, 위험클립, PDF신고서 3개 폴더만 허용
        if (!folder.equals("raw-videos") && !folder.equals("danger-clips") && !folder.equals("post-reports")) {
            throw new IllegalArgumentException("허용되지 않은 S3 폴더명입니다.");
        }

        // 2026-08-20 추가(보안 감사 BE-09): 파일명과 확장자를 검사한다.
        //
        // 이전에는 요청의 filename 을 그대로 S3 키에 이어 붙이고, content-type 은
        // ".pdf 로 끝나는가"로만 갈라서 그 외에는 전부 video/mp4 로 서명했다.
        // 확장자가 무엇이든(.exe, .html 등) 영상으로 위장해 올릴 수 있었고,
        // 파일명에 경로나 개행이 섞여도 걸러지지 않았다.
        String safeName = UploadFiles.sanitizeName(filename);
        Set<String> allowed = "post-reports".equals(folder)
                ? UploadFiles.DOCUMENT_EXTENSIONS   // 신고서는 PDF 등 문서
                : UploadFiles.VIDEO_EXTENSIONS;     // 원본·위험 클립은 영상
        UploadFiles.requireAllowedExtension(safeName, allowed, "업로드");

        String key = folder + "/" + UUID.randomUUID() + "_" + safeName;

        // 확장자에서 정확한 타입을 끌어온다(위 검사를 통과한 확장자만 온다).
        String contentType = UploadFiles.contentTypeOf(safeName);

        URL presignedUrl = videoS3Service.generatePresignedUploadUrl(key, contentType, Duration.ofMinutes(10));

        Map<String, String> response = new HashMap<>();
        response.put("presignedUrl", presignedUrl.toString());
        response.put("fileKey", key);

        return ResponseEntity.ok(response);
    }
}
