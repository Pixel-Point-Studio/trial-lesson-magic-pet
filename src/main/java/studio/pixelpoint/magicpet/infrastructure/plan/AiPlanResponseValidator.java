package studio.pixelpoint.magicpet.infrastructure.plan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import studio.pixelpoint.magicpet.domain.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AiPlanResponseValidator {
    private static final Set<String> ROOT_FIELDS = Set.of("summary", "tasks");
    private static final Set<String> TASK_FIELDS = Set.of("title", "description");
    private static final List<String> UNSAFE_SPORT_PHRASES = List.of(
            "через боль", "до отказа", "максимальный вес", "без воды", "не пей",
            "задержи дыхание", "голодай", "лекарств", "обезболивающ", "экстремальн",
            "through pain", "maximum weight", "no water", "hold your breath"
    );

    private final ObjectMapper mapper = new ObjectMapper();

    public GeneratedPlan validate(String json, Scenario scenario) {
        JsonNode root;
        try {
            root = mapper.readTree(json);
        } catch (JsonProcessingException error) {
            throw new IllegalArgumentException("AI вернул невалидный JSON", error);
        }
        require(root != null && root.isObject() && fields(root).equals(ROOT_FIELDS), "Некорректные поля ответа");
        String summary = text(root.path("summary"), 1, 300, "summary");
        JsonNode taskNodes = root.path("tasks");
        require(taskNodes.isArray() && taskNodes.size() == 3, "AI должен вернуть ровно три задачи");

        List<PlanTask> tasks = new ArrayList<>();
        for (JsonNode taskNode : taskNodes) {
            require(taskNode.isObject() && fields(taskNode).equals(TASK_FIELDS), "Некорректные поля задачи");
            String title = text(taskNode.path("title"), 1, 80, "title");
            String description = text(taskNode.path("description"), 1, 500, "description");
            tasks.add(new PlanTask(title, description, PlanSource.AI));
        }
        if (scenario == Scenario.SPORT) validateSportSafety(summary, tasks);
        return new GeneratedPlan(summary, tasks);
    }

    private void validateSportSafety(String summary, List<PlanTask> tasks) {
        StringBuilder content = new StringBuilder(summary);
        tasks.forEach(task -> content.append(' ').append(task.title()).append(' ').append(task.description()));
        String normalized = content.toString().toLowerCase(Locale.ROOT);
        boolean unsafe = UNSAFE_SPORT_PHRASES.stream().anyMatch(normalized::contains);
        require(!unsafe, "AI вернул потенциально небезопасный спортивный совет");
    }

    private String text(JsonNode node, int min, int max, String field) {
        require(node.isTextual(), "Поле " + field + " должно быть строкой");
        String value = node.asText().trim();
        require(value.length() >= min && value.length() <= max, "Некорректная длина поля " + field);
        return value;
    }

    private Set<String> fields(JsonNode node) {
        var names = new java.util.HashSet<String>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
