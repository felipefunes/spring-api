package com.orbitalwatcher.service;

import com.orbitalwatcher.dto.IssPosition;
import com.orbitalwatcher.dto.IssTelemetryResponse;
import com.orbitalwatcher.dto.LocationInfo;
import com.orbitalwatcher.dto.WeatherInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Orchestrates a single {@code /api/v1/iss-telemetry} request:
 *
 * <ol>
 *     <li>Fetch the ISS's current coordinates (sequential, everything else
 *     depends on it).</li>
 *     <li>Fan out the reverse-geocoding and weather lookups concurrently on
 *     virtual threads, since they are independent of each other and each
 *     only need the coordinates from step 1.</li>
 *     <li>Join both results and assemble the final response.</li>
 * </ol>
 *
 * <p>The fan-out uses {@link CompletableFuture#supplyAsync(java.util.function.Supplier, java.util.concurrent.Executor)}
 * against a virtual-thread-per-task {@link ExecutorService}
 * ({@link com.orbitalwatcher.config.VirtualThreadConfig}). Each of these
 * virtual threads blocks on its own {@code RestClient} HTTP call, but
 * because virtual threads are cheap (backed by platform-thread carriers only
 * while actually running, not while blocked on I/O), this costs a JVM only a
 * few KB of stack rather than a full OS thread — allowing the service to
 * comfortably handle very high concurrent request volume without needing a
 * bounded thread pool.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TelemetryAggregatorService {

    private final IssLocationService issLocationService;
    private final GeocodingService geocodingService;
    private final WeatherService weatherService;
    private final ExecutorService telemetryVirtualThreadExecutor;

    public IssTelemetryResponse getTelemetry() {
        IssPosition position = issLocationService.getCurrentPosition();

        CompletableFuture<LocationInfo> locationFuture = CompletableFuture.supplyAsync(
                () -> geocodingService.reverseGeocode(position.latitude(), position.longitude()),
                telemetryVirtualThreadExecutor
        );

        CompletableFuture<WeatherInfo> weatherFuture = CompletableFuture.supplyAsync(
                () -> weatherService.getCurrentWeather(position.latitude(), position.longitude()),
                telemetryVirtualThreadExecutor
        );

        LocationInfo location = locationFuture.join();
        WeatherInfo weather = weatherFuture.join();

        return new IssTelemetryResponse(Instant.now(), position, location, weather);
    }
}
