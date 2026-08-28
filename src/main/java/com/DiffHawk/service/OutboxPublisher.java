package com.DiffHawk.service;

import com.DiffHawk.domain.OutboxEvent;
import com.DiffHawk.domain.OutboxStatus;
import com.DiffHawk.repository.OutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class OutboxPublisher {
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    @Value("${kafka.topics.pr-review-requested}")
    private String topicName;

    public OutboxPublisher(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }
    @Scheduled(fixedDelay = 5000)
    public void processOutBoxEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.findByStatus(OutboxStatus.PENDING);
        for (OutboxEvent event : pendingEvents) {
            try {
                Map<String, Object> payload = objectMapper.readValue(event.getPayLoad(), Map.class);
                Map<String, Object> repo = (Map<String, Object>) payload.get("repository");
                String githubRepoId = String.valueOf(((Number) repo.get("id")).longValue());
                kafkaTemplate.send(topicName, githubRepoId, event.getPayLoad())
                        .whenComplete((res, ex) -> {
                            if (ex == null) {
                                event.setStatus(OutboxStatus.PUBLISHED);
                                event.setPublishedAt(Instant.now());
                                outboxRepository.save(event);
                            }else {
                                event.setStatus(OutboxStatus.FAILED);
                                outboxRepository.save(event);
                            }
                        });
            } catch (Exception e) {
                event.setStatus(OutboxStatus.FAILED);
                outboxRepository.save(event);
            }
        }
    }
}
