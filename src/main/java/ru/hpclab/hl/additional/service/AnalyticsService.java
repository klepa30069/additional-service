@Service
@RequiredArgsConstructor
public class AdditionalService {
    private final RestTemplate restTemplate;
    private final ObservabilityService observability;

    @Value("${main.service.url}")
    private String mainServiceUrl;

    public double getAverageDuration(String fio, int month, int year) {
        long start = System.currentTimeMillis();
        try {
            ResponseEntity<List<Session>> response = restTemplate.exchange(
                mainServiceUrl + "/sessions/by-fio?fio={fio}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Session>>() {},
                fio
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Main service error");
            }

            List<Session> sessions = response.getBody();
            return calculateAverage(sessions, month);
        } catch (Exception e) {
            observability.recordTiming("additional.service.error", System.currentTimeMillis() - start);
            throw e;
        }
    }
}
