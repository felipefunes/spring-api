package com.orbitalwatcher.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * Central error handling for the API.
 *
 * <p>Note that failures in the enrichment calls (geocoding, weather) are
 * <b>not</b> handled here: {@link com.orbitalwatcher.service.GeocodingService}
 * and {@link com.orbitalwatcher.service.WeatherService} catch their own
 * upstream failures and degrade gracefully to an "UNAVAILABLE" payload so the
 * endpoint can still return 200 OK with the ISS coordinates. This advice only
 * covers the non-degradable failure: not knowing where the ISS is at all.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IssLocationUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleIssLocationUnavailable(IssLocationUnavailableException ex) {
        log.error("Unable to determine ISS location: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(
                        Instant.now(),
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "ISS_LOCATION_UNAVAILABLE",
                        "Unable to retrieve the current ISS location from the upstream Open-Notify API. Please try again shortly."
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error while handling request", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        Instant.now(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "INTERNAL_ERROR",
                        "An unexpected error occurred while processing the request."
                ));
    }
}
