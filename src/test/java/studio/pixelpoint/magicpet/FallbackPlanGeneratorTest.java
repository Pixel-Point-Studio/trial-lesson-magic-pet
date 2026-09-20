package studio.pixelpoint.magicpet;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import studio.pixelpoint.magicpet.domain.Scenario;
import studio.pixelpoint.magicpet.infrastructure.plan.FallbackPlanGenerator;

import static org.junit.jupiter.api.Assertions.*;

class FallbackPlanGeneratorTest {
    @ParameterizedTest
    @EnumSource(Scenario.class)
    void everyScenarioHasThreeCompleteTasks(Scenario scenario) {
        var plan = new FallbackPlanGenerator().generate(scenario, "моя цель");
        assertEquals(3, plan.tasks().size());
        assertTrue(plan.tasks().stream().allMatch(task -> !task.title().isBlank() && !task.description().isBlank()));
    }
}
