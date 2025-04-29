package ru.hpclab.hl.additional.service;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.hpclab.hl.additional.model.Session;
import ru.hpclab.hl.additional.model.Visitor;
import ru.hpclab.hl.additional.cache.VisitorCache;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdditionalService {
    private final MainClient mainClient;
    private final ObservabilityService observabilityService;
    private final VisitorCache visitorCache;

    public Map<String, Map<Integer, Double>> getAllUsersMonthlyAverageDuration() {
        long startTime = System.currentTimeMillis();

        try {
            List<Session> allSessions = mainClient.getAllSessions();

            if (allSessions == null || allSessions.isEmpty()) {
                log.warn("No sessions received from main service");
                return Collections.emptyMap();
            }

            Set<UUID> visitorIds = allSessions.stream()
                .map(Session::getVisitorId)
                .collect(Collectors.toSet());

            Map<UUID, Visitor> visitors = getVisitorsWithCache(visitorIds);

            return allSessions.stream()
                .collect(Collectors.groupingBy(
                    session -> {
                        Visitor visitor = visitors.get(session.getVisitorId());
                        return visitor != null ? visitor.getFio() : "Unknown";
                    },
                    Collectors.groupingBy(
                        session -> session.getDate().getMonthValue(),
                        Collectors.averagingInt(Session::getDuration)
                    )
                ));
        } finally {
            observabilityService.recordTiming(
                "additional.stats.monthly",
                System.currentTimeMillis() - startTime
            );
        }
    }

    private Map<UUID, Visitor> getVisitorsWithCache(Set<UUID> ids) {
        Map<UUID, Visitor> visitors = new HashMap<>();
        long start = System.currentTimeMillis();

        try {
            for (UUID id : ids) {
                long visitorStart = System.currentTimeMillis();
                try {
                    Visitor visitor = visitorCache.get(id);

                    if (visitor == null) {
                        visitor = mainClient.getVisitor(id);
                        if (visitor != null) {
                            visitorCache.put(id, visitor);
                        }
                    }

                    if (visitor != null) {
                        visitors.put(id, visitor);
                    }
                } finally {
                    observabilityService.recordTiming(
                        "external.main.visitors.get.single",
                        System.currentTimeMillis() - visitorStart
                    );
                }
            }
            return visitors;
        } finally {
            observabilityService.recordTiming(
                "external.main.visitors.batch",
                System.currentTimeMillis() - start
            );
        }
    }
}

