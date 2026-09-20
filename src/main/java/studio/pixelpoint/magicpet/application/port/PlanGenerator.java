package studio.pixelpoint.magicpet.application.port;

import studio.pixelpoint.magicpet.domain.GeneratedPlan;
import studio.pixelpoint.magicpet.domain.Scenario;

public interface PlanGenerator {
    GeneratedPlan generate(Scenario scenario, String goalText);
}
