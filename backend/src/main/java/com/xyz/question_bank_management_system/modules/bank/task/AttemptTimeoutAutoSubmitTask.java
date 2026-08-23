package com.xyz.question_bank_management_system.modules.bank.task;

import com.xyz.question_bank_management_system.modules.bank.entity.QbAttempt;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAttemptMapper;
import com.xyz.question_bank_management_system.modules.bank.service.AttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttemptTimeoutAutoSubmitTask {

    private final QbAttemptMapper attemptMapper;
    private final AttemptService attemptService;

    @Value("${qb.attempt.timeout-scan-batch-size:100}")
    private int batchSize;

    @Scheduled(
            initialDelayString = "${qb.attempt.timeout-scan-initial-delay-ms:15000}",
            fixedDelayString = "${qb.attempt.timeout-scan-delay-ms:15000}"
    )
    public void autoSubmitExpiredAttempts() {
        int safeBatchSize = Math.max(batchSize, 1);
        while (true) {
            List<QbAttempt> expiredAttempts = attemptMapper.selectExpiredInProgressAttempts(LocalDateTime.now(), safeBatchSize);
            if (expiredAttempts.isEmpty()) {
                return;
            }
            for (QbAttempt attempt : expiredAttempts) {
                try {
                    attemptService.submitAttempt(attempt.getId(), attempt.getUserId());
                } catch (BizException ex) {
                    log.warn("Skip auto submit for expired attempt {}: {}", attempt.getId(), ex.getMessage());
                } catch (RuntimeException ex) {
                    log.error("Auto submit expired attempt failed, attemptId={}", attempt.getId(), ex);
                }
            }
            if (expiredAttempts.size() < safeBatchSize) {
                return;
            }
        }
    }
}
