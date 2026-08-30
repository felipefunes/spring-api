package com.orbitalwatcher.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Raw payload from Open-Notify's {@code /iss-now.json}. Note latitude and
 * longitude are returned as strings by the upstream API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IssNowResponse(
        @JsonProperty("iss_position") IssPositionRaw issPosition,
        long timestamp,
        String message
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IssPositionRaw(String latitude, String longitude) {
    }
}
