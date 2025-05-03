package ru.hpclab.hl.additional.crach;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient coreServiceWebClient() {
        return WebClient.builder().build();
    }
}
