package studio.pixelpoint.magicpet.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public final class PromptResource {
    private PromptResource() {}

    public static void validate() {
        String resource = "/prompts/plan-generator.ru.json";
        try (InputStream stream = PromptResource.class.getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("Не найден ресурс " + resource);
            JsonNode root = new ObjectMapper().readTree(stream);
            require(root.path("version").isInt(), "version");
            require(root.path("system").isTextual(), "system");
            require(root.path("rules").isArray() && root.path("rules").size() >= 4, "rules");
            for (String scenario : new String[]{"STUDY", "SPORT", "BLOG"}) {
                require(root.path("scenarioRules").path(scenario).isArray(), "scenarioRules." + scenario);
            }
            require(root.path("responseContract").path("summary").isTextual(), "responseContract.summary");
            require(root.path("responseContract").path("tasks").isArray(), "responseContract.tasks");
        } catch (IOException error) {
            throw new IllegalStateException("Не удалось прочитать " + resource, error);
        }
    }

    private static void require(boolean condition, String key) {
        if (!condition) throw new IllegalStateException("Некорректный ресурс промпта: " + key);
    }
}
