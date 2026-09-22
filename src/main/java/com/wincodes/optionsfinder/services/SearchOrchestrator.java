package com.wincodes.optionsfinder.services;

import com.wincodes.optionsfinder.models.*;
import com.wincodes.optionsfinder.sources.OptionSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class SearchOrchestrator {

    private static final Logger log =
            LoggerFactory.getLogger(SearchOrchestrator.class);

    private static final Duration SOURCE_TIMEOUT =
            Duration.ofSeconds(2);

    private final List<OptionSource> sources;

    public SearchOrchestrator(List<OptionSource> sources) {
        this.sources = sources;
    }

    public List<SourceResult> search(
            SearchRequest request
    ) {

        List<CompletableFuture<SourceResult>> futures =
                sources.stream()
                        .map(source ->
                                searchSource(source, request)
                        )
                        .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private CompletableFuture<SourceResult> searchSource(
            OptionSource source,
            SearchRequest request
    ) {

        long start = System.currentTimeMillis();

        return source.search(request)
                .orTimeout(
                        SOURCE_TIMEOUT.toMillis(),
                        java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .exceptionally(exception -> {

                    long duration =
                            System.currentTimeMillis() - start;

                    if (exception.getCause()
                            instanceof java.util.concurrent.TimeoutException
                            || exception
                            instanceof java.util.concurrent.TimeoutException) {
                        log.warn(
                                "Source timeout: source={}, origin={}, destination={}",
                                source.source(),
                                request.origin(),
                                request.destination()
                        );

                        return SourceResult.timeout(
                                source.source(),
                                duration
                        );
                    }

                    log.warn(
                            "Source failure: source={}, origin={}, destination={}, durationMs={}, error={}",
                            source.source(),
                            request.origin(),
                            request.destination(),
                            duration,
                            exception.getMessage()
                    );

                    return SourceResult.failure(
                            source.source(),
                            exception.getMessage(),
                            duration
                    );
                });
    }
}
