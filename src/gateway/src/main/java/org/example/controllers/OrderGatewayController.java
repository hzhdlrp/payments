package org.example.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api")
@Tag(name = "Gateway", description = "")
public class OrderGatewayController {

    @Autowired
    private RestTemplate restTemplate;
    private final String orderServiceUrl = "http://order-service:8080/api/orders";

    @PostMapping("/orders")
    @Operation(summary = "создать заказ")
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        return restTemplate.postForEntity(orderServiceUrl, request, Object.class);
    }

    @GetMapping("/orders/user/{userId}")
    @Operation(summary = "получить заказы пользователя")
    public ResponseEntity<?> getUserOrders(@PathVariable Long userId) {
        return restTemplate.getForEntity(orderServiceUrl + "/user/" + userId, Object.class);
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "получить заказ по айди")
    public ResponseEntity<?> getOrder(@PathVariable Long id) {
        return restTemplate.getForEntity(orderServiceUrl + "/" + id, Object.class);
    }
}

@Getter
class CreateOrderRequest {
    private Long userId;
    private BigDecimal amount;
    private String description;
}