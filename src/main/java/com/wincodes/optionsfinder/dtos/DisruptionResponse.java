package com.wincodes.optionsfinder.dtos;

import java.time.OffsetDateTime;
import java.util.List;

public record DisruptionResponse(
        Disruption disruption,
        List<BookingDto> bookings
) {

    public record Disruption(
            String flight,
            String origin,
            String destination,
            OffsetDateTime scheduledDeparture,
            OffsetDateTime scheduledArrival,
            String status,
            String reason,
            OffsetDateTime cancelledAt,
            int affectedBookings,
            int affectedPassengers
    ) {}

    public record BookingDto(
            String bookingRef,
            int passengers,
            JourneyDto journey
    ) {}

    public record JourneyDto(
            String origin,
            String destination,
            List<SegmentDto> segments
    ) {}

    public record SegmentDto(
            String flight,
            String from,
            String to,
            OffsetDateTime departure,
            OffsetDateTime arrival,
            String status
    ) {}
}
