package org.example.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "")
@Slf4j
public class AccountsController {

    @Autowired
    private RestTemplate restTemplate;
    private final String paymentServiceUrl = "http://payment-service:8080/api/payments";

    @PostMapping
    @Operation(summary = "создать аккаунт")
    public ResponseEntity<?> createAccount(@RequestParam Long userId,
                                           @RequestParam BigDecimal amount) {
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("userId", String.valueOf(userId));
            params.add("amount", String.valueOf(amount));
            String url = paymentServiceUrl + "/accounts";
            var response = restTemplate.postForEntity(url, params, String.class);
            log.info("Payment Service response: {}", response.getStatusCode());
            return response;
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/balance")
    @Operation(summary = "узнать баланс аккаунта")
    public ResponseEntity<?> getBalance(@RequestParam Long userId) {
        try {
            String url = paymentServiceUrl + "/balance?userId=" + userId;
            return restTemplate.getForEntity(url, BigDecimal.class);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
