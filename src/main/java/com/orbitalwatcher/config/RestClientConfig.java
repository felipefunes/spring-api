package com.orbitalwatcher.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executors;

/**
 * Configures the modern {@link RestClient} used to talk to every upstream
 * (Open-Notify, Nominatim, OpenWeatherMap). Each upstream gets its own
 * pre-configured client (base URL + any mandatory headers), but they all
 * share a single {@link ClientHttpRequestFactory} whose underlying
 * {@link HttpClient} is driven by a virtual-thread executor and has explicit
 * connect/read timeouts so a slow or dead upstream can never hang a request
 * thread indefinitely.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory(
            @Value("${app.external-api.connect-timeout-ms}") long connectTimeoutMs,
            @Value("${app.external-api.read-timeout-ms}") long readTimeoutMs) {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return requestFactory;
    }

    @Bean
    public RestClient issApiRestClient(
            ClientHttpRequestFactory clientHttpRequestFactory,
            @Value("${app.external-api.iss-location.base-url}") String baseUrl) {

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(clientHttpRequestFactory)
                .build();
    }

    @Bean
    public RestClient geocodingRestClient(
            ClientHttpRequestFactory clientHttpRequestFactory,
            @Value("${app.external-api.geocoding.base-url}") String baseUrl,
            @Value("${app.external-api.geocoding.user-agent}") String userAgent) {

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(clientHttpRequestFactory)
                // Nominatim's usage policy requires a descriptive User-Agent
                // identifying the application; anonymous/default agents are rate-limited.
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .build();
    }

    @Bean
    public RestClient weatherRestClient(
            ClientHttpRequestFactory clientHttpRequestFactory,
            @Value("${app.external-api.weather.base-url}") String baseUrl) {

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(clientHttpRequestFactory)
                .build();
    }
}
