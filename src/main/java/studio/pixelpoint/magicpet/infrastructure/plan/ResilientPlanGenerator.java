package studio.pixelpoint.magicpet.infrastructure.plan;

import studio.pixelpoint.magicpet.application.port.PlanGenerator;
import studio.pixelpoint.magicpet.domain.GeneratedPlan;
import studio.pixelpoint.magicpet.domain.Scenario;
import studio.pixelpoint.magicpet.infrastructure.logging.SafeLogger;

import java.util.Objects;

public final class ResilientPlanGenerator implements PlanGenerator {
    private final PlanGenerator primary;
    private final PlanGenerator fallback;
    private final SafeLogger logger;

    public ResilientPlanGenerator(PlanGenerator primary, PlanGenerator fallback) {
        this(primary, fallback, null);
    }

    public ResilientPlanGenerator(PlanGenerator primary, PlanGenerator fallback, SafeLogger logger) {
        this.primary = Objects.requireNonNull(primary);
        this.fallback = Objects.requireNonNull(fallback);
        this.logger = logger;
    }

    @Override
    public GeneratedPlan generate(Scenario scenario, String goalText) {
        try {
            GeneratedPlan plan = primary.generate(scenario, goalText);
            if (logger != null) logger.info("ai.plan_generated");
            return plan;
        } catch (RuntimeException error) {
            if (logger != null) logger.error("ai.plan_fallback", error);
            return fallback.generate(scenario, goalText);
        }
    }
}
