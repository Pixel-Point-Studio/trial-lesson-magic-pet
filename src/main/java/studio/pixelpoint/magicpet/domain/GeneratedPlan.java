package studio.pixelpoint.magicpet.domain;

import java.util.List;

public record GeneratedPlan(String summary, List<PlanTask> tasks) {
    public GeneratedPlan {
        tasks = List.copyOf(tasks);
        if (summary == null || summary.isBlank() || tasks.size() != 3) {
            throw new IllegalArgumentException("План должен содержать описание и ровно три задачи");
        }
    }

    public PlanSource source() {
        return tasks.getFirst().source();
    }
}
