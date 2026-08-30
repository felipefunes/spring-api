package com.orbitalwatcher.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record IssTelemetryResponse(
        Instant timestamp,
        IssPosition issPosition,
        LocationInfo location,
        WeatherInfo weather
) {
}
