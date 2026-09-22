package com.wincodes.optionsfinder.services;

import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Map;

@Component
public class AirportTimeZoneRegistry {

    private static final Map<String, ZoneId> AIRPORT_ZONES = Map.ofEntries(
            Map.entry("CGN", ZoneId.of("Europe/Berlin")),
            Map.entry("BER", ZoneId.of("Europe/Berlin")),
            Map.entry("DUS", ZoneId.of("Europe/Berlin")),
            Map.entry("FRA", ZoneId.of("Europe/Berlin")),
            Map.entry("MUC", ZoneId.of("Europe/Berlin")),
            Map.entry("VIE", ZoneId.of("Europe/Vienna")),
            Map.entry("ZRH", ZoneId.of("Europe/Zurich")),
            Map.entry("PMI", ZoneId.of("Europe/Madrid")),
            Map.entry("HAM", ZoneId.of("Europe/Berlin")),
            Map.entry("ATH", ZoneId.of("Europe/Athens")),
            Map.entry("DXB", ZoneId.of("Asia/Dubai"))
    );

    public ZoneId zoneForAirport(String airportCode) {
        ZoneId zone = AIRPORT_ZONES.get(airportCode.toUpperCase());
        if (zone == null) {
            throw new IllegalArgumentException(
                    "Unknown timezone mapping for airport: " + airportCode
            );
        }
        return zone;
    }
}
