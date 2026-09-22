package com.wincodes.optionsfinder.sources;

import com.wincodes.optionsfinder.config.MockSourceBehaviorProperties;
import com.wincodes.optionsfinder.models.SearchRequest;
import com.wincodes.optionsfinder.models.Source;
import com.wincodes.optionsfinder.models.SourceResult;
import com.wincodes.optionsfinder.models.SourceStatus;
import com.wincodes.optionsfinder.services.SearchOrchestrator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class SourceBehaviorInjectionTest {

    private ExecutorService executor;
    private ObjectMapper objectMapper;
    private MockSourceBehaviorProperties behaviorProperties;
    private MockSourceBehaviorApplier behaviorApplier;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(3);
        objectMapper = new ObjectMapper();
        behaviorProperties = new MockSourceBehaviorProperties();
        behaviorApplier = new MockSourceBehaviorApplier();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void injectedLatencyAffectsSourceDuration() {
        behaviorProperties.getInternalFlights().setLatencyMs(120);
        behaviorProperties.getInternalFlights()
                .setOutcome(MockSourceBehaviorProperties.Outcome.EMPTY);

        InternalFlightSource source = new InternalFlightSource(
                objectMapper,
                executor,
                behaviorProperties,
                behaviorApplier
        );

        SourceResult result = source.search(request()).join();

        assertEquals(SourceStatus.SUCCESS, result.status());
        assertTrue(result.durationMs() >= 100);
    }

    @Test
    void injectedEmptyProducesSuccessfulEmptyResult() {
        behaviorProperties.getInternalFlights()
                .setOutcome(MockSourceBehaviorProperties.Outcome.EMPTY);

        InternalFlightSource source = new InternalFlightSource(
                objectMapper,
                executor,
                behaviorProperties,
                behaviorApplier
        );

        SourceResult result = source.search(request()).join();

        assertEquals(SourceStatus.SUCCESS, result.status());
        assertTrue(result.options().isEmpty());
    }

    @Test
    void injectedFailureProducesFailedResult() {
        behaviorProperties.getInternalFlights()
                .setOutcome(MockSourceBehaviorProperties.Outcome.FAIL);

        InternalFlightSource source = new InternalFlightSource(
                objectMapper,
                executor,
                behaviorProperties,
                behaviorApplier
        );

        SourceResult result = source.search(request()).join();

        assertEquals(SourceStatus.FAILED, result.status());
        assertEquals("Injected mock source failure", result.error());
    }

    @Test
    void injectedTimeoutIsMappedByOrchestrator() {
        behaviorProperties.getInternalFlights()
                .setOutcome(MockSourceBehaviorProperties.Outcome.TIMEOUT);
        behaviorProperties.getInternalFlights().setTimeoutDelayMs(2500);

        InternalFlightSource timingOut = new InternalFlightSource(
                objectMapper,
                executor,
                behaviorProperties,
                behaviorApplier
        );

        OptionSource healthy = new OptionSource() {
            @Override
            public Source source() {
                return Source.TRAINS;
            }

            @Override
            public CompletableFuture<SourceResult> search(SearchRequest request) {
                return CompletableFuture.completedFuture(
                        SourceResult.success(source(), List.of(), 10)
                );
            }
        };

        SearchOrchestrator orchestrator =
                new SearchOrchestrator(List.of(timingOut, healthy));

        List<SourceResult> results = orchestrator.search(request());

        assertEquals(2, results.size());
        assertTrue(results.stream()
                .anyMatch(result ->
                        result.source() == Source.INTERNAL_FLIGHTS
                                && result.status() == SourceStatus.TIMEOUT));
        assertTrue(results.stream()
                .anyMatch(result ->
                        result.source() == Source.TRAINS
                                && result.status() == SourceStatus.SUCCESS));
    }

    private SearchRequest request() {
        return new SearchRequest(
                "CGN",
                "BER",
                OffsetDateTime.parse("2026-07-21T18:35:00+02:00")
        );
    }
}
