package ru.hpclab.hl.additional.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.hpclab.hl.additional.metrics.TimingStats;
import ru.hpclab.hl.additional.cache.VisitorCache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
@Slf4j
public class ObservabilityService {
    private final Map<String, TimingStats> metrics = new ConcurrentHashMap<>();
    private final VisitorCache visitorCache;

    public ObservabilityService(VisitorCache visitorCache) {
        this.visitorCache = visitorCache;
    }

    public void recordTiming(String metricName, long durationMs) {
        metrics.computeIfAbsent(metricName, k -> new TimingStats())
               .addRecord(durationMs);
    }

    @Scheduled(fixedRate = 60_000)
    public void logMetrics() {
        if (metrics.isEmpty()) {
            log.info("No metrics collected yet");
            return;
        }

        log.info("=== Performance Metrics Report ===");
        metrics.forEach((name, stats) -> {
            log.info("[{}] {}", name, stats.getSummary());
        });
        log.info("=================================");
        log.info("[CACHE] Visitor Cache - Size: {}",
            visitorCache.size());
        log.info("=================================");
    }

    public TimingStats getMetric(String name) {
        return metrics.get(name);
    }
}

