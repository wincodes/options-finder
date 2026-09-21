package com.wincodes.optionsfinder.models;

import java.time.OffsetDateTime;

public record SearchRequest(
        String origin,
        String destination,
        OffsetDateTime earliestDeparture
) {}