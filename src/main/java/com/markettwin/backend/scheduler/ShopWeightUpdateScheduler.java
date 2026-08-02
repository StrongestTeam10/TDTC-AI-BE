package com.markettwin.backend.scheduler;

import com.markettwin.backend.service.ShopWeightUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShopWeightUpdateScheduler {

    private final ShopWeightUpdateService shopWeightUpdateService;

    // 매일 오전 10시, 오후 9시에 실행
    @Scheduled(cron = "0 0 10,21 * * *")
    public void updateShopWeights() {
        log.info("Triggered ShopWeightUpdateScheduler at scheduled time.");
        shopWeightUpdateService.updateAllStallWeights();
    }
}
