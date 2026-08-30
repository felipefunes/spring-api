# Orbital Watcher 🛰️

Real-time International Space Station (ISS) tracking API. Given the ISS's current
latitude/longitude, it concurrently enriches that position with **where it is** (reverse
geocoded location) and **what the weather is like down there** (current conditions),
returning one cohesive JSON payload.

```
GET /api/v1/iss-telemetry
```

```json
{
  "timestamp": "2023-10-27T14:32:00Z",
  "iss_position": {
    "latitude": 48.8566,
    "longitude": 2.3522
  },
  "location": {
    "name": "Paris",
    "country": "France",
    "is_over_ocean": false
  },
  "weather": {
    "temperature_celsius": 15.2,
    "condition": "Moderate Rain",
    "humidity_percentage": 82
  }
}
```

---

## Table of contents

- [Architecture](#architecture)
- [Tech stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting an OpenWeatherMap API key](#getting-an-openweathermap-api-key)
- [Configuration](#configuration)
- [Build & run](#build--run)
- [Testing the endpoint](#testing-the-endpoint)
- [Graceful degradation & error handling](#graceful-degradation--error-handling)
- [How Virtual Threads are used](#how-virtual-threads-are-used)
- [Project structure](#project-structure)
- [Running the test suite](#running-the-test-suite)

---

## Architecture

A single request to `/api/v1/iss-telemetry` fans out to three upstream, unauthenticated-or-free
public APIs:

| Step | Upstream                                    | Purpose                                    | Auth required |
|------|----------------------------------------------|---------------------------------------------|---------------|
| 1    | Open-Notify (`api.open-notify.org`)           | Current ISS latitude/longitude              | No            |
| 2a   | Nominatim / OpenStreetMap                     | Reverse geocode lat/lon → city, country     | No             |
| 2b   | OpenWeatherMap                                | Current weather at lat/lon                  | Yes (free tier)|

Step 1 must complete first since steps 2a/2b need the coordinates it produces. Once the
coordinates are known, 2a and 2b are **independent of each other**, so they are fired
**concurrently on virtual threads** and joined before the final response is assembled.

```
Client
  │
  ▼
IssTelemetryController
  │
  ▼
TelemetryAggregatorService
  │
  ├─ 1. IssLocationService ───────► Open-Notify  (sequential, blocking)
  │
  ├─ 2a. GeocodingService  ───────► Nominatim     ┐  concurrent, each on
  └─ 2b. WeatherService    ───────► OpenWeatherMap┘  its own virtual thread
  │
  ▼
IssTelemetryResponse (200 OK)
```

**Layering**

- `controller` — thin REST entry point, no business logic.
- `service` — one service per upstream integration (`IssLocationService`,
  `GeocodingService`, `WeatherService`), plus `TelemetryAggregatorService`, which
  orchestrates the sequential-then-concurrent flow described above.
- `dto` — the public API's response shape (records, `snake_case` JSON via Jackson's
  `SnakeCaseStrategy`).
- `dto.external` — raw upstream payload shapes, kept separate from the public DTOs so
  upstream API quirks (e.g. Open-Notify returning lat/lon as strings) never leak into the
  response contract.
- `config` — `RestClient` and virtual-thread executor wiring.
- `exception` — `@RestControllerAdvice` global error handling.

## Tech stack

- **Java 21** — Language & runtime, using **Virtual Threads** (Project Loom).
- **Spring Boot 3.4.x** — Application framework.
- **Spring MVC + `RestClient`** — Modern, fluent, synchronous HTTP client (no
  `RestTemplate`, no WebFlux — virtual threads make blocking I/O cheap enough that a
  reactive stack isn't needed here).
- **Maven** (with Maven Wrapper, `./mvnw`, included — no local Maven install required).
- **Lombok** — Boilerplate reduction (`@RequiredArgsConstructor`, `@Slf4j`) on service/config
  classes; DTOs are plain Java **records**.

## Prerequisites

- **Java 21** or newer (JDK, not just a JRE). Verify with:
  ```bash
  java -version
  ```
- **Maven 3.9+** — optional, since the Maven Wrapper (`./mvnw` / `mvnw.cmd`) is checked in
  and will download the correct Maven version automatically.
- An **OpenWeatherMap API key** (free) — see below.
- Outbound internet access to `api.open-notify.org`, `nominatim.openstreetmap.org`, and
  `api.openweathermap.org`.

> This project was authored and bootstrapped without a local JDK/Maven available in the
> generating environment, so the build could not be executed there. Please run
> `./mvnw clean verify` locally per the steps below as a first sanity check.

## Getting an OpenWeatherMap API key

1. Go to <https://home.openweathermap.org/users/sign_up> and create a free account.
2. Once logged in, go to **My API keys** (<https://home.openweathermap.org/api_keys>).
3. Copy the default key that's generated for you (or click **Generate** to create a new one).
4. **Note:** new keys can take anywhere from a few minutes up to ~2 hours to activate.
   If you get `401 Unauthorized` immediately after creating the key, wait and retry.
5. The free tier's *Current Weather Data* endpoint (used by this project) allows
   **60 calls/minute** and **1,000,000 calls/month**, which is far more than needed for
   local development/testing.

No API key is required for Open-Notify or Nominatim — Nominatim's usage policy does
require a descriptive `User-Agent` header, which is already set for you (see
`app.external-api.geocoding.user-agent` below); for anything beyond light local testing,
please review [Nominatim's usage policy](https://operations.osmfoundation.org/policies/nominatim/).

## Configuration

All configuration lives in `src/main/resources/application.yml`:

```yaml
spring:
  threads:
    virtual:
      enabled: true   # Spring MVC handles each HTTP request on a virtual thread

app:
  external-api:
    connect-timeout-ms: 3000
    read-timeout-ms: 5000
    iss-location:
      base-url: http://api.open-notify.org
    geocoding:
      base-url: https://nominatim.openstreetmap.org
      user-agent: "orbital-watcher/1.0 (contact: ${CONTACT_EMAIL:noreply@example.com})"
    weather:
      base-url: https://api.openweathermap.org/data/2.5
      api-key: ${OPENWEATHER_API_KEY:}
```

The only value you need to supply is the `OPENWEATHER_API_KEY` environment variable
(`CONTACT_EMAIL` is optional, purely cosmetic for the Nominatim User-Agent string).
Timeouts (`connect-timeout-ms` / `read-timeout-ms`) are wired directly into the
underlying `java.net.http.HttpClient` used by `RestClient`, so a hung or unreachable
upstream can never block a request indefinitely.

## Build & run

1. **Clone / open the project**, then export your API key:

   ```bash
   export OPENWEATHER_API_KEY=your_actual_key_here
   ```

   *(Windows PowerShell: `$env:OPENWEATHER_API_KEY="your_actual_key_here"`)*

2. **Build**:

   ```bash
   ./mvnw clean install
   ```

3. **Run**:

   ```bash
   ./mvnw spring-boot:run
   ```

   or, after building, run the packaged jar directly:

   ```bash
   java -jar target/orbital-watcher-0.0.1-SNAPSHOT.jar
   ```

   The API starts on **http://localhost:8080** by default (override with the `SERVER_PORT`
   env var).

4. You should see a log line confirming virtual threads are active:

   ```
   Tomcat started on port 8080 (http) with context path ''
   ```

   (Spring Boot logs the active `spring.threads.virtual.enabled` setting at startup under
   `DEBUG`/`INFO` depending on version; you can also confirm behaviorally — see next section.)

## Testing the endpoint

### curl

```bash
curl -s http://localhost:8080/api/v1/iss-telemetry | jq
```

Without `jq`:

```bash
curl -s -i http://localhost:8080/api/v1/iss-telemetry
```

### HTTPie

```bash
http GET :8080/api/v1/iss-telemetry
```

### Example success response

```json
{
    "timestamp": "2026-08-20T18:04:12.512396Z",
    "iss_position": {
        "latitude": 25.9812,
        "longitude": -142.3311
    },
    "location": {
        "name": "International Waters",
        "country": "N/A",
        "is_over_ocean": true
    },
    "weather": {
        "temperature_celsius": 24.6,
        "condition": "Scattered Clouds",
        "humidity_percentage": 78
    }
}
```

### Example degraded response (weather upstream unreachable / no API key)

Still `200 OK` — the ISS position and any successful enrichment are always returned:

```json
{
    "timestamp": "2026-08-20T18:05:03.118842Z",
    "iss_position": {
        "latitude": 51.2103,
        "longitude": 4.9012
    },
    "location": {
        "name": "Antwerp",
        "country": "Belgium",
        "is_over_ocean": false
    },
    "weather": {
        "temperature_celsius": null,
        "condition": "UNAVAILABLE",
        "humidity_percentage": null
    }
}
```

### Example hard failure (Open-Notify itself is unreachable)

```bash
curl -s -i http://localhost:8080/api/v1/iss-telemetry
```

```
HTTP/1.1 503 Service Unavailable
Content-Type: application/json

{
    "timestamp": "2026-08-20T18:06:44.902113Z",
    "status": 503,
    "error": "ISS_LOCATION_UNAVAILABLE",
    "message": "Unable to retrieve the current ISS location from the upstream Open-Notify API. Please try again shortly."
}
```

## Graceful degradation & error handling

The `/api/v1/iss-telemetry` endpoint distinguishes between two failure classes:

- **Non-degradable failure** — the ISS's own coordinates can't be fetched
  (`IssLocationService` → Open-Notify down/unreachable). Without coordinates there is
  nothing meaningful to enrich, so `GlobalExceptionHandler`
  (`@RestControllerAdvice`) maps this to **`503 Service Unavailable`** with a structured
  `ErrorResponse` body.
- **Degradable failures** — `GeocodingService` and `WeatherService` each **catch their
  own upstream exceptions internally** and fall back to sentinel values
  (`LocationInfo.unavailable()` / `WeatherInfo.unavailable()`, or
  `LocationInfo.internationalWaters()` when Nominatim simply has no address for the
  coordinates — e.g. mid-ocean). These never throw out of the service layer, so the
  endpoint always returns **`200 OK`** with whatever data *was* successfully retrieved,
  and `"UNAVAILABLE"` / `null` markers for what wasn't.

This means a flaky weather provider, a temporary Nominatim rate-limit, or open-ocean
coordinates never take the whole endpoint down — only a missing ISS fix does.

## How Virtual Threads are used

Two independent layers of virtual threads work together:

1. **Request handling** — `spring.threads.virtual.enabled: true` in `application.yml`
   tells Spring Boot's embedded Tomcat to hand off each incoming HTTP request to a
   virtual thread instead of a pooled platform thread. This means the application can
   comfortably serve a very large number of concurrent, slow (I/O-bound) requests without
   needing to size a bounded worker thread pool — each request's thread simply "parks"
   cheaply while blocked on an upstream HTTP call, rather than occupying a scarce OS
   thread.

2. **Concurrent upstream fan-out** — `VirtualThreadConfig` exposes a dedicated
   `ExecutorService` built with `Executors.newVirtualThreadPerTaskExecutor()`.
   `TelemetryAggregatorService` uses it to run the reverse-geocoding call
   (`GeocodingService`) and the weather call (`WeatherService`) **concurrently**,
   via `CompletableFuture.supplyAsync(..., executor)`, joining both before assembling the
   response:

   ```java
   CompletableFuture<LocationInfo> locationFuture =
       CompletableFuture.supplyAsync(() -> geocodingService.reverseGeocode(lat, lon), executor);
   CompletableFuture<WeatherInfo> weatherFuture =
       CompletableFuture.supplyAsync(() -> weatherService.getCurrentWeather(lat, lon), executor);

   LocationInfo location = locationFuture.join();
   WeatherInfo weather = weatherFuture.join();
   ```

   Each of these tasks blocks synchronously on its own `RestClient` HTTP call (no
   reactive/async client code needed), but because each runs on its own cheap virtual
   thread, the two calls execute in **wall-clock parallel** — total latency for step 2 is
   `max(geocoding_latency, weather_latency)` rather than the sum of both, without the
   complexity of reactive composition (`Mono.zip`, callbacks, etc.).

   *Why `CompletableFuture` + a virtual-thread executor instead of Java 21's
   `StructuredTaskScope`?* Structured Concurrency (JEP 453) is still a **preview API** in
   JDK 21, which would require compiling and running this project with `--enable-preview`
   everywhere (build, IDE, `java -jar`, Docker base images, etc.) — a meaningful adoption
   tax for a project meant to run cleanly out of the box. `newVirtualThreadPerTaskExecutor()`
   is a stable, GA API that gets the same practical benefit (cheap, massively concurrent
   blocking I/O) with none of that friction. If/when Structured Concurrency finalizes,
   swapping `TelemetryAggregatorService`'s internals to a `StructuredTaskScope.ShutdownOnFailure`
   block is a self-contained, low-risk change.

3. **Custom `HttpClient` executor** — even the underlying `java.net.http.HttpClient` used
   by every `RestClient` bean (`RestClientConfig`) is itself configured with a
   virtual-thread executor, so the low-level HTTP call machinery never occupies a platform
   thread while waiting on the network either.

## Project structure

```
orbital-watcher/
├── mvnw, mvnw.cmd, .mvn/                       # Maven Wrapper
├── pom.xml
├── README.md
└── src
    ├── main
    │   ├── java/com/orbitalwatcher
    │   │   ├── OrbitalWatcherApplication.java
    │   │   ├── config
    │   │   │   ├── RestClientConfig.java        # RestClient beans + HTTP timeouts
    │   │   │   └── VirtualThreadConfig.java      # Virtual-thread ExecutorService bean
    │   │   ├── controller
    │   │   │   └── IssTelemetryController.java
    │   │   ├── service
    │   │   │   ├── IssLocationService.java       # Open-Notify (non-degradable)
    │   │   │   ├── GeocodingService.java         # Nominatim (degradable)
    │   │   │   ├── WeatherService.java           # OpenWeatherMap (degradable)
    │   │   │   └── TelemetryAggregatorService.java  # Orchestration + concurrency
    │   │   ├── dto
    │   │   │   ├── IssPosition.java
    │   │   │   ├── LocationInfo.java
    │   │   │   ├── WeatherInfo.java
    │   │   │   ├── IssTelemetryResponse.java
    │   │   │   └── external                      # Raw upstream payload shapes
    │   │   │       ├── IssNowResponse.java
    │   │   │       ├── NominatimResponse.java
    │   │   │       └── OpenWeatherMapResponse.java
    │   │   └── exception
    │   │       ├── IssLocationUnavailableException.java
    │   │       ├── ErrorResponse.java
    │   │       └── GlobalExceptionHandler.java
    │   └── resources
    │       └── application.yml
    └── test/java/com/orbitalwatcher
        ├── OrbitalWatcherApplicationTests.java
        └── service/GeocodingServiceTest.java
```

## Running the test suite

```bash
./mvnw test
```

`GeocodingServiceTest` uses Spring's `MockRestServiceServer` (bound to a `RestClient.Builder`)
to verify both the "resolved location" and "International Waters" (no address found)
branches of `GeocodingService` without making real network calls.
