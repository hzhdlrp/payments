package org.example.box;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.repositories.InboxEventRepository;
import org.example.services.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class InboxProcessor {
    @Autowired
    private InboxEventRepository inboxEventRepository;
    @Autowired
    private PaymentService paymentService;

    @Transactional
    @Scheduled(fixedDelay = 5000)
    public void processInboxEvents() {
        List<InboxEvent> events = inboxEventRepository.findByProcessedFalse();
        events.forEach(event -> processSingleEvent(event));
    }

    @Transactional
    public void processSingleEvent(InboxEvent event) {
        try {
            paymentService.processPaymentEvent(event.getPayload());
            event.setProcessed(true);
            log.info("event_str{}", event);
            inboxEventRepository.save(event);
            log.info("Event processed: {}", event.getId());
        } catch (Exception e) {
            log.error("Failed to process event: {}", event.getId(), e);
        }
    }
}
