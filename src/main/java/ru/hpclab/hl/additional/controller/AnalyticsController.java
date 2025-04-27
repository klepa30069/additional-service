package ru.hpclab.hl.additional.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.hpclab.hl.analytics.model.DownloadStatistics;
import ru.hpclab.hl.analytics.service.AnalyticsService;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    @GetMapping("/average-duration")
    public ResponseEntity<Double> getAverageDuration(
            @RequestParam String fio,
            @RequestParam int month,
            @RequestParam int year) {
        long start = System.currentTimeMillis();
        try {
            return ResponseEntity.ok(analyticsService.getAverageDuration(fio, month, year));
        } finally {
            analyticsService.getObservability().recordTiming(
                "controller.analytics.duration",
                System.currentTimeMillis() - start
            );
        }
    }
}
