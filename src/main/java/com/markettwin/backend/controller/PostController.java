package com.markettwin.backend.controller;

import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.dto.response.AttachmentDownloadUrlDto;
import com.markettwin.backend.dto.response.PostDetailDto;
import com.markettwin.backend.dto.response.PostListResponseDto;
import com.markettwin.backend.security.CurrentUserProvider;
import com.markettwin.backend.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * 2026-07-24 추가 (게시판 기능)
 *
 * 작성/수정은 파일 업로드(multipart/form-data)를 함께 받아야 해서 JSON @RequestBody 대신
 * @RequestParam 폼 필드 방식을 씀. FE에서 FormData로 title/content/notice/files를
 * 함께 담아 보내면 됨.
 */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public PostListResponseDto list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryCode,
            // 2026-07-24 추가: 관리자 전용 시장 전환 탭. 일반 사용자가 보내더라도
            // PostService가 무시하고 본인 담당 시장으로 강제함(서버가 최종 검증).
            @RequestParam(required = false) String marketCode,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size
    ) {
        User currentUser = currentUserProvider.getCurrentUser();
        return postService.list(keyword, categoryCode, marketCode, page, size, currentUser);
    }

    @GetMapping("/{postId}")
    public PostDetailDto getDetail(
            @PathVariable Long postId,
            // 2026-07-26 추가: 수정 화면(BoardWritePage)이 기존 값을 불러올 때도 이 API를
            // 재사용하는데, 그 경우엔 "조회"가 아니라 "편집 준비"이므로 조회수를 올리면 안 됨.
            // 기본값 true로 둬서 기존 상세 화면 호출은 그대로 동작하고, 편집 화면만 false로 넘김.
            @RequestParam(required = false, defaultValue = "true") boolean countView
    ) {
        User currentUser = currentUserProvider.getCurrentUser();
        return postService.getDetail(postId, currentUser, countView);
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Long>> create(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false, defaultValue = "false") boolean notice,
            @RequestParam(required = false) String categoryCode,
            // 2026-08-12 추가: 관리자만 지정할 수 있는 게시 시장. 빈 문자열은 "전체"라는
            // 뜻이라 파라미터를 아예 보내지 않은 경우(null)와 구분해야 한다.
            @RequestParam(required = false) String marketCode,
            @RequestParam(required = false) List<MultipartFile> files
    ) {
        User currentUser = currentUserProvider.getCurrentUser();
        Long postId = postService.create(title, content, notice, categoryCode, marketCode, files, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("postId", postId));
    }

    @PutMapping(value = "/{postId}", consumes = "multipart/form-data")
    public ResponseEntity<Void> update(
            @PathVariable Long postId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) Boolean notice,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String marketCode,
            @RequestParam(required = false) List<Long> deleteAttachmentIds,
            @RequestParam(required = false) List<MultipartFile> files
    ) {
        User currentUser = currentUserProvider.getCurrentUser();
        postService.update(postId, title, content, notice, categoryCode, marketCode,
                deleteAttachmentIds, files, currentUser);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(@PathVariable Long postId) {
        User currentUser = currentUserProvider.getCurrentUser();
        postService.delete(postId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/like")
    public Map<String, Boolean> toggleLike(@PathVariable Long postId) {
        User currentUser = currentUserProvider.getCurrentUser();
        boolean liked = postService.toggleLike(postId, currentUser);
        return Map.of("liked", liked);
    }

    // 2026-08-04 변경: 302 리다이렉트 방식은 크로스오리진 리다이렉트 시 브라우저가
    // Origin 헤더를 "null"로 바꿔버려서(표준 스펙 동작) S3 CORS 설정을 아무리
    // 정확히 해도 절대 통과할 수 없는 구조적 문제가 있었음. presigned URL을 JSON으로
    // 내려주고 FE가 window.location으로 직접 이동시키는 방식으로 변경 - 진짜 페이지
    // 이동은 CORS 검사 대상이 아니라 이 문제 자체가 생기지 않음.
    @GetMapping("/{postId}/attachments/{attachmentId}/download")
    public AttachmentDownloadUrlDto download(@PathVariable Long postId, @PathVariable Long attachmentId) {
        User currentUser = currentUserProvider.getCurrentUser();
        URL presignedUrl = postService.getDownloadUrl(postId, attachmentId, currentUser);
        return new AttachmentDownloadUrlDto(presignedUrl.toString());
    }
}
