package studio.pixelpoint.magicpet.infrastructure.plan;

import studio.pixelpoint.magicpet.application.port.PlanGenerator;
import studio.pixelpoint.magicpet.domain.GeneratedPlan;
import studio.pixelpoint.magicpet.domain.Scenario;

import java.util.Objects;

public final class ResilientPlanGenerator implements PlanGenerator {
    private final PlanGenerator primary;
    private final PlanGenerator fallback;

    public ResilientPlanGenerator(PlanGenerator primary, PlanGenerator fallback) {
        this.primary = Objects.requireNonNull(primary);
        this.fallback = Objects.requireNonNull(fallback);
    }

    @Override
    public GeneratedPlan generate(Scenario scenario, String goalText) {
        try {
            return primary.generate(scenario, goalText);
        } catch (RuntimeException ignored) {
            return fallback.generate(scenario, goalText);
        }
    }
}
