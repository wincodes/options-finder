package com.wincodes.optionsfinder.services;

import com.wincodes.optionsfinder.models.*;
import com.wincodes.optionsfinder.rankings.RecommendationRanker;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RecommendationServiceTest {

    @Test
    void groupsBookingsByRoute() {
        RecordingSearchOrchestrator orchestrator = new RecordingSearchOrchestrator(Map.of(
                "CGN->BER", List.of(SourceResult.success(Source.INTERNAL_FLIGHTS, List.of(), 10)),
                "PMI->BER", List.of(SourceResult.success(Source.INTERNAL_FLIGHTS, List.of(), 10))
        ));

        RecommendationService service =
                new RecommendationService(orchestrator, new RecommendationRanker());

        List<Booking> bookings = List.of(
                booking("B1", 1, "CGN", "BER", "2026-07-21T18:35:00+02:00"),
                booking("B2", 1, "CGN", "BER", "2026-07-21T18:00:00+02:00"),
                booking("B3", 1, "PMI", "BER", "2026-07-21T17:10:00+02:00")
        );

        service.recommend(bookings);

        assertEquals(2, orchestrator.requests.size());
        assertTrue(orchestrator.requests.stream().anyMatch(r -> routeOf(r).equals("CGN->BER")));
        assertTrue(orchestrator.requests.stream().anyMatch(r -> routeOf(r).equals("PMI->BER")));
    }

    @Test
    void doesNotSearchSameRouteRepeatedly() {
        RecordingSearchOrchestrator orchestrator = new RecordingSearchOrchestrator(Map.of(
                "CGN->BER", List.of(SourceResult.success(Source.INTERNAL_FLIGHTS, List.of(), 10))
        ));

        RecommendationService service =
                new RecommendationService(orchestrator, new RecommendationRanker());

        List<Booking> bookings = List.of(
                booking("B1", 1, "CGN", "BER", "2026-07-21T18:35:00+02:00"),
                booking("B2", 2, "CGN", "BER", "2026-07-21T18:00:00+02:00"),
                booking("B3", 3, "CGN", "BER", "2026-07-21T19:10:00+02:00")
        );

        service.recommend(bookings);

        assertEquals(1, orchestrator.requests.size());
    }

    @Test
    void producesRecommendationPerBooking() {
        TravelOption option = option("OPT-1", 10, "2026-07-21T20:30:00+02:00", "2026-07-21T21:40:00+02:00");
        RecordingSearchOrchestrator orchestrator = new RecordingSearchOrchestrator(Map.of(
                "CGN->BER", List.of(SourceResult.success(Source.INTERNAL_FLIGHTS, List.of(option), 10))
        ));

        RecommendationService service =
                new RecommendationService(orchestrator, new RecommendationRanker());

        List<Booking> bookings = List.of(
                booking("B1", 1, "CGN", "BER", "2026-07-21T18:35:00+02:00"),
                booking("B2", 2, "CGN", "BER", "2026-07-21T18:35:00+02:00")
        );

        List<Recommendation> recommendations = service.recommend(bookings);

        assertEquals(2, recommendations.size());
        assertEquals(Set.of("B1", "B2"), recommendations.stream().map(Recommendation::bookingRef).collect(java.util.stream.Collectors.toSet()));
        assertTrue(recommendations.stream().allMatch(r -> r.recommendation() != null));
    }

    @Test
    void handlesNoAlternatives() {
        RecordingSearchOrchestrator orchestrator = new RecordingSearchOrchestrator(Map.of(
                "CGN->BER", List.of(SourceResult.success(Source.INTERNAL_FLIGHTS, List.of(), 10))
        ));

        RecommendationService service =
                new RecommendationService(orchestrator, new RecommendationRanker());

        List<Recommendation> recommendations = service.recommend(List.of(
                booking("B1", 2, "CGN", "BER", "2026-07-21T18:35:00+02:00")
        ));

        Recommendation recommendation = recommendations.get(0);
        assertNull(recommendation.recommendation());
        assertEquals("No alternative available", recommendation.reason());
        assertTrue(recommendation.alternatives().isEmpty());
    }

    @Test
    void handlesInsufficientCapacity() {
        TravelOption tooSmall = option("OPT-1", 1, "2026-07-21T20:30:00+02:00", "2026-07-21T21:40:00+02:00");
        RecordingSearchOrchestrator orchestrator = new RecordingSearchOrchestrator(Map.of(
                "CGN->BER", List.of(SourceResult.success(Source.INTERNAL_FLIGHTS, List.of(tooSmall), 10))
        ));

        RecommendationService service =
                new RecommendationService(orchestrator, new RecommendationRanker());

        List<Recommendation> recommendations = service.recommend(List.of(
                booking("B1", 3, "CGN", "BER", "2026-07-21T18:35:00+02:00")
        ));

        Recommendation recommendation = recommendations.get(0);
        assertNull(recommendation.recommendation());
        assertEquals("No alternative has sufficient capacity", recommendation.reason());
        assertTrue(recommendation.alternatives().isEmpty());
    }

    @Test
    void usesEarliestDepartureWithinRouteForSearchRequest() {
        RecordingSearchOrchestrator orchestrator = new RecordingSearchOrchestrator(Map.of(
                "CGN->BER", List.of(SourceResult.success(Source.INTERNAL_FLIGHTS, List.of(), 10))
        ));

        RecommendationService service =
                new RecommendationService(orchestrator, new RecommendationRanker());

        service.recommend(List.of(
                booking("B1", 1, "CGN", "BER", "2026-07-21T19:35:00+02:00"),
                booking("B2", 1, "CGN", "BER", "2026-07-21T18:35:00+02:00"),
                booking("B3", 1, "CGN", "BER", "2026-07-21T20:35:00+02:00")
        ));

        assertEquals(1, orchestrator.requests.size());
        assertEquals(
                OffsetDateTime.parse("2026-07-21T18:35:00+02:00"),
                orchestrator.requests.get(0).earliestDeparture()
        );
    }

    private Booking booking(
            String ref,
            int passengers,
            String origin,
            String destination,
            String cancelledDeparture
    ) {
        Journey.Segment cancelled = new Journey.Segment(
                "EW 4711",
                origin,
                destination,
                OffsetDateTime.parse(cancelledDeparture),
                OffsetDateTime.parse(cancelledDeparture).plusHours(1),
                "CANCELLED"
        );

        return new Booking(ref, passengers, new Journey(origin, destination, List.of(cancelled)));
    }

    private TravelOption option(
            String id,
            int seats,
            String departure,
            String arrival
    ) {
        return new TravelOption(
                id,
                Source.INTERNAL_FLIGHTS,
                "Provider",
                "CGN",
                "BER",
                OffsetDateTime.parse(departure),
                OffsetDateTime.parse(arrival),
                seats,
                new TravelOption.Money(BigDecimal.valueOf(100), "EUR"),
                List.of(),
                null,
                false
        );
    }

    private String routeOf(SearchRequest request) {
        return request.origin() + "->" + request.destination();
    }

    private static class RecordingSearchOrchestrator extends SearchOrchestrator {
        private final Map<String, List<SourceResult>> responses;
        private final List<SearchRequest> requests = new ArrayList<>();

        private RecordingSearchOrchestrator(Map<String, List<SourceResult>> responses) {
            super(List.of());
            this.responses = responses;
        }

        @Override
        public List<SourceResult> search(SearchRequest request) {
            requests.add(request);
            return responses.getOrDefault(routeOf(request), List.of());
        }

        private static String routeOf(SearchRequest request) {
            return request.origin() + "->" + request.destination();
        }
    }
}
