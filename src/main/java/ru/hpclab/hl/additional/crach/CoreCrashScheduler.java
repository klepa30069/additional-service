package ru.hpclab.hl.additional.crach;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CoreCrashScheduler {
    private final WebApiKillerClient webApiKillerClient;

    @Scheduled(fixedDelayString = "${service.scheduler.core.crash.delay}")
    public void callCrashEndpoint() {
        webApiKillerClient.crashCoreService();
    }
}
