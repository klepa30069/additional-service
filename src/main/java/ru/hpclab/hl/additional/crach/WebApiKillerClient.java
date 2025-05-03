package ru.hpclab.hl.additional.crach;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import ru.hpclab.hl.additional.service.ObservabilityService;

@Component
@RequiredArgsConstructor
public class WebApiKillerClient {
    private final WebClient coreServiceWebClient;
    private final ObservabilityService observabilityService;

    public void crashCoreService() {
        long startTime = System.currentTimeMillis();
        try {
            coreServiceWebClient.post()
                .uri("/internal/crash")
                .retrieve()
                .bodyToMono(Void.class)
                .block();
        } finally {
            observabilityService.recordTiming(
                "external.core.crash",
                System.currentTimeMillis() - startTime
            );
        }
    }
}

