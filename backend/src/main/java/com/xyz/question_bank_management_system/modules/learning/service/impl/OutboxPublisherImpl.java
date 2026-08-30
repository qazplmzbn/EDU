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
 @Override @Scheduled(fixedDelayString="${app.outbox.publish-delay-ms:5000}")
 public int publishPending(){int n=0;for(var e:mapper.pendingOutbox(100))n+=mapper.markOutboxPublished(e.getId());return n;}
}
