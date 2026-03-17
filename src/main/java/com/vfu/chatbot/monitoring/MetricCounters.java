package com.vfu.chatbot.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MetricCounters {
    private final MeterRegistry meterRegistry;

    private final Counter agentEscalationsCount;

    public MetricCounters(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.agentEscalationsCount = Counter.builder("chatbot.escalations.agent")
                .description("Low confidence → agent escalation")
                .register(meterRegistry);
    }

    public void incrementAgentEscalations() {
        agentEscalationsCount.increment();
    }

}
