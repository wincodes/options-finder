package com.wincodes.optionsfinder.models;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record TravelOption(
        String id,
        Source source,
        String provider,
        String origin,
        String destination,
        OffsetDateTime departure,
        OffsetDateTime arrival,
        int availableSeats,
        Money price,
        List<Leg> legs,
        String note
) {

    public record Money(
            BigDecimal amount,
            String currency
    ) {}

    public record Leg(
            String identifier,
            String from,
            String to
    ) {}
}
