package com.orbitalwatcher.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Raw payload from OpenWeatherMap's Current Weather Data API
 * ({@code /data/2.5/weather}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenWeatherMapResponse(
        List<Weather> weather,
        Main main
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Weather(String main, String description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Main(double temp, double feels_like, int humidity) {
    }
}
