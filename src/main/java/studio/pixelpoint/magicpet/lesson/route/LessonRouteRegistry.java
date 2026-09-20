package studio.pixelpoint.magicpet.lesson.route;

import studio.pixelpoint.magicpet.application.IncomingMessage;
import studio.pixelpoint.magicpet.domain.Scenario;

import java.util.List;

public final class LessonRouteRegistry {
    private final List<LessonRoute> routes = List.of(new StudyLesson(), new SportLesson(), new BlogLesson());

    public LessonRoute findSelection(IncomingMessage message) {
        return routes.stream().filter(route -> route.selectionPressed(message)).findFirst().orElse(null);
    }

    public LessonRoute forScenario(Scenario scenario) {
        if (scenario == null) return null;
        return routes.stream().filter(route -> route.scenario() == scenario).findFirst().orElse(null);
    }
}
