package com.wincodes.optionsfinder.sources;

import com.wincodes.optionsfinder.config.MockSourceBehaviorProperties;
import com.wincodes.optionsfinder.models.Source;
import com.wincodes.optionsfinder.models.SourceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

@Component
public class MockSourceBehaviorApplier {

    private static final Logger log =
            LoggerFactory.getLogger(MockSourceBehaviorApplier.class);

    public void applyLatency(
            Source source,
            MockSourceBehaviorProperties.Behavior behavior
    ) {
        if (behavior.getLatencyMs() > 0) {
            log.info(
                    "Applying mock latency: source={}, latencyMs={}",
                    source,
                    behavior.getLatencyMs()
            );
        }
        sleep(behavior.getLatencyMs());
    }

    public SourceResult maybeInjectResult(
            MockSourceBehaviorProperties.Behavior behavior,
            Source source,
            long start
    ) {
        return switch (behavior.getOutcome()) {
            case NORMAL -> null;
            case EMPTY -> {
                log.info(
                        "Injecting empty result: source={}",
                        source
                );
                yield SourceResult.success(
                        source,
                        List.of(),
                        elapsed(start)
                );
            }
            case FAIL -> {
                log.warn(
                        "Injecting failure result: source={}",
                        source
                );
                yield SourceResult.failure(
                        source,
                        "Injected mock source failure",
                        elapsed(start)
                );
            }
            case TIMEOUT -> {
                sleep(behavior.getTimeoutDelayMs());
                log.warn(
                        "Late source failure after timeout: source={}, elapsedMs={}",
                        source,
                        behavior.getTimeoutDelayMs()
                );
                throw new CompletionException(
                        new TimeoutException(
                                "Injected mock source timeout"
                        )
                );
            }
        };
    }

    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }

    private void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Interrupted while injecting mock source behavior",
                    e
            );
        }
    }
}
