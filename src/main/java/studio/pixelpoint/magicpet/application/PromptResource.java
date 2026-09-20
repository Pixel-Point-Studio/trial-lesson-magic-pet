package studio.pixelpoint.magicpet.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import studio.pixelpoint.magicpet.domain.Scenario;

import java.util.StringJoiner;

public final class PromptResource {
    private PromptResource() {}

    public static void validate() {
        validate(load());
    }

    public static String buildPrompt(Scenario scenario) {
        JsonNode root = load();
        validate(root);
        StringJoiner prompt = new StringJoiner("\n- ", "", "");
        prompt.add(root.path("system").asText());
        for (JsonNode rule : root.path("rules")) prompt.add(rule.asText());
        for (JsonNode rule : root.path("scenarioRules").path(scenario.name())) prompt.add(rule.asText());
        prompt.add("Ответ должен точно соответствовать JSON-контракту: "
                + root.path("responseContract").toString());
        return prompt.toString();
    }

    private static JsonNode load() {
        String resource = "/prompts/plan-generator.ru.json";
        try (InputStream stream = PromptResource.class.getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("Не найден ресурс " + resource);
            return new ObjectMapper().readTree(stream);
        } catch (IOException error) {
            throw new IllegalStateException("Не удалось прочитать " + resource, error);
        }
    }

    private static void validate(JsonNode root) {
        require(root.path("version").isInt(), "version");
        require(root.path("system").isTextual(), "system");
        require(root.path("rules").isArray() && root.path("rules").size() >= 4, "rules");
        for (String scenario : new String[]{"STUDY", "SPORT", "BLOG"}) {
            require(root.path("scenarioRules").path(scenario).isArray(), "scenarioRules." + scenario);
        }
        require(root.path("responseContract").path("summary").isTextual(), "responseContract.summary");
        require(root.path("responseContract").path("tasks").isArray(), "responseContract.tasks");
    }

    private static void require(boolean condition, String key) {
        if (!condition) throw new IllegalStateException("Некорректный ресурс промпта: " + key);
    }
}
