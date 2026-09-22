package com.wincodes.optionsfinder.sources;

import com.wincodes.optionsfinder.config.MockSourceBehaviorProperties;
import com.wincodes.optionsfinder.dtos.external.ExternalFlightResponse;
import com.wincodes.optionsfinder.models.*;
import com.wincodes.optionsfinder.services.AirportTimeZoneRegistry;
import tools.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Component
public class ExternalFlightSource implements OptionSource {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ObjectMapper objectMapper;
    private final ExecutorService executor;
    private final AirportTimeZoneRegistry airportTimeZoneRegistry;
    private final MockSourceBehaviorProperties behaviorProperties;
    private final MockSourceBehaviorApplier behaviorApplier;

    public ExternalFlightSource(
            ObjectMapper objectMapper,
            ExecutorService executor,
            AirportTimeZoneRegistry airportTimeZoneRegistry,
            MockSourceBehaviorProperties behaviorProperties,
            MockSourceBehaviorApplier behaviorApplier
    ) {
        this.objectMapper = objectMapper;
        this.executor = executor;
        this.airportTimeZoneRegistry = airportTimeZoneRegistry;
        this.behaviorProperties = behaviorProperties;
        this.behaviorApplier = behaviorApplier;
    }

    @Override
    public Source source() {
        return Source.EXTERNAL_FLIGHTS;
    }

    @Override
    public CompletableFuture<SourceResult> search(
            SearchRequest request
    ) {

        return CompletableFuture.supplyAsync(() -> {

            long start = System.currentTimeMillis();
            MockSourceBehaviorProperties.Behavior behavior =
                    behaviorProperties.getExternalFlights();
            behaviorApplier.applyLatency(source(), behavior);

            SourceResult injected =
                    behaviorApplier.maybeInjectResult(
                            behavior,
                            source(),
                            start
                    );
            if (injected != null) {
                return injected;
            }

            try {

                ClassPathResource resource =
                        new ClassPathResource("data/external-flights.json");

                try (InputStream inputStream = resource.getInputStream()) {

                    ExternalFlightResponse response =
                            objectMapper.readValue(
                                    inputStream,
                                    ExternalFlightResponse.class
                            );

                    List<TravelOption> options =
                            response.results()
                                    .stream()
                                    .filter(offer ->
                                            offer.departureAirport()
                                                    .equalsIgnoreCase(request.origin())
                                    )
                                    .filter(offer ->
                                            offer.arrivalAirport()
                                                    .equalsIgnoreCase(request.destination())
                                    )
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
            ExternalFlightResponse.Offer offer,
            SearchRequest request
    ) {

        OffsetDateTime departure =
                parseDate(
                        offer.departureTime(),
                        offer.departureAirport()
                );

        OffsetDateTime arrival =
                parseDate(
                        offer.arrivalTime(),
                        offer.arrivalAirport()
                );

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
                Source.EXTERNAL_FLIGHTS,
                offer.airlineName(),
                offer.departureAirport(),
                offer.arrivalAirport(),
                departure,
                arrival,
                offer.availability(),
                new TravelOption.Money(
                        BigDecimal.valueOf(offer.priceCents())
                                .divide(BigDecimal.valueOf(100)),
                        offer.currency()
                ),
                legs,
                offer.note()
        );
    }

    private OffsetDateTime parseDate(
            String value,
            String airportCode
    ) {

        LocalDateTime dateTime =
                LocalDateTime.parse(value, FORMATTER);

        return dateTime.atZone(
                        airportTimeZoneRegistry.zoneForAirport(
                                airportCode
                        )
                )
                .toOffsetDateTime();
    }
}
