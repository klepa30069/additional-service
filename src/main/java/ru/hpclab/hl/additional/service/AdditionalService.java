package ru.hpclab.hl.additional.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.hpclab.hl.additional.model.Session;
import ru.hpclab.hl.additional.model.Visitor;

import java.util.*;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AdditionalService {
    private final RestTemplate restTemplate;
    private final ObservabilityService observabilityService;

    public AdditionalService(RestTemplate restTemplate, ObservabilityService observabilityService) {
        this.restTemplate = restTemplate;
        this.observabilityService = observabilityService;
    }

    public List<Session> getSessionsByFio(String fio) {
        long startTime = System.currentTimeMillis();

        try {
            List<Visitor> visitors = getVisitorsWithTiming(fio);

            if (visitors.isEmpty()) {
                log.info("No visitors found for fio: {}", fio);
                return Collections.emptyList();
            }

            List<Session> allSessions = getAllSessionsWithTiming();
            List<Session> result = filterSessions(visitors, allSessions);

            log.info("Successfully found {} sessions for fio: {}", result.size(), fio);
            return result;
        } catch (Exception e) {
            log.error("Error getting sessions by fio: {}", fio, e);
            throw e;
        } finally {
            observabilityService.recordTiming("getSessionsByFio",
                System.currentTimeMillis() - startTime);
        }
    }

    private List<Visitor> getVisitorsWithTiming(String fio) {
        long start = System.currentTimeMillis();
        try {
            List<Visitor> visitors = restTemplate.exchange(
                "http://main-service:8080/visitors",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Visitor>>() {}
            ).getBody();

            return visitors != null ? visitors.stream()
                .filter(visitor -> visitor.getFio().equalsIgnoreCase(fio))
                .collect(Collectors.toList()) : Collections.emptyList();
        } finally {
            observabilityService.recordTiming("getVisitors",
                System.currentTimeMillis() - start);
        }
    }

    private List<Session> getAllSessionsWithTiming() {
        long start = System.currentTimeMillis();
        try {
            List<Session> sessions = restTemplate.exchange(
                "http://main-service:8080/sessions",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Session>>() {}
            ).getBody();
            return sessions != null ? sessions : Collections.emptyList();
        } finally {
            observabilityService.recordTiming("getAllSessions",
                System.currentTimeMillis() - start);
        }
    }

    private List<Session> filterSessions(List<Visitor> visitors, List<Session> sessions) {
        Set<UUID> visitorIds = visitors.stream()
            .map(Visitor::getID)
            .collect(Collectors.toSet());

        return sessions.stream()
            .filter(session -> visitorIds.contains(session.getVisitorId()))
            .collect(Collectors.toList());
    }

    public double calculateAverageDuration(String fio, int month, String code) {
        long startTime = System.currentTimeMillis();

        try {
            List<Visitor> visitors = getVisitorsWithTiming(fio);
            if (visitors.isEmpty()) {
                return 0;
            }

            List<Session> sessions = getAllSessionsWithTiming().stream()
                .filter(s -> visitors.stream()
                    .anyMatch(v -> v.getID().equals(s.getVisitorId())))
                .collect(Collectors.toList());

            if (sessions.isEmpty()) {
                return 0;
            }

            return sessions.stream()
                .mapToInt(Session::getDuration)
                .average()
                .orElse(0);
        } finally {
            observabilityService.recordTiming("calculateAverageDuration",
                System.currentTimeMillis() - startTime);
        }
    }
}
