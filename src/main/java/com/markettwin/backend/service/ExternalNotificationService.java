package com.markettwin.backend.service;

import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

@Slf4j
@Service
public class ExternalNotificationService {

    // application.yml에서 설정한 수신 전화번호 (내 번호 or 112)
    @Value("${emergency.target-phone}")
    private String targetPhone;

    // 환경변수(IntelliJ)에서 가져올 Solapi 설정값들
    @Value("${solapi.api-key}")
    private String apiKey;

    @Value("${solapi.api-secret}")
    private String apiSecret;

    @Value("${solapi.sender-phone}")
    private String senderPhone;

    private DefaultMessageService messageService;

    // 서버가 켜질 때 한 번만 Solapi 인증 객체를 생성합니다.
    @PostConstruct
    public void init() {
        this.messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecret, "https://api.solapi.com");
    }

    @Async // 이 메서드는 백그라운드에서 실행되어 서버 속도에 영향을 주지 않습니다.
    public void sendEmergencySms(Long zoneId, String alertType) {
        log.info("🚨 [긴급 SMS 발송 시작] 수신처: {}", targetPhone);
        log.info("🚨 내용: 구역 [{}]에서 [{}] 위험 상황이 15초 이상 지속되었습니다. 즉시 확인 바랍니다.", zoneId, alertType);

        try {
            // 실제 SMS 발송 코드 (CoolSMS/Solapi)
            Message message = new Message();
            message.setFrom(senderPhone); // 발신 번호
            message.setTo(targetPhone);   // 수신 번호
            String messageText = String.format(
                    "[망원시장 통합관제센터 긴급보고]\n" +
                            "- 발생 위치: 제 %d구역\n" +
                            "- 상황 분류: %s\n" +
                            "- 조치 결과: 112/119 긴급 출동 요청 및 신고 접수 완료\n\n",
                    zoneId, alertType
            );
            message.setText(messageText);


            // 문자 발송 실행
            this.messageService.sendOne(new SingleMessageSendingRequest(message));

            log.info("🚨 [긴급 SMS 발송 완료]");
        } catch (Exception e) {
            log.error("❌ SMS 발송 실패: {}", e.getMessage());
        }
    }
}
