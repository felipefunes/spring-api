package com.orbitalwatcher.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LocationInfo(
        String name,
        String country,
        @JsonProperty("is_over_ocean") Boolean isOverOcean
) {

    public static LocationInfo internationalWaters() {
        return new LocationInfo("International Waters", "N/A", true);
    }

    public static LocationInfo unavailable() {
        return new LocationInfo("UNAVAILABLE", "UNAVAILABLE", null);
    }
}
