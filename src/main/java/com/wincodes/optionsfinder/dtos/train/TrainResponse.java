package com.wincodes.optionsfinder.dtos.train;

import java.util.List;

public record TrainResponse(
        String provider,
        List<Query> queries
) {

    public record Query(
            String fromCity,
            String fromAirport,
            String toCity,
            String toAirport,
            String notBefore,
            List<Connection> connections
    ) {}

    public record Connection(
            String id,
            String operator,
            String operatorCode,
            String trainNumber,
            String departureStation,
            String arrivalStation,
            String departure,
            String arrival,
            int availableSeats,
            String price,
            String via
    ) {}
}