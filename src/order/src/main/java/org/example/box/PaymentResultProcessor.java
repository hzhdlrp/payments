package org.example.box;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.orders.Order;
import org.example.orders.OrderStatus;
import org.example.repositories.InboxEventRepository;
import org.example.repositories.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentResultProcessor {
    @Autowired
    private InboxEventRepository inboxEventRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processPaymentResults() {
        inboxEventRepository.findByProcessedFalse().forEach(event -> {
            try {
                JsonNode json = objectMapper.readTree(event.getPayload());
                Long orderId = json.get("orderId").asLong();
                String status = json.get("status").asText();

                Order order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new RuntimeException("Order not found"));

                if ("SUCCESS".equals(status)) {
                    order.setStatus(OrderStatus.FINISHED);
                } else {
                    order.setStatus(OrderStatus.CANCELLED);
                    order.setDescription(json.get("reason").asText());
                }

                orderRepository.save(order);
                event.setProcessed(true);
                inboxEventRepository.save(event);
            } catch (Exception e) {
                log.error("Failed to process outbox event: {}", event.getId(), e);
            }
        });
    }
}
