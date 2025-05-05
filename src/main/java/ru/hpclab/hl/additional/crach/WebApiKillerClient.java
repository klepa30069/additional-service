package ru.hpclab.hl.additional.crach;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import ru.hpclab.hl.additional.service.ObservabilityService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClientRequestException;

@Component
@RequiredArgsConstructor
public class WebApiKillerClient {
    private final WebClient coreServiceWebClient;
    private final ObservabilityService observabilityService;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                            .withZone(ZoneId.systemDefault());

    public void crashCoreService() {
        long startTime = System.currentTimeMillis();
        String timestamp = TIME_FORMATTER.format(Instant.now());
        final AtomicInteger errorCode = new AtomicInteger(-1);

        try {
            coreServiceWebClient.post()
                .uri("/internal/crash")
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(response -> {
                    int statusCode = response.getStatusCode().value();
                    errorCode.set(statusCode);
                    long duration = System.currentTimeMillis() - startTime;

                    System.out.printf("[%s] Sucsseful. Code: %d | Time: %d ms%n",
                            timestamp, statusCode, duration);

                    observabilityService.recordTiming("external.core.crash", duration);
                })
                .doOnError(WebClientResponseException.class, ex -> {
                    errorCode.set(ex.getStatusCode().value());
                    long duration = System.currentTimeMillis() - startTime;
                    String responseBody = ex.getResponseBodyAsString();

                    System.err.printf("[%s] Error HTTP. Code: %d | Time: %d ms | Body: %s | Message: %s%n",
                            timestamp, errorCode.get(), duration,
                            responseBody, ex.getMessage());
                })
                .doOnError(IOException.class, ex -> {
                    errorCode.set(resolveErrorCode(ex));
                    long duration = System.currentTimeMillis() - startTime;

                    System.err.printf("[%s] Network error. Code: %d | Type: %s | Time: %d ms | Message: %s%n",
                            timestamp, errorCode.get(), ex.getClass().getSimpleName(),
                            duration, ex.getMessage());
                })
                .doOnError(Exception.class, ex -> {
                    errorCode.set(resolveErrorCode(ex));
                    long duration = System.currentTimeMillis() - startTime;

                    System.err.printf("[%s] System error. Code: %d | Type: %s | Time: %d ms | Message: %s%n",
                            timestamp, errorCode.get(), ex.getClass().getSimpleName(),
                            duration, ex.getMessage());
                })
                .block();
        } catch (Exception e) {
            errorCode.set(resolveErrorCode(e));
            long duration = System.currentTimeMillis() - startTime;

            System.err.printf("[%s] Unexpectied error. Code: %d | Type: %s | Time: %d ms | Message: %s%n",
                    timestamp, errorCode.get(), e.getClass().getSimpleName(),
                    duration, e.getMessage());
        } finally {
            System.out.printf("[%s] Itog Code error: %d%n", timestamp, errorCode.get());
        }
    }

    private int resolveErrorCode(Throwable ex) {
        if (ex instanceof ConnectException) return 1001;
        if (ex instanceof SocketTimeoutException) return 1002;
        if (ex instanceof UnknownHostException) return 1003;
        if (ex instanceof SSLException) return 1004;
        if (ex instanceof WebClientRequestException) return 1005;
        return 1999;
    }
}
