package org.example.box;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.repositories.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class OutboxProcessor {
    @Autowired
    private OutboxEventRepository outboxEventRepository;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutboxEvents() {
        outboxEventRepository.findUnprocessedEvents().forEach(event -> {
            try {
                String topic = "payments-results";
                String messageKey = event.getAggregateType() + "_" + event.getAggregateId();

                CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, messageKey, event.getPayload());
                future.whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Sent message: {}", result.getProducerRecord().value());
                    } else {
                        log.error("Failed to send message", ex);
                    }
                });
                event.setProcessed(true);
                outboxEventRepository.save(event);
            } catch (Exception e) {
                log.error("Failed to process outbox event: {}", event.getId(), e);
            }
        });
    }
}
