package org.example.box;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.repositories.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OutboxProcessor {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutboxEvents() {
        log.info("Scheduler started");
        outboxEventRepository.findUnprocessedEvents().forEach(event -> {
            try {
                log.info("looking for ProcessedFalseEvents");
                String messageKey = event.getAggregateType() + "_" + event.getAggregateId();
                JsonNode payload = objectMapper.readTree(event.getPayload());
                ((ObjectNode) payload).put("eventId", event.getId().toString());
                log.info("sending with kafkaTemplate");
                var future = kafkaTemplate.send("orders", messageKey, payload.toString());
                log.info("waiting for future");
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
