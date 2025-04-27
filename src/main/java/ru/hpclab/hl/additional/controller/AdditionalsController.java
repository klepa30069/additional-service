package ru.hpclab.hl.additional.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.hpclab.hl.additional.model.Session;
import ru.hpclab.hl.additional.service.AdditionalService;

import java.util.List;

@RestController
@RequestMapping("/additionals")
public class AdditionalsController {
    private final AdditionalService additionalService;

    public AdditionalsController(AdditionalService additionalService) {
        this.additionalService = additionalService;
    }

    @GetMapping("/average-duration")
    public ResponseEntity<Double> getAverageDuration(
        @RequestParam String fio,
        @RequestParam int month,
        @RequestParam String code) {

        return ResponseEntity.ok(additionalService.calculateAverageDuration(fio, month, code));
    }
}
