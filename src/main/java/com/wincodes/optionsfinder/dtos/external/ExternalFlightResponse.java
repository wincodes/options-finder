package com.wincodes.optionsfinder.dtos.external;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ExternalFlightResponse(
        String source,

        @JsonProperty("generated_at")
        long generatedAt,

        List<Offer> results
) {

    public record Offer(

            @JsonProperty("offer_id")
            String offerId,

            @JsonProperty("airline_name")
            String airlineName,

            @JsonProperty("dep_airport")
            String departureAirport,

            @JsonProperty("arr_airport")
            String arrivalAirport,

            @JsonProperty("dep_time")
            String departureTime,

            @JsonProperty("arr_time")
            String arrivalTime,

            int availability,

            @JsonProperty("price_cents")
            int priceCents,

            String currency,

            List<Leg> legs,

            String note
    ) {}

    public record Leg(
            @JsonProperty("flight_no")
            String flightNumber,

            String from,
            String to
    ) {}
}
