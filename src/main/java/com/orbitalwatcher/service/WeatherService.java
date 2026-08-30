package com.orbitalwatcher.service;

import com.orbitalwatcher.dto.WeatherInfo;
import com.orbitalwatcher.dto.external.OpenWeatherMapResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Locale;

/**
 * Fetches current weather conditions for the ISS's ground position from
 * OpenWeatherMap. Any upstream failure (missing/invalid API key, timeout,
 * no data for the given coordinates) degrades gracefully to
 * {@link WeatherInfo#unavailable()} rather than failing the whole request.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final RestClient weatherRestClient;

    @Value("${app.external-api.weather.api-key}")
    private String apiKey;

    public WeatherInfo getCurrentWeather(double latitude, double longitude) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("No OpenWeatherMap API key configured (OPENWEATHER_API_KEY); returning UNAVAILABLE weather");
            return WeatherInfo.unavailable();
        }

        try {
            OpenWeatherMapResponse response = weatherRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/weather")
                            .queryParam("lat", latitude)
                            .queryParam("lon", longitude)
                            .queryParam("units", "metric")
                            .queryParam("appid", apiKey)
                            .build())
                    .retrieve()
                    .body(OpenWeatherMapResponse.class);

            if (response == null || response.main() == null) {
                return WeatherInfo.unavailable();
            }

            String condition = extractCondition(response.weather());
            return new WeatherInfo(response.main().temp(), condition, response.main().humidity());
        } catch (RestClientException ex) {
            log.warn("Weather API call failed for lat={}, lon={}: {}", latitude, longitude, ex.getMessage());
            return WeatherInfo.unavailable();
        }
    }

    private static String extractCondition(List<OpenWeatherMapResponse.Weather> weather) {
        if (weather == null || weather.isEmpty() || weather.get(0).description() == null) {
            return "Unknown";
        }
        String description = weather.get(0).description();
        return Character.toUpperCase(description.charAt(0)) + description.substring(1).toLowerCase(Locale.ROOT);
    }
}
