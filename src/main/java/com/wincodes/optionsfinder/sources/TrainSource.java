package com.wincodes.optionsfinder.sources;

import com.wincodes.optionsfinder.dtos.train.TrainResponse;
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
public class TrainSource implements OptionSource {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ObjectMapper objectMapper;
    private final ExecutorService executor;
    private final AirportTimeZoneRegistry airportTimeZoneRegistry;

    public TrainSource(
            ObjectMapper objectMapper,
            ExecutorService executor,
            AirportTimeZoneRegistry airportTimeZoneRegistry
    ) {
        this.objectMapper = objectMapper;
        this.executor = executor;
        this.airportTimeZoneRegistry = airportTimeZoneRegistry;
    }

    @Override
    public Source source() {
        return Source.TRAINS;
    }

    @Override
    public CompletableFuture<SourceResult> search(
            SearchRequest request
    ) {

        return CompletableFuture.supplyAsync(() -> {

            long start = System.currentTimeMillis();

            try {

                ClassPathResource resource =
                        new ClassPathResource("data/trains.json");

                try (InputStream inputStream = resource.getInputStream()) {

                    TrainResponse response =
                            objectMapper.readValue(
                                    inputStream,
                                    TrainResponse.class
                            );

                    List<TravelOption> options =
                            response.queries()
                                    .stream()
                                    .filter(query ->
                                            query.fromAirport()
                                                    .equalsIgnoreCase(request.origin())
                                    )
                                    .filter(query ->
                                            query.toAirport()
                                                    .equalsIgnoreCase(request.destination())
                                    )
                                    .flatMap(query ->
                                            query.connections().stream()
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
            TrainResponse.Connection connection,
            SearchRequest request
    ) {

        OffsetDateTime departure =
                parseDate(
                        connection.departure(),
                        request.origin()
                );

        OffsetDateTime arrival =
                parseDate(
                        connection.arrival(),
                        request.destination()
                );

        TravelOption.Leg leg =
                new TravelOption.Leg(
                        connection.trainNumber(),
                        connection.departureStation(),
                        connection.arrivalStation()
                );

        String note = connection.via();

        return new TravelOption(
                connection.id(),
                Source.TRAINS,
                connection.operator(),
                request.origin(),
                request.destination(),
                departure,
                arrival,
                connection.availableSeats(),
                parsePrice(connection.price()),
                List.of(leg),
                note
        );
    }

    private OffsetDateTime parseDate(
            String value,
            String airportCode
    ) {

        return LocalDateTime
                .parse(value, FORMATTER)
                .atZone(
                        airportTimeZoneRegistry.zoneForAirport(
                                airportCode
                        )
                )
                .toOffsetDateTime();
    }

    private TravelOption.Money parsePrice(String value) {

        String[] parts = value.split(" ");

        return new TravelOption.Money(
                new BigDecimal(parts[0]),
                parts[1]
        );
    }
}
