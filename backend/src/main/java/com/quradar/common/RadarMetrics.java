package com.quradar.common;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/** Domain metrics: what the radar network does, in numbers Prometheus can scrape. */
@Component
public class RadarMetrics {

    private final Counter eventsReceived;
    private final Counter eventsDuplicate;
    private final Counter eventsBadDevice;
    private final Counter violationsCreated;
    private final Counter finesCreated;
    private final Timer ruleEvaluation;

    public RadarMetrics(MeterRegistry registry) {
        this.eventsReceived = Counter.builder("quradar.events.received")
                .description("Observations accepted for evaluation").register(registry);
        this.eventsDuplicate = Counter.builder("quradar.events.rejected")
                .description("Observations rejected before evaluation")
                .tag("reason", "duplicate").register(registry);
        this.eventsBadDevice = Counter.builder("quradar.events.rejected")
                .description("Observations rejected before evaluation")
                .tag("reason", "device").register(registry);
        this.violationsCreated = Counter.builder("quradar.violations.created")
                .description("Violations persisted").register(registry);
        this.finesCreated = Counter.builder("quradar.fines.created")
                .description("Fines persisted").register(registry);
        this.ruleEvaluation = Timer.builder("quradar.rules.evaluation.duration")
                .description("Time spent evaluating rules per observation")
                .register(registry);
    }

    public void received() {
        eventsReceived.increment();
    }

    public void duplicate() {
        eventsDuplicate.increment();
    }

    public void badDevice() {
        eventsBadDevice.increment();
    }

    public void created(int violationCount) {
        finesCreated.increment();
        violationsCreated.increment(violationCount);
    }

    public <T> T timed(Supplier<T> evaluation) {
        return ruleEvaluation.record(evaluation);
    }
}
