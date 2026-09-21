package com.wincodes.optionsfinder.dtos.internal;

import java.time.OffsetDateTime;
import java.util.List;

public record InternalFlightResponse(
        String source,
        String generatedAt,
        List<Search> searches
) {

    public record Search(
            String searchId,
            Query query,
            List<Offer> offers
    ) {}

    public record Query(
            String origin,
            String destination,
            OffsetDateTime earliestDeparture
    ) {}

    public record Offer(
            String offerId,
            String carrier,
            String carrierCode,
            String departureAirport,
            String arrivalAirport,
            OffsetDateTime departure,
            OffsetDateTime arrival,
            int availableSeats,
            PricePerSeat pricePerSeat,
            List<Leg> legs
    ) {}

    public record PricePerSeat(
            double amount,
            String currency
    ) {}

    public record Leg(
            String flightNumber,
            String from,
            String to
    ) {}
}
