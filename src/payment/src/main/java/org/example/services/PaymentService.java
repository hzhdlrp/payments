package org.example.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.example.accounts.Account;
import org.example.box.OutboxEvent;
import org.example.repositories.AccountRepository;
import org.example.repositories.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@Transactional
public class PaymentService {
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private OutboxEventRepository outboxEventRepository;
    @Autowired
    private ObjectMapper objectMapper;

    public Account createAccount(Long userId, BigDecimal amount) {
        if (accountRepository.existsByUserId(userId)) {
            throw new RuntimeException("Account already exists");
        }

        Account account = Account.builder()
                .userId(userId)
                .balance(amount)
                .build();
        return accountRepository.save(account);
    }

    public Account deposit(Long userId, BigDecimal amount) {
        Account account = accountRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (account.getBalance().add(amount).compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Not enough balance");
        }
        account.setBalance(account.getBalance().add(amount));
        return accountRepository.save(account);
    }

    public BigDecimal getBalance(Long userId) {
        return accountRepository.findByUserId(userId)
                .map(Account::getBalance)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    public void processPaymentEvent(String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);
            Long orderId = json.get("orderId").asLong();
            Long userId = json.get("userId").asLong();
            BigDecimal amount = new BigDecimal(json.get("amount").asText());

            Account account = accountRepository.findByUserIdWithLock(userId)
                    .orElseThrow(() -> {
                        createPaymentFailedEvent(orderId, userId, "Account not found");
                        return new RuntimeException("Account not found");
                    });

            if (account.getBalance().compareTo(amount) < 0) {
                createPaymentFailedEvent(orderId, userId, "Insufficient funds");
                return;
            }

            account.setBalance(account.getBalance().subtract(amount));
            accountRepository.save(account);

            createPaymentSuccessEvent(orderId, userId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process payment: " + e.getMessage());
        }
    }

    private void createPaymentSuccessEvent(Long orderId, Long userId) {
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Payment")
                    .aggregateId(orderId.toString())
                    .createdAt(LocalDateTime.now())
                    .eventType("PaymentSucceed")
                    .payload(objectMapper.writeValueAsString(Map.of(
                                    "orderId", orderId,
                                    "userId", userId,
                                    "status", "SUCCESS"
                            )))
                    .build();
            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create success event", e);
        }
    }

    private void createPaymentFailedEvent(Long orderId, Long userId, String reason) {
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateId(orderId.toString())
                    .aggregateType("Payment")
                    .eventType("PaymentFailed")
                    .payload(objectMapper.writeValueAsString(Map.of(
                            "orderId", orderId,
                            "userId", userId,
                            "status", "FAILED",
                            "reason", reason
                    )))
                    .createdAt(LocalDateTime.now())
                    .build();
            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create failed event", e);
        }
    }
}
