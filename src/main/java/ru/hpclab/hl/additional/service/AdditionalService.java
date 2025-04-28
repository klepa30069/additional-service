package ru.hpclab.hl.additional.service;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.hpclab.hl.additional.model.Session;
import ru.hpclab.hl.additional.model.Visitor;
import ru.hpclab.hl.additional.service.ObservabilityService;

import java.util.*;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdditionalService {
    private final RestTemplate restTemplate;
    private final ObservabilityService observabilityService;

    public List<Session> getSessionsByFio(String fio) {
        long startTime = System.currentTimeMillis();

        try {
            List<Visitor> visitors = getVisitorsWithTiming(fio);

            if (visitors.isEmpty()) {
                return Collections.emptyList();
            }

            List<Session> allSessions = getAllSessionsWithTiming();
            List<Session> result = filterSessions(visitors, allSessions);

            return result;
        } finally {
            observabilityService.recordTiming("service.additionals.get_sessions_by_fio",
                System.currentTimeMillis() - startTime);
        }
    }

    private List<Visitor> getVisitorsWithTiming(String fio) {
        long startTime = System.currentTimeMillis();
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
            observabilityService.recordTiming("service.additionals.get_visitors",
                System.currentTimeMillis() - startTime);
        }
    }

    private List<Session> getAllSessionsWithTiming() {
        long startTime = System.currentTimeMillis();
        try {
            List<Session> sessions = restTemplate.exchange(
                "http://main-service:8080/sessions",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Session>>() {}
            ).getBody();
            return sessions != null ? sessions : Collections.emptyList();
        } finally {
            observabilityService.recordTiming("service.additionals.get_all_sessions",
                System.currentTimeMillis() - startTime);
        }
    }

    private List<Session> filterSessions(List<Visitor> visitors, List<Session> sessions) {
        long startTime = System.currentTimeMillis();

        try {
            Set<UUID> visitorIds = visitors.stream()
                .map(Visitor::getID)
                .collect(Collectors.toSet());

            return sessions.stream()
                .filter(session -> visitorIds.contains(session.getVisitorId()))
                .collect(Collectors.toList());
        } finally {
            observabilityService.recordTiming("servie.additionals.filter_sessions",
                System.currentTimeMillis() - startTime);
        }
    }

    public double calculateAverageDuration(String fio, int month, int year) {
        long startTime = System.currentTimeMillis();

        try {
            List<Visitor> visitors = getVisitorsWithTiming(fio);
            if (visitors.isEmpty()) {
                return 0;
            }

            List<Session> filteredSessions = getAllSessionsWithTiming().stream()
                .filter(s -> visitors.stream()
                    .anyMatch(v -> v.getID().equals(s.getVisitorId())))
                .filter(s -> s.getDate().getMonthValue() == month && s.getDate().getYear() == year)
                .collect(Collectors.toList());

            if (filteredSessions.isEmpty()) {
                return 0;
            }

            double average = filteredSessions.stream()
                .mapToInt(Session::getDuration)
                .average()
                .orElse(0);

            return average;
        } finally {
            observabilityService.recordTiming("service.additionals.calculate_average_duration",
                System.currentTimeMillis() - startTime);
        }
    }
}

