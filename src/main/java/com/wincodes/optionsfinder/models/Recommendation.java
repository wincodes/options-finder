package com.wincodes.optionsfinder.models;

import java.util.List;

public record Recommendation(
        String bookingRef,
        int passengers,
        TravelOption recommendation,
        String reason,
        List<TravelOption> alternatives
) {}
