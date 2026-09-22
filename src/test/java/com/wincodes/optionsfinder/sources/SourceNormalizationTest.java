package com.wincodes.optionsfinder.sources;

import com.wincodes.optionsfinder.models.SearchRequest;
import com.wincodes.optionsfinder.models.SourceResult;
import com.wincodes.optionsfinder.models.SourceStatus;
import com.wincodes.optionsfinder.models.TravelOption;
import com.wincodes.optionsfinder.services.AirportTimeZoneRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class SourceNormalizationTest {

    private ExecutorService executor;
    private ObjectMapper objectMapper;
    private AirportTimeZoneRegistry airportTimeZoneRegistry;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(3);
        objectMapper = new ObjectMapper();
        airportTimeZoneRegistry = new AirportTimeZoneRegistry();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void internalSourceNormalizesOffersToTravelOption() {
        InternalFlightSource source = new InternalFlightSource(objectMapper, executor);
        SearchRequest request = new SearchRequest("CGN", "BER", OffsetDateTime.parse("2026-07-21T18:35:00+02:00"));

        SourceResult result = source.search(request).join();
        TravelOption int1 = result.options().stream()
                .filter(option -> "INT-1".equals(option.id()))
                .findFirst()
                .orElseThrow();

        assertEquals(SourceStatus.SUCCESS, result.status());
        assertEquals("Lufthansa", int1.provider());
        assertEquals(2, int1.legs().size());
        assertEquals(new BigDecimal("214.0"), int1.price().amount());
    }

    @Test
    void externalSourceParsesCustomDateAndPriceFormat() {
        ExternalFlightSource source = new ExternalFlightSource(
                objectMapper,
                executor,
                airportTimeZoneRegistry
        );
        SearchRequest request = new SearchRequest("CGN", "BER", OffsetDateTime.parse("2026-07-21T18:35:00+02:00"));

        SourceResult result = source.search(request).join();
        TravelOption ext1 = result.options().stream()
                .filter(option -> "EXT-1".equals(option.id()))
                .findFirst()
                .orElseThrow();

        assertEquals(SourceStatus.SUCCESS, result.status());
        assertEquals(ZoneOffset.ofHours(2), ext1.departure().getOffset());
        assertEquals(ZoneOffset.ofHours(2), ext1.arrival().getOffset());
        assertEquals(new BigDecimal("189"), ext1.price().amount());
        assertEquals("EUR", ext1.price().currency());

        SearchRequest dxbRequest = new SearchRequest("CGN", "DXB", OffsetDateTime.parse("2026-07-21T18:35:00+02:00"));
        SourceResult dxbResult = source.search(dxbRequest).join();
        TravelOption ext4 = dxbResult.options().stream()
                .filter(option -> "EXT-4".equals(option.id()))
                .findFirst()
                .orElseThrow();
        assertEquals(ZoneOffset.ofHours(4), ext4.arrival().getOffset());
    }

    @Test
    void trainSourceParsesPriceAndSingleLegConnection() {
        TrainSource source = new TrainSource(
                objectMapper,
                executor,
                airportTimeZoneRegistry
        );
        SearchRequest request = new SearchRequest("CGN", "BER", OffsetDateTime.parse("2026-07-21T18:35:00+02:00"));

        SourceResult result = source.search(request).join();
        TravelOption trn2 = result.options().stream()
                .filter(option -> "TRN-2".equals(option.id()))
                .findFirst()
                .orElseThrow();

        assertEquals(SourceStatus.SUCCESS, result.status());
        assertEquals(new BigDecimal("79.90"), trn2.price().amount());
        assertEquals("change in Hannover Hbf", trn2.note());
        assertEquals(1, trn2.legs().size());
        assertEquals(List.of("RX 1188"), trn2.legs().stream().map(TravelOption.Leg::identifier).toList());
    }
}
