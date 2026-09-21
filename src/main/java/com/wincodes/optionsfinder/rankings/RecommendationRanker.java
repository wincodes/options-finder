package com.wincodes.optionsfinder.rankings;

import com.wincodes.optionsfinder.models.TravelOption;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

@Component
public class RecommendationRanker {

    public List<TravelOption> rank(
            List<TravelOption> options,
            int passengers,
            OffsetDateTime earliestDeparture
    ) {

        return options.stream()

                // Must have enough seats
                .filter(option ->
                        option.availableSeats() >= passengers
                )

                // Must depart after the required time
                .filter(option ->
                        !option.departure()
                                .isBefore(earliestDeparture)
                )

                .sorted(
                        Comparator
                                .comparing(
                                        TravelOption::arrival
                                )
                                .thenComparing(
                                        TravelOption::availableSeats,
                                        Comparator.reverseOrder()
                                )
                                .thenComparing(
                                        TravelOption::id
                                )
                )

                .toList();
    }
}
