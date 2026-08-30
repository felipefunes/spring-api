package com.orbitalwatcher.service;

import com.orbitalwatcher.dto.LocationInfo;
import com.orbitalwatcher.dto.external.NominatimResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Reverse-geocodes ISS coordinates into a human-readable place name using
 * Nominatim (OpenStreetMap). Roughly 70% of the Earth's surface is ocean, so
 * "no address found" is an expected, common outcome and is mapped to
 * {@link LocationInfo#internationalWaters()} rather than treated as an error.
 * Genuine upstream failures degrade to {@link LocationInfo#unavailable()} so
 * the endpoint can still return 200 OK.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodingService {

    private final RestClient geocodingRestClient;

    public LocationInfo reverseGeocode(double latitude, double longitude) {
        try {
            NominatimResponse response = geocodingRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/reverse")
                            .queryParam("format", "json")
                            .queryParam("lat", latitude)
                            .queryParam("lon", longitude)
                            .queryParam("zoom", 10)
                            .queryParam("addressdetails", 1)
                            .build())
                    .retrieve()
                    .body(NominatimResponse.class);

            if (response == null || response.error() != null || response.address() == null) {
                return LocationInfo.internationalWaters();
            }

            NominatimResponse.Address address = response.address();
            String country = address.country();
            if (country == null || country.isBlank()) {
                // No resolvable landmass at these coordinates (e.g. mid-ocean).
                return LocationInfo.internationalWaters();
            }

            String name = firstNonBlank(address.city(), address.town(), address.village(), address.county(), address.state());
            return new LocationInfo(name != null ? name : "Unknown Region", country, false);
        } catch (RestClientException ex) {
            log.warn("Reverse geocoding call failed for lat={}, lon={}: {}", latitude, longitude, ex.getMessage());
            return LocationInfo.unavailable();
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
