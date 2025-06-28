package org.example.box;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.repositories.InboxEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class OrderEventListener {
    @Autowired
    private InboxEventRepository inboxEventRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "orders")
    @Transactional
    public void listen(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            String messageKey = record.key();
            String payload = record.value();

            JsonNode json = objectMapper.readTree(payload);
            String eventId = json.get("eventId").asText();

            if (inboxEventRepository.existsByMessageId(eventId)) {
                ack.acknowledge();
                return;
            }

            InboxEvent event = InboxEvent.builder()
                    .messageId(eventId)
                    .eventType("OrderCreated")
                    .payload(payload)
                    .receivedAt(LocalDateTime.now())
                    .build();
            inboxEventRepository.save(event);

            ack.acknowledge();
        } catch (Exception e) {
            throw new RuntimeException("Failed to process Kafka message", e);
        }
    }
}
