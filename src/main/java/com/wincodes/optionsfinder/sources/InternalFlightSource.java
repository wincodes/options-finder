package com.wincodes.optionsfinder.sources;

import com.wincodes.optionsfinder.dtos.internal.InternalFlightResponse;
import com.wincodes.optionsfinder.models.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Component
public class InternalFlightSource implements OptionSource {

    private final ObjectMapper objectMapper;
    private final ExecutorService executor;

    public InternalFlightSource(
            ObjectMapper objectMapper,
            ExecutorService executor
    ) {
        this.objectMapper = objectMapper;
        this.executor = executor;
    }

    @Override
    public Source source() {
        return Source.INTERNAL_FLIGHTS;
    }

    @Override
    public CompletableFuture<SourceResult> search(
            SearchRequest request
    ) {
        return CompletableFuture.supplyAsync(() -> {

            long start = System.currentTimeMillis();

            try {
                ClassPathResource resource =
                        new ClassPathResource("data/internal-flights.json");

                try (InputStream inputStream = resource.getInputStream()) {

                    InternalFlightResponse response =
                            objectMapper.readValue(
                                    inputStream,
                                    InternalFlightResponse.class
                            );

                    List<TravelOption> options =
                            response.searches()
                                    .stream()
                                    .filter(search ->
                                            search.query().origin()
                                                    .equalsIgnoreCase(request.origin())
                                                    &&
                                                    search.query().destination()
                                                            .equalsIgnoreCase(request.destination())
                                    )
                                    .flatMap(search -> search.offers().stream())
                                    .map(connection -> normalize(connection, request))
                                    .toList();

                    return SourceResult.success(
                            source(),
                            options,
                            System.currentTimeMillis() - start
                    );
                }

            } catch (Exception e) {

                return SourceResult.failure(
                        source(),
                        e.getMessage(),
                        System.currentTimeMillis() - start
                );
            }

        }, executor);
    }

    private TravelOption normalize(
            InternalFlightResponse.Offer offer,
            SearchRequest request
    ) {

        List<TravelOption.Leg> legs =
                offer.legs()
                        .stream()
                        .map(leg ->
                                new TravelOption.Leg(
                                        leg.flightNumber(),
                                        leg.from(),
                                        leg.to()
                                )
                        )
                        .toList();

        return new TravelOption(
                offer.offerId(),
                Source.INTERNAL_FLIGHTS,
                offer.carrier(),
                offer.departureAirport(),
                offer.arrivalAirport(),
                offer.departure(),
                offer.arrival(),
                offer.availableSeats(),
                new TravelOption.Money(
                        BigDecimal.valueOf(
                                offer.pricePerSeat().amount()
                        ),
                        offer.pricePerSeat().currency()
                ),
                legs,
                null,
                !offer.departureAirport()
                        .equalsIgnoreCase(request.origin())
        );
    }
}
