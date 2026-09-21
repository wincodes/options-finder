package com.wincodes.optionsfinder.models;

import java.util.List;

public record SourceResult(
        Source source,
        SourceStatus status,
        List<TravelOption> options,
        String error,
        long durationMs
) {

    public static SourceResult success(
            Source source,
            List<TravelOption> options,
            long durationMs
    ) {
        return new SourceResult(
                source,
                SourceStatus.SUCCESS,
                options,
                null,
                durationMs
        );
    }

    public static SourceResult failure(
            Source source,
            String error,
            long durationMs
    ) {
        return new SourceResult(
                source,
                SourceStatus.FAILED,
                List.of(),
                error,
                durationMs
        );
    }

    public static SourceResult timeout(
            Source source,
            long durationMs
    ) {
        return new SourceResult(
                source,
                SourceStatus.TIMEOUT,
                List.of(),
                "Provider timed out",
                durationMs
        );
    }
}
