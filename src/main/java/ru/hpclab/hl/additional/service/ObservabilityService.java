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

    @Scheduled(fixedRate = 10_000)
    public void logMetrics() {
        if (metrics.isEmpty()) {
            log.info("No metrics collected yet");
            return;
        }

        log.info("=== Performance Metrics Report ===");
        log.info("=== Last 10 seconds ===");
        printStatsForPeriod(10_000);

        log.info("=== Last 30 seconds ===");
        printStatsForPeriod(30_000);

        log.info("=== Last 1 minute ===");
        printStatsForPeriod(60_000);

        log.info("=================================");
        log.info("[CACHE] Visitor Cache - Size: {}", visitorCache.size());
        log.info("=================================");
    }

    private void printStatsForPeriod(long periodMs) {
        metrics.forEach((name, stats) -> {
            TimingStats.StatsSnapshot snapshot = stats.getStats(periodMs);
            if (snapshot.count > 0) {
                log.info("[{}] count={}, avg={}ms, min={}ms, max={}ms",
                    name,
                    snapshot.count,
                    snapshot.avg,
                    snapshot.min,
                    snapshot.max);
            }
        });
    }

    public TimingStats getMetric(String name) {
        return metrics.get(name);
    }
}
