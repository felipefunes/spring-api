package com.orbitalwatcher.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record WeatherInfo(
        Double temperatureCelsius,
        String condition,
        Integer humidityPercentage
) {

    public static WeatherInfo unavailable() {
        return new WeatherInfo(null, "UNAVAILABLE", null);
    }
}
