package com.wincodes.optionsfinder.services;

import com.wincodes.optionsfinder.models.*;
import com.wincodes.optionsfinder.rankings.RecommendationRanker;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final SearchOrchestrator searchOrchestrator;
    private final RecommendationRanker ranker;

    public RecommendationService(
            SearchOrchestrator searchOrchestrator,
            RecommendationRanker ranker
    ) {
        this.searchOrchestrator = searchOrchestrator;
        this.ranker = ranker;
    }

    public List<Recommendation> recommend(
            List<Booking> bookings
    ) {

        /*
         * Group bookings by route.
         *
         * If 30 bookings all travel CGN -> BER,
         * we shouldn't call every provider 30 times.
         */
        Map<RouteKey, List<Booking>> grouped =
                bookings.stream()
                        .collect(Collectors.groupingBy(
                                booking -> new RouteKey(
                                        booking.journey().origin(),
                                        booking.journey().destination()
                                )
                        ));

        Map<RouteKey, List<TravelOption>> optionsByRoute =
                new HashMap<>();

        /*
         * Search once per unique route.
         */
        for (RouteKey route : grouped.keySet()) {

            OffsetDateTime earliestDeparture =
                    bookings.stream()
                            .filter(booking ->
                                    booking.journey()
                                            .origin()
                                            .equals(route.origin())
                                            &&
                                            booking.journey()
                                                    .destination()
                                                    .equals(route.destination())
                            )
                            .map(this::getEarliestDeparture)
                            .min(Comparator.naturalOrder())
                            .orElse(OffsetDateTime.now());

            SearchRequest request =
                    new SearchRequest(
                            route.origin(),
                            route.destination(),
                            earliestDeparture
                    );

            List<SourceResult> results =
                    searchOrchestrator.search(request);

            List<TravelOption> options =
                    results.stream()
                            .flatMap(result ->
                                    result.options().stream()
                            )
                            .filter(option ->
                                    option.origin()
                                            .equalsIgnoreCase(route.origin())
                                            &&
                                            option.destination()
                                                    .equalsIgnoreCase(route.destination())
                            )
                            .toList();

            optionsByRoute.put(route, options);
        }

        /*
         * Generate a recommendation for every booking.
         */
        List<Recommendation> recommendations =
                new ArrayList<>();

        for (Booking booking : bookings) {

            RouteKey route =
                    new RouteKey(
                            booking.journey().origin(),
                            booking.journey().destination()
                    );

            List<TravelOption> options =
                    optionsByRoute.getOrDefault(
                            route,
                            List.of()
                    );

            OffsetDateTime earliestDeparture =
                    getEarliestDeparture(booking);

            List<TravelOption> ranked =
                    ranker.rank(
                            options,
                            booking.passengers(),
                            earliestDeparture
                    );

            TravelOption recommendation =
                    ranked.isEmpty()
                            ? null
                            : ranked.get(0);

            List<TravelOption> alternatives =
                    ranked.size() <= 1
                            ? List.of()
                            : ranked.subList(
                            1,
                            ranked.size()
                    );

            String reason;

            if (recommendation == null) {

                reason =
                        options.isEmpty()
                                ? "No alternative available"
                                : "No alternative has sufficient capacity";

            } else {

                reason =
                        "Earliest arrival with sufficient seats";
            }

            recommendations.add(
                    new Recommendation(
                            booking.bookingRef(),
                            booking.passengers(),
                            recommendation,
                            reason,
                            alternatives
                    )
            );
        }

        return recommendations;
    }

    private OffsetDateTime getEarliestDeparture(
            Booking booking
    ) {

        return booking.journey()
                .segments()
                .stream()
                .filter(segment ->
                        "CANCELLED".equalsIgnoreCase(
                                segment.status()
                        )
                )
                .map(Journey.Segment::departure)
                .min(Comparator.naturalOrder())
                .orElseGet(() ->
                        booking.journey()
                                .segments()
                                .get(0)
                                .departure()
                );
    }

    private record RouteKey(
            String origin,
            String destination
    ) {}
}
