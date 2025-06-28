package org.example.box;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.repositories.InboxEventRepository;
import org.example.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class PaymentResultListener {
    @Autowired
    private InboxEventRepository inboxEventRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OrderService orderService;

    @KafkaListener(topics = "payments-results")
    @Transactional
    public void listen(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            String messageKey = record.key();
            String payload = record.value();

            JsonNode json = objectMapper.readTree(payload);
            log.info("got message with key {}", messageKey);
            if (inboxEventRepository.existsByMessageId(messageKey)) {
                ack.acknowledge();
                log.info("message with id {} already exists", messageKey);
                return;
            }

            InboxEvent event = InboxEvent.builder()
                    .messageId(messageKey)
                    .eventType(json.get("status").asText().equals("SUCCESS") ?
                            "PaymentSucceeded" : "PaymentFailed")
                    .payload(payload)
                    .receivedAt(LocalDateTime.now())
                    .build();
            log.info("saving event with message key {}", messageKey);
            inboxEventRepository.save(event);
            ack.acknowledge();
        } catch (Exception e) {
            throw new RuntimeException("Failed to process payment result", e);
        }
    }
}
