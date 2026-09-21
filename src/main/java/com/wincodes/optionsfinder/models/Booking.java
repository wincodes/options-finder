package com.wincodes.optionsfinder.models;

public record Booking(
        String bookingRef,
        int passengers,
        Journey journey
) {}
