package com.orbitalwatcher.service;

import com.orbitalwatcher.dto.IssPosition;
import com.orbitalwatcher.dto.external.IssNowResponse;
import com.orbitalwatcher.exception.IssLocationUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Fetches the ISS's current coordinates from the Open-Notify API. This is
 * the one call that is NOT gracefully degraded: every other piece of the
 * response depends on having a valid latitude/longitude, so a failure here
 * is surfaced as an error via {@link IssLocationUnavailableException}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IssLocationService {

    private final RestClient issApiRestClient;

    public IssPosition getCurrentPosition() {
        IssNowResponse response;
        try {
            response = issApiRestClient.get()
                    .uri("/iss-now.json")
                    .retrieve()
                    .body(IssNowResponse.class);
        } catch (RestClientException ex) {
            throw new IssLocationUnavailableException("Failed to call Open-Notify ISS location API", ex);
        }

        if (response == null || response.issPosition() == null) {
            throw new IssLocationUnavailableException("Open-Notify returned an empty ISS location payload");
        }

        try {
            double latitude = Double.parseDouble(response.issPosition().latitude());
            double longitude = Double.parseDouble(response.issPosition().longitude());
            return new IssPosition(latitude, longitude);
        } catch (NumberFormatException ex) {
            throw new IssLocationUnavailableException("Open-Notify returned a non-numeric ISS location", ex);
        }
    }
}
