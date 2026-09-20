package studio.pixelpoint.magicpet.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public final class UiTexts {
    private final JsonNode root;

    private UiTexts(JsonNode root) {
        this.root = root;
        require("buttons", "scenarioStudy", "scenarioSport", "scenarioBlog", "taskDone",
                "myTasks", "addTask", "completedTasks", "deleteTask", "back");
        require("messages", "chooseScenario", "askPetName", "askGoal", "fallbackPlanUsed",
                "invalidName", "invalidGoal", "alreadyCompleted", "planCompleted", "activeTasks",
                "completedTasks", "noActiveTasks", "noCompletedTasks", "askTaskTitle", "askTaskDescription",
                "invalidTaskTitle", "invalidTaskDescription", "taskAdded", "taskDeleted", "taskNotFound");
        require("templates", "planReady", "task", "taskCard", "progress", "levelUp", "petText");
    }

    public static UiTexts load() {
        return load("/content/ui-texts.ru.json");
    }

    static UiTexts load(String resource) {
        try (InputStream stream = UiTexts.class.getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("Не найден ресурс " + resource);
            return new UiTexts(new ObjectMapper().readTree(stream));
        } catch (IOException error) {
            throw new IllegalStateException("Не удалось прочитать " + resource, error);
        }
    }

    public String button(String key) { return value("buttons", key); }
    public String message(String key) { return value("messages", key); }

    public String template(String key, Map<String, ?> values) {
        String result = value("templates", key);
        for (var entry : values.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }

    private String value(String section, String key) {
        JsonNode node = root.path(section).path(key);
        if (!node.isTextual() || node.asText().isBlank()) {
            throw new IllegalStateException("Отсутствует обязательный текст: " + section + "." + key);
        }
        return node.asText();
    }

    private void require(String section, String... keys) {
        for (String key : keys) value(section, key);
    }
}
