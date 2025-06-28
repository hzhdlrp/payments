package org.example.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import lombok.Getter;
import org.example.orders.Order;
import org.example.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Обращение к сервису заказов")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PostMapping
    @Operation(summary = "создать заказ")
    public ResponseEntity<Order> createOrder(
            @RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrder(
                request.getUserId(),
                request.getAmount(),
                request.getDescription()
        );
        return ResponseEntity.ok(order);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "узнать заказы пользователя")
    public ResponseEntity<?> getUserOrders(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(orderService.getUserOrders(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("order service error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "получить заказ по айди")
    public ResponseEntity<?> getOrder(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(orderService.getOrder(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("order service error: " + e.getMessage());
        }
    }
}

@Getter
class CreateOrderRequest {
    private Long userId;
    private BigDecimal amount;
    private String description;
}
