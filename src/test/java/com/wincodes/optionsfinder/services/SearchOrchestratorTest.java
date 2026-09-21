package com.wincodes.optionsfinder.services;

import com.wincodes.optionsfinder.models.SearchRequest;
import com.wincodes.optionsfinder.models.Source;
import com.wincodes.optionsfinder.models.SourceResult;
import com.wincodes.optionsfinder.models.SourceStatus;
import com.wincodes.optionsfinder.sources.OptionSource;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SearchOrchestratorTest {

    @Test
    void executesProvidersInParallel() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);

        try {
            OptionSource first = asyncBlockingSource(Source.INTERNAL_FLIGHTS, executor, started, release);
            OptionSource second = asyncBlockingSource(Source.EXTERNAL_FLIGHTS, executor, started, release);

            SearchOrchestrator orchestrator = new SearchOrchestrator(List.of(first, second));
            SearchRequest request = new SearchRequest("CGN", "BER", OffsetDateTime.parse("2026-07-21T18:35:00+02:00"));

            CompletableFuture<List<SourceResult>> searchFuture =
                    CompletableFuture.supplyAsync(() -> orchestrator.search(request));

            assertTrue(started.await(1, TimeUnit.SECONDS), "Both providers should start before either completes");

            release.countDown();
            List<SourceResult> results = searchFuture.get(1, TimeUnit.SECONDS);

            assertEquals(2, results.size());
            assertTrue(results.stream().allMatch(result -> result.status() == SourceStatus.SUCCESS));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void oneProviderFailureDoesNotFailEntireSearch() {
        OptionSource success = immediateSuccessSource(Source.INTERNAL_FLIGHTS);

        OptionSource failure = source(
                Source.EXTERNAL_FLIGHTS,
                request -> CompletableFuture.failedFuture(new RuntimeException("boom"))
        );

        SearchOrchestrator orchestrator = new SearchOrchestrator(List.of(success, failure));
        SearchRequest request = new SearchRequest("CGN", "BER", OffsetDateTime.parse("2026-07-21T18:35:00+02:00"));

        List<SourceResult> results = orchestrator.search(request);

        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(result -> result.source() == Source.INTERNAL_FLIGHTS && result.status() == SourceStatus.SUCCESS));
        assertTrue(results.stream().anyMatch(result -> result.source() == Source.EXTERNAL_FLIGHTS && result.status() == SourceStatus.FAILED));
    }

    @Test
    void oneProviderTimeoutDoesNotFailEntireSearch() {
        OptionSource success = immediateSuccessSource(Source.INTERNAL_FLIGHTS);
        OptionSource hanging = source(Source.TRAINS, request -> new CompletableFuture<>());

        SearchOrchestrator orchestrator = new SearchOrchestrator(List.of(success, hanging));
        SearchRequest request = new SearchRequest("CGN", "BER", OffsetDateTime.parse("2026-07-21T18:35:00+02:00"));

        long start = System.currentTimeMillis();
        List<SourceResult> results = orchestrator.search(request);
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration >= 1900, "Search should wait for timeout window");
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(result -> result.source() == Source.INTERNAL_FLIGHTS && result.status() == SourceStatus.SUCCESS));
        assertTrue(results.stream().anyMatch(result -> result.source() == Source.TRAINS && result.status() == SourceStatus.TIMEOUT));
    }

    @Test
    void emptyProviderResponseIsSuccess() {
        OptionSource empty = source(
                Source.TRAINS,
                request -> CompletableFuture.completedFuture(
                        SourceResult.success(Source.TRAINS, List.of(), 12)
                )
        );

        SearchOrchestrator orchestrator = new SearchOrchestrator(List.of(empty));
        SearchRequest request = new SearchRequest("CGN", "DXB", OffsetDateTime.parse("2026-07-21T18:35:00+02:00"));

        List<SourceResult> results = orchestrator.search(request);

        assertEquals(1, results.size());
        assertEquals(SourceStatus.SUCCESS, results.get(0).status());
        assertTrue(results.get(0).options().isEmpty());
    }

    private OptionSource immediateSuccessSource(Source source) {
        return this.source(
                source,
                request -> CompletableFuture.completedFuture(
                        SourceResult.success(source, List.of(), 5)
                )
        );
    }

    private OptionSource asyncBlockingSource(
            Source source,
            ExecutorService executor,
            CountDownLatch started,
            CountDownLatch release
    ) {
        return this.source(
                source,
                request -> CompletableFuture.supplyAsync(() -> {
                    started.countDown();
                    try {
                        if (!release.await(1, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Timed out waiting for test release signal");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                    return SourceResult.success(source, List.of(), 20);
                }, executor)
        );
    }

    private OptionSource source(
            Source source,
            java.util.function.Function<SearchRequest, CompletableFuture<SourceResult>> behavior
    ) {
        return new OptionSource() {
            @Override
            public Source source() {
                return source;
            }

            @Override
            public CompletableFuture<SourceResult> search(SearchRequest request) {
                return behavior.apply(request);
            }
        };
    }
}
