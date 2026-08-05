package com.markettwin.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ExternalNotificationService {

    // application.yml에서 설정한 전화번호(내 번호 or 112)를 자동으로 땡겨옵니다.
    @Value("${emergency.target-phone}")
    private String targetPhone;

    @Async // 이 메서드는 백그라운드에서 실행되어 서버 속도에 영향을 주지 않습니다.
    public void sendEmergencySms(Long zoneId, String alertType) {
        log.info("🚨 [긴급 SMS 발송 시작] 수신처: {}", targetPhone);
        log.info("🚨 내용: 구역 ID [{}]에서 [{}] 위험 상황이 15초 이상 지속되었습니다. 즉시 확인 바랍니다.", zoneId, alertType);

        // TODO: 향후 이곳에 실제 SMS 발송 코드 (CoolSMS 등) 3~4줄만 넣으시면 됩니다.
        // coolsmsApi.sendMessage(targetPhone, "위험 발생!");

        log.info("🚨 [긴급 SMS 발송 완료]");
    }
}
