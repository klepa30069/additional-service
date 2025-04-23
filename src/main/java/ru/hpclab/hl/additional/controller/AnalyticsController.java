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
