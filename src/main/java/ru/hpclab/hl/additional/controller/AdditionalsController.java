package ru.hpclab.hl.additional.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import ru.hpclab.hl.additional.model.Session;
import ru.hpclab.hl.additional.service.AdditionalService;
import ru.hpclab.hl.additional.service.ObservabilityService;

import java.util.List;

@RestController
@RequestMapping("/additionals")
@RequiredArgsConstructor
public class AdditionalsController {
    private final AdditionalService additionalService;
    private final ObservabilityService observabilityService;

    @GetMapping("/average-duration")
    public ResponseEntity<Double> getAverageDuration(
        @RequestParam String fio,
        @RequestParam int month,
        @RequestParam int year) {
        long startTime = System.currentTimeMillis();

        try {
            double average = additionalService.calculateAverageDuration(fio, month, year);
            return ResponseEntity.ok(average);
        } finally {
            observabilityService.recordTiming("controller.additionals.average_duration",
                System.currentTimeMillis() - startTime);
        }

    }
}
