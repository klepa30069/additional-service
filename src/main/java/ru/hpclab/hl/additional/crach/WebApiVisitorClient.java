package ru.hpclab.hl.additional.crach;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.hpclab.hl.additional.model.Visitor;
import ru.hpclab.hl.additional.model.Session;
import ru.hpclab.hl.additional.service.ObservabilityService;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WebApiVisitorClient {
    private final WebClient coreServiceWebClient;
    private final ObservabilityService observabilityService;

    @CircuitBreaker(name = "CORE_SERVICE", fallbackMethod = "fallback")
    @Retry(name = "CORE_SERVICE")
    public Visitor findVisitorById(UUID id) {
        long startTime = System.currentTimeMillis();
        try {
            Visitor visitor = coreServiceWebClient.get()
                .uri("/visitors/{id}", id)
                .retrieve()
                .bodyToMono(Visitor.class)
                .block();
            return visitor;
        } finally {
            observabilityService.recordTiming(
                "external.core.visitors.get",
                System.currentTimeMillis() - startTime
            );
        }
    }

    private Visitor fallback(UUID id, Exception e) {
        System.out.println("Fallback for findVisitorById triggered: " + e.getMessage());
        return null;
    }
}

