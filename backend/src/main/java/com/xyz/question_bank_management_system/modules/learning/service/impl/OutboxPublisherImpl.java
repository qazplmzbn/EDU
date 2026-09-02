package com.xyz.question_bank_management_system.modules.learning.service.impl;
import com.xyz.question_bank_management_system.modules.agent.mapper.PersonalizedResourceMapper;
import com.xyz.question_bank_management_system.modules.learning.service.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
@Service @RequiredArgsConstructor
@ConditionalOnProperty(prefix="app.outbox",name="enabled",havingValue="true")
public class OutboxPublisherImpl implements OutboxPublisher {
 private final PersonalizedResourceMapper mapper;
 /**
  * Resource interactions are consumed synchronously by ProfileEvidenceConsumer.
  * Until an external transport is introduced, this scheduler must never claim
  * that an unconsumed event was published.
  */
 @Override @Scheduled(fixedDelayString="${app.outbox.publish-delay-ms:5000}")
 public int publishPending(){return 0;}
}
