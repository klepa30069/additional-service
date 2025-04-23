@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final RestTemplate restTemplate;
    private final ObservabilityService observability;

    public double getAverageDuration(String fio, int month, int year) {
        long start = System.currentTimeMillis();
        try {
            List<Session> sessions = restTemplate.getForObject(
                "http://main-service/sessions?fio={fio}",
                List.class,
                fio
            );
            return sessions.stream()
                .filter(s -> s.getDate().getMonthValue() == month)
                .mapToInt(Session::getDuration)
                .average()
                .orElse(0.0);
        } finally {
            observability.recordTiming("analytics.calculate", System.currentTimeMillis() - start);
        }
    }
}
