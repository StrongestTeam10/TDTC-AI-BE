package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.Market;
import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.domain.entity.Zone;
import com.markettwin.backend.repository.MarketRepository;
import com.markettwin.backend.repository.UserRepository;
import com.markettwin.backend.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalNotificationService {

    private final ZoneRepository zoneRepository;
    private final MarketRepository marketRepository;
    private final UserRepository userRepository;

    @Value("${solapi.api-key}")
    private String apiKey;

    @Value("${solapi.api-secret}")
    private String apiSecret;

    @Value("${solapi.sender-phone}")
    private String senderPhone;

    private DefaultMessageService messageService;

    @PostConstruct
    public void init() {
        this.messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecret, "https://api.solapi.com");
    }

    @Async
    public void sendEmergencySms(Long zoneId, String alertType) {
        log.info("🚨 [긴급 SMS 발송 시작] 구역: {}", zoneId);

        try {
            Zone zone = zoneRepository.findById(zoneId).orElse(null);
            if (zone == null) return;

            Market market = marketRepository.findById(zone.getMarketId()).orElse(null);
            if (market == null) return;

            // 1. 당직자(isDuty=true) 목록 가져오기
            List<User> dutyUsers = userRepository.findByMarketCodeAndIsDutyTrue(market.getMarketCode());

            // 2. 전화번호가 '실제로 있는' 사람만 추려내기
            List<User> targetUsers = new ArrayList<>();
            for (User user : dutyUsers) {
                if (user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank()) {
                    targetUsers.add(user);
                }
            }

            // 3. [방어 로직] 당직자가 아예 없거나, 전화번호 있는 당직자가 0명인 경우!
            if (targetUsers.isEmpty()) {
                log.warn("🚨 수신 가능한 당직자가 없습니다. [방어 로직] 관제요원을 탐색합니다. (시장: {})", market.getMarketName());

                // 해당 시장의 관제요원(ROL02) 목록 조회
                List<User> controlStaffs = userRepository.findByMarketCodeAndRulesCodeOrderByCreatedAtDesc(market.getMarketCode(), "ROL02");

                for (User staff : controlStaffs) {
                    if (staff.getPhoneNumber() != null && !staff.getPhoneNumber().isBlank()) {
                        targetUsers.add(staff);
                    }
                }

                // 관제요원 중에도 전화번호가 있는 사람이 없다면 발송 포기
                if (targetUsers.isEmpty()) {
                    log.error("❌ SMS 발송 실패: 당직자도, 전화번호가 등록된 관제요원도 없습니다.");
                    return;
                }
            }

            // 4. 최종 수신자(targetUsers) 모두에게 순차적으로 SMS 발송
            for (User user : targetUsers) {
                Message message = new Message();
                message.setFrom(senderPhone);
                message.setTo(user.getPhoneNumber());

                String messageText = String.format(
                        "[%s 통합관제센터 긴급보고]\n" +
                                "- 발생 위치: 제 %d구역\n" +
                                "- 상황 분류: %s\n" +
                                "- 조치 결과: 112/119 긴급 출동 요청 및 신고 접수 완료\n\n",
                        market.getMarketName(), zoneId, alertType
                );
                message.setText(messageText);

                this.messageService.sendOne(new SingleMessageSendingRequest(message));

                log.info("🚨 [긴급 SMS 발송 완료] 수신자: {} ({})", user.getName(), user.getPhoneNumber());
            }

        } catch (Exception e) {
            log.error("❌ SMS 발송 실패: {}", e.getMessage());
        }
    }
}
