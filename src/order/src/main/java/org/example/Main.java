package org.example;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.repositories.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling
@Slf4j
@EnableTransactionManagement
@EnableKafka
public class Main {
    @Autowired
    private OutboxEventRepository outboxEventRepository;

    public static void main(String[] args)
    {
        SpringApplication.run(Main.class, args);
    }



    @PostConstruct
    public void init() {
        log.info("Found {} unprocessed events",
                outboxEventRepository.findUnprocessedEvents().size());
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}