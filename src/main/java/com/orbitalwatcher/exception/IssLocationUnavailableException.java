package com.orbitalwatcher.exception;

/**
 * Thrown when the ISS's current position cannot be determined. Unlike the
 * geocoding/weather enrichment steps, this is not gracefully degradable: with
 * no coordinates there is nothing meaningful to return, so this bubbles up to
 * a 503 response via {@link com.orbitalwatcher.exception.GlobalExceptionHandler}.
 */
public class IssLocationUnavailableException extends RuntimeException {

    public IssLocationUnavailableException(String message) {
        super(message);
    }

    public IssLocationUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
