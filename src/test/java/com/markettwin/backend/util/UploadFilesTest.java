package com.markettwin.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 업로드 파일명 정규화와 확장자 검사를 확인한다.
 *
 * presigned URL 발급은 요청의 filename 을 S3 키에 그대로 이어 붙였고 content-type 도
 * ".pdf 인가"로만 갈랐다. 확장자를 무엇으로 주든 영상으로 서명되던 자리라, 아래
 * 경우들이 다시 열리면 바로 드러나도록 고정해 둔다.
 */
class UploadFilesTest {

    // ---------- 파일명 정규화 ----------

    @Test
    @DisplayName("상위 경로 이동(../)을 걷어낸다")
    void stripsParentTraversal() {
        assertThat(UploadFiles.sanitizeName("../../etc/passwd")).isEqualTo("passwd");
        assertThat(UploadFiles.sanitizeName("..\\..\\windows\\system32\\a.mp4")).isEqualTo("a.mp4");
    }

    @Test
    @DisplayName("브라우저가 보낸 전체 경로에서 파일명만 남긴다")
    void keepsOnlyFileName() {
        assertThat(UploadFiles.sanitizeName("C:\\Users\\worjs\\사진.png")).isEqualTo("사진.png");
        assertThat(UploadFiles.sanitizeName("/var/tmp/clip.mp4")).isEqualTo("clip.mp4");
    }

    @Test
    @DisplayName("개행·널바이트 같은 제어문자를 제거한다")
    void removesControlCharacters() {
        assertThat(UploadFiles.sanitizeName("a\nb\r\nc.mp4")).isEqualTo("abc.mp4");
        assertThat(UploadFiles.sanitizeName("evil\u0000.mp4")).isEqualTo("evil.mp4");
    }

    @Test
    @DisplayName("숨김파일처럼 앞에 붙은 점을 없앤다")
    void stripsLeadingDots() {
        assertThat(UploadFiles.sanitizeName(".hidden.mp4")).isEqualTo("hidden.mp4");
    }

    @Test
    @DisplayName("이름이 너무 길면 확장자를 지킨 채 자른다")
    void truncatesLongNameKeepingExtension() {
        String name = "가".repeat(400) + ".mp4";
        String out = UploadFiles.sanitizeName(name);
        assertThat(out).endsWith(".mp4");
        assertThat(out.length()).isLessThanOrEqualTo(150);
    }

    @Test
    @DisplayName("남는 글자가 없으면 기본 이름을 준다")
    void fallsBackWhenEmpty() {
        assertThat(UploadFiles.sanitizeName(null)).isEqualTo("file");
        assertThat(UploadFiles.sanitizeName("   ")).isEqualTo("file");
        assertThat(UploadFiles.sanitizeName("///")).isEqualTo("file");
    }

    @Test
    @DisplayName("한글·공백이 든 정상 이름은 그대로 둔다")
    void keepsNormalKoreanName() {
        assertThat(UploadFiles.sanitizeName("망원시장 1구역 영상.mp4"))
                .isEqualTo("망원시장 1구역 영상.mp4");
    }

    // ---------- 확장자 ----------

    @Test
    @DisplayName("확장자는 소문자로 뽑는다")
    void extensionIsLowercase() {
        assertThat(UploadFiles.extensionOf("CLIP.MP4")).isEqualTo("mp4");
        assertThat(UploadFiles.extensionOf("a.b.PdF")).isEqualTo("pdf");
    }

    @Test
    @DisplayName("확장자가 없으면 빈 문자열")
    void noExtension() {
        assertThat(UploadFiles.extensionOf("noext")).isEmpty();
        assertThat(UploadFiles.extensionOf("trailing.")).isEmpty();
        assertThat(UploadFiles.extensionOf(null)).isEmpty();
    }

    @Test
    @DisplayName("허용 목록 밖 확장자는 거부한다 - 실행파일이 영상으로 위장하던 자리")
    void rejectsDisallowedExtension() {
        assertThatThrownBy(() ->
                UploadFiles.requireAllowedExtension("evil.exe", UploadFiles.VIDEO_EXTENSIONS, "업로드"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(".exe");

        assertThatThrownBy(() ->
                UploadFiles.requireAllowedExtension("page.html", UploadFiles.IMAGE_EXTENSIONS, "상점 사진"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("확장자가 아예 없으면 거부한다")
    void rejectsMissingExtension() {
        assertThatThrownBy(() ->
                UploadFiles.requireAllowedExtension("noext", UploadFiles.VIDEO_EXTENSIONS, "업로드"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("확장자가 없는");
    }

    @Test
    @DisplayName("이중 확장자는 마지막 것으로 판정한다")
    void doubleExtensionUsesLast() {
        assertThat(UploadFiles.extensionOf("clip.mp4.exe")).isEqualTo("exe");
        assertThatThrownBy(() ->
                UploadFiles.requireAllowedExtension("clip.mp4.exe", UploadFiles.VIDEO_EXTENSIONS, "업로드"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("허용된 확장자는 통과한다")
    void allowsWhitelisted() {
        assertThatCode(() -> {
            UploadFiles.requireAllowedExtension("clip.mp4", UploadFiles.VIDEO_EXTENSIONS, "업로드");
            UploadFiles.requireAllowedExtension("보고서.PDF", UploadFiles.DOCUMENT_EXTENSIONS, "업로드");
            UploadFiles.requireAllowedExtension("front.jpeg", UploadFiles.IMAGE_EXTENSIONS, "상점 사진");
        }).doesNotThrowAnyException();
    }

    // ---------- Content-Type ----------

    @Test
    @DisplayName("확장자에 맞는 타입을 준다 - 예전엔 PDF가 아니면 전부 video/mp4였다")
    void contentTypeFollowsExtension() {
        assertThat(UploadFiles.contentTypeOf("a.pdf")).isEqualTo("application/pdf");
        assertThat(UploadFiles.contentTypeOf("a.mp4")).isEqualTo("video/mp4");
        assertThat(UploadFiles.contentTypeOf("a.mov")).isEqualTo("video/quicktime");
        assertThat(UploadFiles.contentTypeOf("a.png")).isEqualTo("image/png");
        assertThat(UploadFiles.contentTypeOf("a.JPG")).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("모르는 확장자는 octet-stream 으로 둔다")
    void unknownContentType() {
        assertThat(UploadFiles.contentTypeOf("a.xyz")).isEqualTo("application/octet-stream");
        assertThat(UploadFiles.contentTypeOf("noext")).isEqualTo("application/octet-stream");
    }
}
