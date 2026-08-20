package com.markettwin.backend;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 로그 메시지의 줄바꿈이 실제로 제거되는지 확인한다(보안 감사 BE-06).
 *
 * 운영 로그는 awslogs 드라이버가 "타임스탬프로 시작하는 줄"을 새 이벤트로 끊어
 * CloudWatch 에 올린다(docker-compose.yml 의 awslogs-multiline-pattern). 그래서
 * 입력에 줄바꿈과 가짜 타임스탬프를 섞으면 진짜와 구분되지 않는 로그 이벤트를
 * 심을 수 있다. 아래는 그 경로가 막혀 있는지를 본다.
 *
 * 설정 파일의 패턴을 직접 읽어와 검사하므로, logback-spring.xml 에서 %replace 가
 * 빠지면 이 테스트가 깨진다.
 */
class LogPatternTest {

    private static final File CONFIG = new File("src/main/resources/logback-spring.xml");

    /** logback-spring.xml 에 정의된 CONSOLE_LOG_PATTERN 값을 그대로 읽어온다. */
    private String consolePatternFromConfig() throws Exception {
        var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(CONFIG);
        var nodes = doc.getElementsByTagName("property");
        for (int i = 0; i < nodes.getLength(); i++) {
            var el = (org.w3c.dom.Element) nodes.item(i);
            if ("CONSOLE_LOG_PATTERN".equals(el.getAttribute("name"))) {
                return el.getAttribute("value");
            }
        }
        throw new IllegalStateException("logback-spring.xml 에 CONSOLE_LOG_PATTERN 이 없다");
    }

    /**
     * 설정 파일의 패턴에서 메시지 부분(%replace(%m){...})만 떼어낸다.
     * 전체 패턴에는 Spring Boot 확장 컨버터(%clr, %applicationName)가 들어 있어
     * 순수 logback 컨텍스트로는 렌더링할 수 없기 때문이다.
     */
    private String messageConverterFromConfig() throws Exception {
        String full = consolePatternFromConfig();
        Matcher m = Pattern.compile("%replace\\(%m\\)\\{[^}]*}").matcher(full);
        assertThat(m.find())
                .as("CONSOLE_LOG_PATTERN 에 %replace(%m){...} 가 있어야 한다")
                .isTrue();
        return m.group();
    }

    private String render(String converterPattern, String message) {
        LoggerContext ctx = new LoggerContext();
        PatternLayout layout = new PatternLayout();
        layout.setContext(ctx);
        layout.setPattern(converterPattern);
        layout.start();

        LoggingEvent event = new LoggingEvent();
        event.setLoggerName("test");
        event.setLevel(Level.INFO);
        event.setMessage(message);
        event.setTimeStamp(System.currentTimeMillis());

        String out = layout.doLayout(event);
        layout.stop();
        return out;
    }

    @Test
    @DisplayName("설정 파일이 메시지에 %replace 를 걸어두었다")
    void configWrapsMessageWithReplace() throws Exception {
        assertThat(consolePatternFromConfig()).contains("%replace(%m)");
    }

    @Test
    @DisplayName("메시지 안의 개행(LF)이 사라진다")
    void removesLineFeed() throws Exception {
        String out = render(messageConverterFromConfig(), "앞부분\n뒷부분");
        assertThat(out).doesNotContain("\n");
        assertThat(out).contains("앞부분").contains("뒷부분");
    }

    @Test
    @DisplayName("윈도 개행(CRLF)과 캐리지리턴도 사라진다")
    void removesCarriageReturn() throws Exception {
        String conv = messageConverterFromConfig();
        assertThat(render(conv, "a\r\nb")).doesNotContain("\r").doesNotContain("\n");
        assertThat(render(conv, "a\rb")).doesNotContain("\r");
    }

    @Test
    @DisplayName("가짜 로그 이벤트를 심으려는 입력이 한 줄로 뭉개진다 - 이 감사의 핵심")
    void forgedLogEventIsFlattened() throws Exception {
        // awslogs 는 이 타임스탬프 형태로 시작하는 줄을 새 이벤트로 인식한다.
        String forged = "정상요청"
                + "\n2026-08-20T03:00:00.000+09:00  INFO 1 --- [main] c.m.b.Fake : 관리자 로그인 성공";

        String out = render(messageConverterFromConfig(), forged);

        assertThat(out).doesNotContain("\n");
        // 남아는 있지만 앞 줄에 이어 붙어서, 독립된 로그 이벤트가 되지 못한다.
        assertThat(out).startsWith("정상요청");
        assertThat(out.split("\\R")).hasSize(1);
    }

    @Test
    @DisplayName("줄바꿈이 없는 평범한 메시지는 그대로 둔다")
    void keepsNormalMessage() throws Exception {
        String msg = "AI 긴급 클립 수신: clipId=12, zoneId=3";
        assertThat(render(messageConverterFromConfig(), msg)).isEqualTo(msg);
    }

    @Test
    @DisplayName("예외 스택은 여러 줄로 남긴다 - 패턴에서 %wEx 를 건드리지 않았다")
    void keepsExceptionPattern() throws Exception {
        assertThat(consolePatternFromConfig()).contains("%wEx");
    }
}
