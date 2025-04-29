package ru.hpclab.hl.additional.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import ru.hpclab.hl.additional.model.Session;
import ru.hpclab.hl.additional.service.AdditionalService;
import ru.hpclab.hl.additional.service.ObservabilityService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/additionals")
@RequiredArgsConstructor
public class AdditionalsController {
    private final AdditionalService additionalService;
    private final ObservabilityService observabilityService;

    @GetMapping("/average-duration")
    public Map<String, Map<Integer, Double>> getMonthlyStats() {
        long startTime = System.currentTimeMillis();
        try {
            return additionalService.getAllUsersMonthlyAverageDuration();
        } finally {
            observabilityService.recordTiming("controller.additionals.average_duration",
                System.currentTimeMillis() - startTime);
        }

    }
}
