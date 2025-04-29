package ru.hpclab.hl.additional.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.hpclab.hl.additional.model.Session;
import ru.hpclab.hl.additional.model.Visitor;

import java.util.List;
import java.util.UUID;

@Service
public class MainClient {
    private final RestTemplate restTemplate;
    private final String mainServiceUrl;
    private final ObservabilityService observabilityService;

    public MainClient(RestTemplate restTemplate,
                    @Value("${main.service.url}") String mainServiceUrl,
                    ObservabilityService observabilityService) {
        this.restTemplate = restTemplate;
        this.mainServiceUrl = mainServiceUrl;
        this.observabilityService = observabilityService;
    }

    public List<Session> getAllSessions() {
        long startTime = System.currentTimeMillis();
        try {
            return restTemplate.exchange(
                mainServiceUrl + "/sessions",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Session>>() {}
            ).getBody();
        } finally {
            observabilityService.recordTiming(
                "external.main.sessions.getAll",
                System.currentTimeMillis() - startTime
            );
        }
    }

    public Visitor getVisitor(UUID visitorId) {
        long startTime = System.currentTimeMillis();
        try {
            return restTemplate.getForObject(
                mainServiceUrl + "/visitors/" + visitorId,
                Visitor.class
            );
        } finally {
            observabilityService.recordTiming(
                "external.main.visitors.get",
                System.currentTimeMillis() - startTime
            );
        }
    }
}
