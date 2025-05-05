package ru.hpclab.hl.additional.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.hpclab.hl.additional.model.Session;
import ru.hpclab.hl.additional.model.Visitor;
import ru.hpclab.hl.additional.cache.RedisVisitorCache;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AdditionalService {
    private final MainClient mainClient;
    private final RedisVisitorCache visitorCache;
    private final ObservabilityService observabilityService;

    public AdditionalService(MainClient mainClient,
                           RedisVisitorCache visitorCache,
                           ObservabilityService observabilityService) {
        this.mainClient = mainClient;
        this.visitorCache = visitorCache;
        this.observabilityService = observabilityService;
    }

    public Map<String, Map<Integer, Double>> getAllUsersMonthlyAverageDuration() {
        long startTime = System.currentTimeMillis();

        try {
            List<Session> allSessions = getAllSessionsWithMetrics();

            if (allSessions == null || allSessions.isEmpty()) {
                log.warn("No sessions received from main service");
                return Collections.emptyMap();
            }

            Set<UUID> visitorIds = extractUniqueVisitorIds(allSessions);
            Map<UUID, Visitor> visitors = getVisitorsWithCache(visitorIds);

            return calculateMonthlyAverages(allSessions, visitors);
        } finally {
            observabilityService.recordTiming(
                "additional.stats.monthly",
                System.currentTimeMillis() - startTime
            );
        }
    }

    private List<Session> getAllSessionsWithMetrics() {
        long startTime = System.currentTimeMillis();

        try {
            List<Session> sessions = mainClient.getAllSessions();
            log.debug("Retrieved {} sessions from main service", sessions.size());
            return sessions;
        } finally {
            observabilityService.recordTiming(
                "additional.stats.get_all_sessions",
                System.currentTimeMillis() - startTime
            );
        }
    }

    private Set<UUID> extractUniqueVisitorIds(List<Session> sessions) {
        return sessions.stream()
                .map(Session::getVisitorId)
                .collect(Collectors.toSet());
    }

    private Map<UUID, Visitor> getVisitorsWithCache(Set<UUID> ids) {
        Map<UUID, Visitor> visitors = new HashMap<>();
        long batchStart = System.currentTimeMillis();

        try {
            for (UUID id : ids) {
                long singleStart = System.currentTimeMillis();
                try {
                    Visitor visitor = getVisitorWithCache(id);
                    if (visitor != null) {
                        visitors.put(id, visitor);
                    }
                } finally {
                    observabilityService.recordTiming(
                        "external.main.visitors.get.single",
                        System.currentTimeMillis() - singleStart
                    );
                }
            }
            return visitors;
        } finally {
            observabilityService.recordTiming(
                "external.main.visitors.batch",
                System.currentTimeMillis() - batchStart
            );
        }
    }

    private Visitor getVisitorWithCache(UUID visitorId) {
        log.debug("Checking cache for visitor ID: {}", visitorId);
        Visitor visitor = visitorCache.get(visitorId);

        if (visitor == null) {
            log.debug("Cache miss, fetching visitor ID: {} from main service", visitorId);
            visitor = mainClient.getVisitor(visitorId);
            if (visitor != null) {
                log.debug("Caching visitor ID: {}", visitorId);
                visitorCache.put(visitorId, visitor);
            }
        }
        return visitor;
    }

    private Map<String, Map<Integer, Double>> calculateMonthlyAverages(
            List<Session> sessions,
            Map<UUID, Visitor> visitors) {

        return sessions.stream()
            .collect(Collectors.groupingBy(
                session -> getVisitorName(session, visitors),
                Collectors.groupingBy(
                    session -> session.getDate().getMonthValue(),
                    Collectors.averagingInt(Session::getDuration)
                )
            ));
    }

    private String getVisitorName(Session session, Map<UUID, Visitor> visitors) {
        Visitor visitor = visitors.get(session.getVisitorId());
        return visitor != null ? visitor.getFio() : "Unknown";
    }
}

