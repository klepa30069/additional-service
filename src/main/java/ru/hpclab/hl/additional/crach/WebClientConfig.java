package ru.hpclab.hl.additional.crach;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.time.Instant;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClientRequestException;

@Configuration
public class WebClientConfig {

    @Value("${main.service.url}")
    private String mainServiceUrl;

    @Bean
    public WebClient coreServiceWebClient() {
        return WebClient.builder()
            .baseUrl(mainServiceUrl)
            .filter(this::errorHandlingFilter)
            .build();
    }

    private Mono<ClientResponse> errorHandlingFilter(ClientRequest request, ExchangeFunction next) {
        long startTime = System.currentTimeMillis();

        return next.exchange(request)
            .doOnSuccess(response -> {
                if (response.statusCode().isError()) {
                    System.err.printf("[%s] HTTP %d ERROR | URL: %s | Time: %dms%n",
                        Instant.now().toString(),
                        response.statusCode().value(),
                        request.url(),
                        System.currentTimeMillis() - startTime);
                }
            })
            .onErrorResume(WebClientRequestException.class, ex -> {
                System.err.printf("[%s] HTTP 503 ERROR | URL: %s | Type: %s | Time: %dms | Message: %s%n",
                    Instant.now().toString(),
                    request.url(),
                    ex.getClass().getSimpleName(),
                    System.currentTimeMillis() - startTime,
                    ex.getMessage());

                return Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE)
                    .header("Content-Type", "text/plain")
                    .body("Main service unavailable: " + ex.getMessage())
                    .build());
            });
    }
}
