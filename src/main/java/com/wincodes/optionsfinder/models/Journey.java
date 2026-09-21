package com.wincodes.optionsfinder.models;

import java.time.OffsetDateTime;
import java.util.List;

public record Journey(
        String origin,
        String destination,
        List<Segment> segments
) {

    public record Segment(
            String flight,
            String from,
            String to,
            OffsetDateTime departure,
            OffsetDateTime arrival,
            String status
    ) {}
}
