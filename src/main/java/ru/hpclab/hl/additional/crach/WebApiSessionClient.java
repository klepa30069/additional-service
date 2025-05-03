package ru.hpclab.hl.additional.crach;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.hpclab.hl.additional.model.Session;
import ru.hpclab.hl.additional.model.Visitor;
import ru.hpclab.hl.additional.service.ObservabilityService;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class WebApiSessionClient {
    private final WebClient coreServiceWebClient;
    private final ObservabilityService observabilityService;

    @CircuitBreaker(name = "CORE_SERVICE", fallbackMethod = "fallback")
    @Retry(name = "CORE_SERVICE")
    public List<Session> findAll() {
        long startTime = System.currentTimeMillis();
        try {
            List<Session> sessions = coreServiceWebClient.get()
                .uri("/sessions")
                .retrieve()
                .bodyToFlux(Session.class)
                .collectList()
                .block();
            return sessions;
        } finally {
            observabilityService.recordTiming(
                "external.core.sessions.get",
                System.currentTimeMillis() - startTime
            );
        }
    }

    private List<Session> fallback(Exception e) {
        System.out.println("Fallback for findAll triggered: " + e.getMessage());
        return Collections.emptyList();
    }
}

