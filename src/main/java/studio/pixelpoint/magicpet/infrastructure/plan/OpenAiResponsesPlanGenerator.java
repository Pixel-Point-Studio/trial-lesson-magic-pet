package studio.pixelpoint.magicpet.infrastructure.plan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import studio.pixelpoint.magicpet.application.PromptResource;
import studio.pixelpoint.magicpet.application.port.PlanGenerator;
import studio.pixelpoint.magicpet.domain.GeneratedPlan;
import studio.pixelpoint.magicpet.domain.Scenario;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Direct client for OpenAI Responses API with Structured Outputs. */
public final class OpenAiResponsesPlanGenerator implements PlanGenerator {
    private final HttpClient client;
    private final URI endpoint;
    private final String apiKey;
    private final String model;
    private final Duration timeout;
    private final ObjectMapper mapper;
    private final AiPlanResponseValidator validator;

    public OpenAiResponsesPlanGenerator(URI endpoint, String apiKey, String model, Duration timeout) {
        this(HttpClient.newBuilder().connectTimeout(timeout).build(), endpoint, apiKey, model, timeout,
                new ObjectMapper(), new AiPlanResponseValidator());
    }

    OpenAiResponsesPlanGenerator(HttpClient client, URI endpoint, String apiKey, String model, Duration timeout,
                                 ObjectMapper mapper, AiPlanResponseValidator validator) {
        this.client = client;
        this.endpoint = endpoint;
        this.apiKey = apiKey == null ? "" : apiKey;
        this.model = model == null ? "" : model;
        this.timeout = timeout;
        this.mapper = mapper;
        this.validator = validator;
        if (!"http".equalsIgnoreCase(endpoint.getScheme()) && !"https".equalsIgnoreCase(endpoint.getScheme())) {
            throw new IllegalArgumentException("LLM_API_URL должен использовать http или https");
        }
        if (this.apiKey.isBlank()) throw new IllegalArgumentException("Для OpenAI нужен LLM_API_KEY");
        if (this.model.isBlank()) throw new IllegalArgumentException("Для OpenAI нужен LLM_MODEL");
    }

    @Override
    public GeneratedPlan generate(Scenario scenario, String goalText) {
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody(scenario, goalText)))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new OpenAiRequestException("OpenAI вернул HTTP " + response.statusCode());
            }
            return validator.validate(extractOutputText(response.body()), scenario);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new OpenAiRequestException("OpenAI-запрос прерван", error);
        } catch (IOException error) {
            throw new OpenAiRequestException("OpenAI недоступен", error);
        }
    }

    private String requestBody(Scenario scenario, String goalText) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.put("store", false);
        body.put("instructions", PromptResource.buildPrompt(scenario));
        body.put("input", "Сценарий: " + scenario.name() + "\nЦель пользователя: " + goalText);

        ObjectNode format = body.putObject("text").putObject("format");
        format.put("type", "json_schema");
        format.put("name", "magic_pet_plan");
        format.put("strict", true);
        format.set("schema", responseSchema());

        try {
            return mapper.writeValueAsString(body);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Не удалось собрать OpenAI-запрос", error);
        }
    }

    private ObjectNode responseSchema() {
        ObjectNode root = mapper.createObjectNode();
        root.put("type", "object");
        root.put("additionalProperties", false);
        ObjectNode properties = root.putObject("properties");
        properties.putObject("summary").put("type", "string");

        ObjectNode tasks = properties.putObject("tasks");
        tasks.put("type", "array");
        tasks.put("minItems", 3);
        tasks.put("maxItems", 3);
        ObjectNode item = tasks.putObject("items");
        item.put("type", "object");
        item.put("additionalProperties", false);
        ObjectNode taskProperties = item.putObject("properties");
        taskProperties.putObject("title").put("type", "string");
        taskProperties.putObject("description").put("type", "string");
        item.putArray("required").add("title").add("description");
        root.putArray("required").add("summary").add("tasks");
        return root;
    }

    private String extractOutputText(String responseBody) {
        JsonNode root;
        try {
            root = mapper.readTree(responseBody);
        } catch (JsonProcessingException error) {
            throw new OpenAiRequestException("OpenAI вернул невалидный JSON", error);
        }
        if (!"completed".equals(root.path("status").asText())) {
            throw new OpenAiRequestException("OpenAI не завершил генерацию");
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode item : root.path("output")) {
            if (!"message".equals(item.path("type").asText())) continue;
            for (JsonNode content : item.path("content")) {
                if ("refusal".equals(content.path("type").asText())) {
                    throw new OpenAiRequestException("OpenAI отказался генерировать план");
                }
                if ("output_text".equals(content.path("type").asText()) && content.path("text").isTextual()) {
                    text.append(content.path("text").asText());
                }
            }
        }
        if (text.isEmpty()) throw new OpenAiRequestException("OpenAI не вернул текст плана");
        return text.toString();
    }

    static final class OpenAiRequestException extends IllegalStateException {
        OpenAiRequestException(String message) { super(message); }
        OpenAiRequestException(String message, Throwable cause) { super(message, cause); }
    }
}
