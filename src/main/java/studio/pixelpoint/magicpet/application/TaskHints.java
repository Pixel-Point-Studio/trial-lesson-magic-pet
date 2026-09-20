package studio.pixelpoint.magicpet.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import studio.pixelpoint.magicpet.domain.Scenario;

import java.io.IOException;
import java.io.InputStream;

public final class TaskHints {
    private final JsonNode root;

    private TaskHints(JsonNode root) {
        this.root = root;
    }

    public static TaskHints load() {
        try (InputStream stream = TaskHints.class.getResourceAsStream("/content/task-hints.ru.json")) {
            if (stream == null) throw new IllegalStateException("Не найден ресурс task-hints.ru.json");
            return new TaskHints(new ObjectMapper().readTree(stream));
        } catch (IOException error) {
            throw new IllegalStateException("Не удалось прочитать task-hints.ru.json", error);
        }
    }

    public String hint(Scenario scenario, int position) {
        JsonNode hints = root.path(scenario.name().toLowerCase()).path(position);
        if (!hints.isArray() || hints.isEmpty()) return root.path("custom").asText();
        return hints.get(Math.floorMod(position, hints.size())).asText();
    }

    public String customHint() {
        return root.path("custom").asText();
    }
}
