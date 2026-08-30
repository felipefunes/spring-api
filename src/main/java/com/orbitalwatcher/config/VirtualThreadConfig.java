package com.orbitalwatcher.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Dedicated virtual-thread-per-task executor used to fan out the concurrent
 * calls to the geocoding and weather APIs once the ISS coordinates are known.
 * Kept separate from Spring MVC's own request-handling virtual thread pool
 * (spring.threads.virtual.enabled) so that fan-out concurrency is explicit
 * and independently testable.
 */
@Configuration
public class VirtualThreadConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService telemetryVirtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
