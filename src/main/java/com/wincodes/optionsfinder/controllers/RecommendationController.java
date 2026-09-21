package com.wincodes.optionsfinder.controllers;

import com.wincodes.optionsfinder.dtos.DisruptionResponse;
import com.wincodes.optionsfinder.models.*;
import com.wincodes.optionsfinder.services.RecommendationService;
import tools.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final ObjectMapper objectMapper;

    public RecommendationController(
            RecommendationService recommendationService,
            ObjectMapper objectMapper
    ) {
        this.recommendationService = recommendationService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public RecommendationResponse getRecommendations()
            throws Exception {

        ClassPathResource resource =
                new ClassPathResource("data/disruption.json");

        DisruptionResponse disruption;

        try (InputStream inputStream =
                     resource.getInputStream()) {

            disruption =
                    objectMapper.readValue(
                            inputStream,
                            DisruptionResponse.class
                    );
        }

        List<Booking> bookings =
                disruption.bookings()
                        .stream()
                        .map(this::mapBooking)
                        .toList();

        List<Recommendation> recommendations =
                recommendationService.recommend(
                        bookings
                );

        return new RecommendationResponse(
                disruption.disruption(),
                recommendations
        );
    }

    private Booking mapBooking(
            DisruptionResponse.BookingDto booking
    ) {

        List<Journey.Segment> segments =
                booking.journey()
                        .segments()
                        .stream()
                        .map(segment ->
                                new Journey.Segment(
                                        segment.flight(),
                                        segment.from(),
                                        segment.to(),
                                        segment.departure(),
                                        segment.arrival(),
                                        segment.status()
                                )
                        )
                        .toList();

        Journey journey =
                new Journey(
                        booking.journey().origin(),
                        booking.journey().destination(),
                        segments
                );

        return new Booking(
                booking.bookingRef(),
                booking.passengers(),
                journey
        );
    }

    public record RecommendationResponse(
            DisruptionResponse.Disruption disruption,
            List<Recommendation> recommendations
    ) {}
}
