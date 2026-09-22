package com.wincodes.optionsfinder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mock.sources")
public class MockSourceBehaviorProperties {

    private Behavior internalFlights = new Behavior();
    private Behavior externalFlights = new Behavior();
    private Behavior trains = new Behavior();

    public Behavior getInternalFlights() {
        return internalFlights;
    }

    public void setInternalFlights(Behavior internalFlights) {
        this.internalFlights = internalFlights;
    }

    public Behavior getExternalFlights() {
        return externalFlights;
    }

    public void setExternalFlights(Behavior externalFlights) {
        this.externalFlights = externalFlights;
    }

    public Behavior getTrains() {
        return trains;
    }

    public void setTrains(Behavior trains) {
        this.trains = trains;
    }

    public static class Behavior {
        private long latencyMs = 0;
        private Outcome outcome = Outcome.NORMAL;
        private long timeoutDelayMs = 3000;

        public long getLatencyMs() {
            return latencyMs;
        }

        public void setLatencyMs(long latencyMs) {
            this.latencyMs = latencyMs;
        }

        public Outcome getOutcome() {
            return outcome;
        }

        public void setOutcome(Outcome outcome) {
            this.outcome = outcome;
        }

        public long getTimeoutDelayMs() {
            return timeoutDelayMs;
        }

        public void setTimeoutDelayMs(long timeoutDelayMs) {
            this.timeoutDelayMs = timeoutDelayMs;
        }
    }

    public enum Outcome {
        NORMAL,
        EMPTY,
        FAIL,
        TIMEOUT
    }
}
