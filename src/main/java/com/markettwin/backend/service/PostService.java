package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.Post;
import com.markettwin.backend.domain.entity.PostAttachment;
import com.markettwin.backend.domain.entity.PostLike;
import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.dto.response.AttachmentDto;
import com.markettwin.backend.dto.response.PageResponseDto;
import com.markettwin.backend.dto.response.PostDetailDto;
import com.markettwin.backend.dto.response.PostListResponseDto;
import com.markettwin.backend.dto.response.PostSummaryDto;
import com.markettwin.backend.exception.AttachmentNotFoundException;
import com.markettwin.backend.exception.ForbiddenActionException;
import com.markettwin.backend.exception.InvalidCategoryCodeException;
import com.markettwin.backend.exception.PostNotFoundException;
import com.markettwin.backend.repository.CommonCodeRepository;
import com.markettwin.backend.repository.PostAttachmentRepository;
import com.markettwin.backend.repository.PostLikeRepository;
import com.markettwin.backend.repository.PostRepository;
import com.markettwin.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 2026-07-24 추가 (게시판 기능)
 *
 * 권한 규칙 요약:
 *  - 목록/상세 조회: 관리자(ROL01)는 전체 시장, 그 외는 본인 market_code 글 + 공지 전체
 *  - 수정/삭제: 관리자는 전체, 그 외는 본인 작성 글만
 *  - 공지 고정(notice): 관리자만 설정/해제 가능
 *
 * 2026-07-26: 목록 조회의 N+1 문제(게시글마다 첨부파일 개수·작성자 이름을 개별
 * 조회하던 것)를 배치 조회(IN 절, batchAttachmentCounts/batchWriterNames)로 해결함.
 * toDetail은 게시글 1건만 다루므로 N+1 대상이 아니라 그대로 유지.
 */
@Service
@RequiredArgsConstructor
public class PostService {

    private static final String ADMIN_ROLE_CODE = "ROL01";
    private static final int MAX_PAGE_SIZE = 50;
    private static final String ATTACHMENT_KEY_PREFIX = "board-attachments";
    private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(5);

    // 2026-07-24 추가(UI 설계서 반영 - 카테고리 탭)
    private static final String CATEGORY_CODE_COB = "BCT";
    private static final String NOTICE_CATEGORY_CODE = "BCTNT"; // 공지사항 카테고리 - 관리자만 작성 가능
    // 2026-07-25 변경: 카테고리를 공지사항/자유게시판 2개로 축소하면서 기본값도
    // BCTQA(질문과 답변, 폐지) -> BCTFR(자유게시판)로 변경
    private static final String DEFAULT_CATEGORY_CODE = "BCTFR"; // 카테고리 미지정 시 기본값(자유게시판)

