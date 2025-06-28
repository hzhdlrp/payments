package org.example.configurations;

import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;
import org.slf4j.Logger;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .additionalInterceptors(new LoggingInterceptor())
                .build();
    }
}

class LoggingInterceptor implements ClientHttpRequestInterceptor {
    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) {
        log.info("Calling {} {}", request.getMethod(), request.getURI());
        try {
            ClientHttpResponse response = execution.execute(request, body);
            log.info("Response: {}", response.getStatusCode());
            return response;
        } catch (IOException e) {
            log.error("Connection failed to {}: {}", request.getURI(), e.getMessage());
            throw new RuntimeException(e);
        }
    }
}