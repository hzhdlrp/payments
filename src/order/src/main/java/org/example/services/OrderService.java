package org.example.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.example.orders.Order;
import org.example.orders.OrderStatus;
import org.example.box.OutboxEvent;
import org.example.repositories.OrderRepository;
import org.example.repositories.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

@Service
@Transactional
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ObjectMapper objectMapper;


    public Order createOrder(Long userId, BigDecimal amount, String description) {
        Order order = Order.builder()
                .amount(amount)
                .description(description)
                .userId(userId)
                .status(OrderStatus.NEW)
                .build();
        order = orderRepository.save(order);

        createOrderCreatedEvent(order);

        return order;
    }

    private void createOrderCreatedEvent(Order order) {
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateId(order.getId().toString())
                    .aggregateType("Order")
                    .createdAt(LocalDateTime.now())
                    .eventType("OrderCreated")
                    .payload(objectMapper.writeValueAsString(
                                Map.of(
                                        "orderId", order.getId(),
                                        "userId", order.getUserId(),
                                        "amount", order.getAmount()
                                )
                            )
                    )
                    .processed(false)
                    .build();
            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create outbox event", e);
        }
    }

    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public void updateOrderStatus(Long orderId, OrderStatus status, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(status);
        if (reason != null) {
            order.setDescription(reason);
        }

        orderRepository.save(order);
    }
}