    private final PostRepository postRepository;
    private final PostAttachmentRepository attachmentRepository;
    private final PostLikeRepository likeRepository;
    private final UserRepository userRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public PostListResponseDto list(String keyword, String categoryCode, String marketCodeFilter,
                                     int page, int size, User currentUser) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);

        // 공지: 시장/카테고리 무관 항상 전체 노출, 페이징 없이 최신순
        List<Post> pinnedPosts = postRepository.findByNoticeTrueOrderByCreatedAtDesc();

        Specification<Post> spec = Specification.where(PostSpecs.isNotNotice());
        if (isAdmin(currentUser)) {
            // 2026-07-24 추가: 관리자는 시장 탭으로 특정 시장을 선택했을 때만 필터링하고,
            // "전체"(파라미터 없음)면 전 시장을 다 봄. 일반 사용자는 이 파라미터 자체를
            // 보내지 않도록 FE에서 막아두지만, 혹시 보내더라도 서버가 무시하고 본인
            // market_code로 강제함(아래 else 분기) - 클라이언트를 신뢰하지 않기 위함.
            if (marketCodeFilter != null && !marketCodeFilter.isBlank()) {
                spec = spec.and(PostSpecs.marketCodeEquals(marketCodeFilter));
            }
        } else {
            spec = spec.and(PostSpecs.marketCodeEquals(currentUser.getMarketCode()));
        }
        if (categoryCode != null && !categoryCode.isBlank()) {
            spec = spec.and(PostSpecs.categoryCodeEquals(categoryCode));
        }
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(PostSpecs.keywordMatches(keyword.trim()));
        }

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> resultPage = postRepository.findAll(spec, pageable);

        // 2026-07-26: 공지 + 페이지 게시글을 합친 ID 목록으로 첨부파일 개수·작성자 이름을
        // 각각 쿼리 1번씩(총 2번)만 배치 조회한다. 이전에는 게시글마다 개별 조회(N+1)했음.
        List<Post> allPosts = new ArrayList<>(pinnedPosts);
        allPosts.addAll(resultPage.getContent());
        Map<Long, Integer> attachmentCounts = batchAttachmentCounts(allPosts);
        Map<Long, String> writerNames = batchWriterNames(allPosts);

        List<PostSummaryDto> pinned = pinnedPosts.stream()
                .map(post -> toSummary(post, attachmentCounts, writerNames))
                .toList();

        return PostListResponseDto.builder()
                .pinned(pinned)
                .page(PageResponseDto.from(resultPage.map(post -> toSummary(post, attachmentCounts, writerNames))))
                .build();
    }

    // 2026-07-26 추가: 게시글 목록의 첨부파일 개수를 IN 절 1번으로 일괄 조회
    private Map<Long, Integer> batchAttachmentCounts(List<Post> posts) {
        List<Long> postIds = posts.stream().map(Post::getPostId).distinct().toList();
        if (postIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> counts = new HashMap<>();
        for (Object[] row : attachmentRepository.countByPostIdIn(postIds)) {
            counts.put((Long) row[0], ((Long) row[1]).intValue());
        }
        return counts;
    }

    // 2026-07-26 추가: 게시글 목록의 작성자 이름을 IN 절 1번으로 일괄 조회
    private Map<Long, String> batchWriterNames(List<Post> posts) {
        List<Long> writerIds = posts.stream().map(Post::getWriterId).distinct().toList();
        if (writerIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new HashMap<>();
        for (Object[] row : userRepository.findNamesByIds(writerIds)) {
            names.put((Long) row[0], (String) row[1]);
        }
        return names;
    }

    @Transactional
    public PostDetailDto getDetail(Long postId, User currentUser, boolean countView) {
        Post post = getPostOrThrow(postId);
        assertVisible(post, currentUser);

        // 2026-07-26 변경: 편집 화면에서 기존 값을 불러오는 호출(countView=false)은
        // 조회수를 올리지 않도록 분기. 상세 화면에서의 일반 조회(countView=true, 기본값)만 증가.
        if (countView) {
            post.setViewCount(post.getViewCount() + 1);
            postRepository.save(post);
        }

        return toDetail(post, currentUser);
    }

    @Transactional
    public Long create(String title, String content, boolean noticeRequested, String categoryCodeRequested,
                        List<MultipartFile> files, User currentUser) {
        if (noticeRequested && !isAdmin(currentUser)) {
            throw new ForbiddenActionException("공지 고정은 관리자만 설정할 수 있습니다.");
        }
        boolean notice = noticeRequested; // 위에서 이미 관리자 검증 완료

        String categoryCode = validateCategoryCode(categoryCodeRequested, currentUser);

        // 공지는 시장 무관 전체 노출이라 market_code를 필수로 요구하지 않음(관리자 담당
        // 시장이 없어도 공지는 작성 가능해야 하므로). 일반 글은 작성자 marketCode로 채움.
        String marketCode = notice ? currentUser.getMarketCode() : requireMarketCode(currentUser);

        Instant now = Instant.now();
        Post post = Post.builder()
                .marketCode(marketCode)
                .writerId(currentUser.getUserId())
                .title(title)
                .content(content)
                .notice(notice)
                .categoryCode(categoryCode)
                .viewCount(0)
                .likeCount(0)
                .createdAt(now)
                .build();
        Post saved = postRepository.save(post);

        uploadAttachments(saved.getPostId(), files);

        return saved.getPostId();
    }

    @Transactional
    public void update(Long postId, String title, String content, Boolean noticeRequested,
                        String categoryCodeRequested,
                        List<Long> deleteAttachmentIds, List<MultipartFile> newFiles, User currentUser) {
        Post post = getPostOrThrow(postId);
        assertCanModify(post, currentUser);

        if (title != null && !title.isBlank()) {
            post.setTitle(title);
        }
        if (content != null) {
            post.setContent(content);
        }
        if (noticeRequested != null) {
            if (!isAdmin(currentUser)) {
                throw new ForbiddenActionException("공지 고정 여부는 관리자만 변경할 수 있습니다.");
            }
            post.setNotice(noticeRequested);
        }
        if (categoryCodeRequested != null && !categoryCodeRequested.isBlank()) {
            post.setCategoryCode(validateCategoryCode(categoryCodeRequested, currentUser));
        }
        post.setUpdatedAt(Instant.now());
        postRepository.save(post);

        if (deleteAttachmentIds != null) {
            for (Long attachmentId : deleteAttachmentIds) {
                removeAttachment(post.getPostId(), attachmentId);
            }
        }
        uploadAttachments(post.getPostId(), newFiles);
    }

    @Transactional
    public void delete(Long postId, User currentUser) {
        Post post = getPostOrThrow(postId);
        assertCanModify(post, currentUser);

        // brdattc01d/brdlike01d 행 자체는 ON DELETE CASCADE로 DB에서 정리되지만,
        // S3에 올라간 실제 파일 객체는 DB가 대신 지워주지 않으므로 먼저 지워야 함
        attachmentRepository.findByPostId(postId)
                .forEach(attachment -> fileStorageService.delete(attachment.getS3Key()));

        postRepository.delete(post);
    }

    @Transactional
    public boolean toggleLike(Long postId, User currentUser) {
        Post post = getPostOrThrow(postId);
        assertVisible(post, currentUser);

        boolean alreadyLiked = likeRepository.existsByPostIdAndUserId(postId, currentUser.getUserId());
        if (alreadyLiked) {
            likeRepository.deleteByPostIdAndUserId(postId, currentUser.getUserId());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        } else {
            likeRepository.save(PostLike.builder()
                    .postId(postId)
                    .userId(currentUser.getUserId())
                    .createdAt(Instant.now())
                    .build());
            post.setLikeCount(post.getLikeCount() + 1);
        }
        postRepository.save(post);
        return !alreadyLiked;
    }

    @Transactional(readOnly = true)
    public URL getDownloadUrl(Long postId, Long attachmentId, User currentUser) {
        Post post = getPostOrThrow(postId);
        assertVisible(post, currentUser);

        PostAttachment attachment = attachmentRepository.findByPostId(postId).stream()
                .filter(a -> a.getAttachmentId().equals(attachmentId))
                .findFirst()
                .orElseThrow(() -> new AttachmentNotFoundException(attachmentId));

        return fileStorageService.generatePresignedDownloadUrl(
                attachment.getS3Key(), attachment.getOriginalName(), DOWNLOAD_URL_TTL);
    }

    // ---------- 내부 헬퍼 ----------

    private void uploadAttachments(Long postId, List<MultipartFile> files) {
        if (files == null) {
            return;
        }
        Instant now = Instant.now();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String key = fileStorageService.upload(file, ATTACHMENT_KEY_PREFIX + "/" + postId);
            attachmentRepository.save(PostAttachment.builder()
                    .postId(postId)
                    .originalName(file.getOriginalFilename())
                    .s3Key(key)
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .createdAt(now)
                    .build());
        }
    }

    private void removeAttachment(Long postId, Long attachmentId) {
        attachmentRepository.findById(attachmentId).ifPresentOrElse(attachment -> {
            if (!attachment.getPostId().equals(postId)) {
                throw new AttachmentNotFoundException(attachmentId);
            }
            fileStorageService.delete(attachment.getS3Key());
            attachmentRepository.delete(attachment);
        }, () -> {
            throw new AttachmentNotFoundException(attachmentId);
        });
    }

    private Post getPostOrThrow(Long postId) {
        return postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException(postId));
    }

    private boolean isAdmin(User user) {
        return ADMIN_ROLE_CODE.equals(user.getRulesCode());
    }

    // 다른 시장 소속 글은 "권한 없음"이 아니라 "존재하지 않음"으로 응답을 통일함
    // (다른 시장에 어떤 글이 있는지 자체를 노출하지 않기 위한 선택)
    private void assertVisible(Post post, User currentUser) {
        if (isAdmin(currentUser) || post.isNotice()) {
            return;
        }
        boolean sameMarket = post.getMarketCode() != null
                && post.getMarketCode().equals(currentUser.getMarketCode());
        if (!sameMarket) {
            throw new PostNotFoundException(post.getPostId());
        }
    }

    private void assertCanModify(Post post, User currentUser) {
        if (isAdmin(currentUser)) {
            return;
        }
        if (!post.getWriterId().equals(currentUser.getUserId())) {
            throw new ForbiddenActionException("본인이 작성한 게시글만 수정/삭제할 수 있습니다.");
        }
    }

    private String requireMarketCode(User user) {
        if (user.getMarketCode() == null || user.getMarketCode().isBlank()) {
            throw new ForbiddenActionException("담당 시장 정보가 없어 일반 게시글을 작성할 수 없습니다. 관리자에게 문의하세요.");
        }
        return user.getMarketCode();
    }

    // 2026-07-24 추가(UI 설계서 반영 - 카테고리 탭)
    // 카테고리 미지정 시 기본값(자유게시판)으로 채우고, comcode01m BCT 도메인에 실제
    // 존재하는 코드인지 확인함. 공지사항(BCTNT) 카테고리는 관리자만 선택 가능 -
    // is_notice(상단 고정) 권한 검증과 별개로 한 번 더 막아둠(카테고리만 공지사항으로
    // 골라서 우회하는 것 방지).
    private String validateCategoryCode(String categoryCode, User currentUser) {
        String resolved = (categoryCode == null || categoryCode.isBlank())
                ? DEFAULT_CATEGORY_CODE
                : categoryCode;

        if (NOTICE_CATEGORY_CODE.equals(resolved) && !isAdmin(currentUser)) {
            throw new ForbiddenActionException("공지사항 카테고리는 관리자만 작성할 수 있습니다.");
        }

        boolean valid = commonCodeRepository.existsByCodeCobAndCode(CATEGORY_CODE_COB, resolved);
        if (!valid) {
            throw new InvalidCategoryCodeException(resolved);
        }
        return resolved;
    }

    private PostSummaryDto toSummary(Post post, Map<Long, Integer> attachmentCounts, Map<Long, String> writerNames) {
        int attachmentCount = attachmentCounts.getOrDefault(post.getPostId(), 0);
        String writerName = writerNames.getOrDefault(post.getWriterId(), "(알수없음)");

        return PostSummaryDto.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .writerName(writerName)
                .marketCode(post.getMarketCode())
                .categoryCode(post.getCategoryCode())
                .notice(post.isNotice())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .attachmentCount(attachmentCount)
                .createdAt(post.getCreatedAt())
                .build();
    }

    private PostDetailDto toDetail(Post post, User currentUser) {
        List<AttachmentDto> attachments = attachmentRepository.findByPostId(post.getPostId()).stream()
                .map(a -> AttachmentDto.builder()
                        .attachmentId(a.getAttachmentId())
                        .originalName(a.getOriginalName())
                        .fileSize(a.getFileSize())
                        .contentType(a.getContentType())
                        .build())
                .toList();

        String writerName = userRepository.findById(post.getWriterId())
                .map(User::getName)
                .orElse("(알수없음)");
        boolean liked = likeRepository.existsByPostIdAndUserId(post.getPostId(), currentUser.getUserId());
        boolean canModify = isAdmin(currentUser) || post.getWriterId().equals(currentUser.getUserId());

        return PostDetailDto.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(post.getContent())
                .writerId(post.getWriterId())
                .writerName(writerName)
                .marketCode(post.getMarketCode())
                .categoryCode(post.getCategoryCode())
                .notice(post.isNotice())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .liked(liked)
                .canEdit(canModify)
                .canDelete(canModify)
                .attachments(attachments)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
