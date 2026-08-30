package com.xyz.question_bank_management_system.modules.competency.task;

import com.xyz.question_bank_management_system.modules.competency.service.CompetencyLandingService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.landing.boss", name = "sync-enabled", havingValue = "true")
@RequiredArgsConstructor
public class CompetencyLandingSyncTask {

    private final CompetencyLandingService competencyLandingService;

    @Scheduled(
            initialDelayString = "${app.landing.boss.sync-initial-delay-ms:20000}",
            fixedDelayString = "${app.landing.boss.sync-delay-ms:900000}"
    )
    public void syncBossJobs() {
        try {
            competencyLandingService.runScheduledSync();
        } catch (RuntimeException ex) {
            log.error("Scheduled competency landing sync failed", ex);
        }
    }
}
