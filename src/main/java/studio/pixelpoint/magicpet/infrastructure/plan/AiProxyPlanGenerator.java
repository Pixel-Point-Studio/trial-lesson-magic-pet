package studio.pixelpoint.magicpet.infrastructure.plan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

public final class AiProxyPlanGenerator implements PlanGenerator {
    private final HttpClient client;
    private final URI endpoint;
    private final String apiKey;
    private final String model;
    private final Duration timeout;
    private final ObjectMapper mapper;
    private final AiPlanResponseValidator validator;

    public AiProxyPlanGenerator(URI endpoint, String apiKey, String model, Duration timeout) {
        this(HttpClient.newBuilder().connectTimeout(timeout).build(), endpoint, apiKey, model, timeout,
                new ObjectMapper(), new AiPlanResponseValidator());
    }

    AiProxyPlanGenerator(HttpClient client, URI endpoint, String apiKey, String model, Duration timeout,
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
    }

    @Override
    public GeneratedPlan generate(Scenario scenario, String goalText) {
        HttpRequest.Builder request = HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody(scenario, goalText)));
        if (!apiKey.isBlank()) request.header("Authorization", "Bearer " + apiKey);
        try {
            HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("AI-прокси вернул HTTP " + response.statusCode());
            }
            return validator.validate(response.body(), scenario);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI-запрос прерван", error);
        } catch (IOException error) {
            throw new IllegalStateException("AI-прокси недоступен", error);
        }
    }

    private String requestBody(Scenario scenario, String goalText) {
        ObjectNode body = mapper.createObjectNode();
        if (!model.isBlank()) body.put("model", model);
        body.put("scenario", scenario.name());
        body.put("goal", goalText);
        body.put("prompt", PromptResource.buildPrompt(scenario));
        try {
            return mapper.writeValueAsString(body);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Не удалось собрать AI-запрос", error);
        }
    }
}
