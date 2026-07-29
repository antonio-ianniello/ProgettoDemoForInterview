package com.example.usermanagement.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxEventPoller {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxEventPoller.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;

    public OutboxEventPoller(
        OutboxEventRepository outboxEventRepository,
        ApplicationEventPublisher applicationEventPublisher,
        ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:5000}")
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> pending = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        for (OutboxEvent outboxEvent : pending) {
            try {
                UserCreatedEvent event = objectMapper.readValue(outboxEvent.getPayload(), UserCreatedEvent.class);
                applicationEventPublisher.publishEvent(event);
                outboxEvent.setStatus(OutboxStatus.PROCESSED);
                outboxEvent.setProcessedAt(Instant.now());
                LOGGER.info("Outbox event {} processed: type={}, aggregateId={}", outboxEvent.getId(), outboxEvent.getEventType(), outboxEvent.getAggregateId());
            } catch (Exception e) {
                LOGGER.error("Failed to process outbox event {}: {}", outboxEvent.getId(), e.getMessage(), e);
                outboxEvent.setStatus(OutboxStatus.FAILED);
            }
            outboxEventRepository.save(outboxEvent);
        }
    }
}
