package com.orbitalwatcher.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Raw payload from Nominatim's {@code /reverse} endpoint. When the
 * coordinates fall over open ocean (no landmass), Nominatim typically
 * responds with an {@code error} field and no {@code address} block instead
 * of an HTTP error, which {@link com.orbitalwatcher.service.GeocodingService}
 * treats as "International Waters".
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NominatimResponse(
        String error,
        Address address
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Address(
            String city,
            String town,
            String village,
            String county,
            String state,
            String country,
            String country_code
    ) {
    }
}
