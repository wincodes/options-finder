package com.wincodes.optionsfinder.rankings;

import com.wincodes.optionsfinder.models.Source;
import com.wincodes.optionsfinder.models.TravelOption;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationRankerTest {

    private final RecommendationRanker ranker = new RecommendationRanker();

    @Test
    void rejectsInsufficientCapacity() {
        TravelOption lowCapacity = option(
                "A",
                "2026-07-21T20:00:00+02:00",
                "2026-07-21T21:00:00+02:00",
                1
        );

        List<TravelOption> ranked = ranker.rank(
                List.of(lowCapacity),
                2,
                OffsetDateTime.parse("2026-07-21T18:35:00+02:00")
        );

        assertTrue(ranked.isEmpty());
    }

    @Test
    void selectsEarliestArrival() {
        TravelOption later = option(
                "LATER",
                "2026-07-21T20:00:00+02:00",
                "2026-07-21T22:00:00+02:00",
                5
        );
        TravelOption earlier = option(
                "EARLIER",
                "2026-07-21T20:10:00+02:00",
                "2026-07-21T21:30:00+02:00",
                5
        );

        List<TravelOption> ranked = ranker.rank(
                List.of(later, earlier),
                2,
                OffsetDateTime.parse("2026-07-21T18:35:00+02:00")
        );

        assertEquals("EARLIER", ranked.get(0).id());
    }

    @Test
    void prefersMoreSeatsWhenArrivalIsEqual() {
        TravelOption fewerSeats = option(
                "FEWER",
                "2026-07-21T20:00:00+02:00",
                "2026-07-21T21:30:00+02:00",
                5
        );
        TravelOption moreSeats = option(
                "MORE",
                "2026-07-21T19:50:00+02:00",
                "2026-07-21T21:30:00+02:00",
                10
        );

        List<TravelOption> ranked = ranker.rank(
                List.of(fewerSeats, moreSeats),
                2,
                OffsetDateTime.parse("2026-07-21T18:35:00+02:00")
        );

        assertEquals("MORE", ranked.get(0).id());
    }

    @Test
    void producesDeterministicOrdering() {
        TravelOption b = option(
                "B",
                "2026-07-21T20:00:00+02:00",
                "2026-07-21T21:30:00+02:00",
                10
        );
        TravelOption a = option(
                "A",
                "2026-07-21T20:00:00+02:00",
                "2026-07-21T21:30:00+02:00",
                10
        );

        for (int i = 0; i < 5; i++) {
            List<TravelOption> ranked = ranker.rank(
                    List.of(b, a),
                    2,
                    OffsetDateTime.parse("2026-07-21T18:35:00+02:00")
            );

            assertEquals(List.of("A", "B"), ranked.stream().map(TravelOption::id).toList());
        }
    }

    private TravelOption option(
            String id,
            String departure,
            String arrival,
            int seats
    ) {
        return new TravelOption(
                id,
                Source.INTERNAL_FLIGHTS,
                "Test Provider",
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
}
