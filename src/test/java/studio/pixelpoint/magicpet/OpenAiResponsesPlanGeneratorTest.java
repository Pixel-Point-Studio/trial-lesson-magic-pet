package studio.pixelpoint.magicpet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import studio.pixelpoint.magicpet.application.port.PlanGenerator;
import studio.pixelpoint.magicpet.domain.PlanSource;
import studio.pixelpoint.magicpet.domain.Scenario;
import studio.pixelpoint.magicpet.infrastructure.logging.SafeLogger;
import studio.pixelpoint.magicpet.infrastructure.plan.FallbackPlanGenerator;
import studio.pixelpoint.magicpet.infrastructure.plan.OpenAiResponsesPlanGenerator;
import studio.pixelpoint.magicpet.infrastructure.plan.ResilientPlanGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiResponsesPlanGeneratorTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String VALID = """
            {"summary":"Персональный маршрут","tasks":[
              {"title":"Шаг 1","description":"Сделай первое короткое действие"},
              {"title":"Шаг 2","description":"Закрепи результат за две минуты"},
              {"title":"Шаг 3","description":"Запланируй продолжение"}
            ]}
            """;

    @Test
    void validResponsesApiOutputProducesAiPlanAndUsesStructuredSchema() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        try (TestServer server = server(exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, response(VALID));
        })) {
            var plan = ai(server.uri(), Duration.ofSeconds(1)).generate(Scenario.STUDY, "выучить Java");

            assertEquals(PlanSource.AI, plan.source());
            assertEquals(3, plan.tasks().size());
            assertEquals("Bearer test-secret", authorization.get());

            JsonNode body = MAPPER.readTree(requestBody.get());
            assertEquals("test-model", body.path("model").asText());
            assertFalse(body.path("store").asBoolean(true));
            assertTrue(body.path("input").asText().contains("выучить Java"));
            assertTrue(body.path("instructions").asText().contains("JSON-контракту"));
            assertEquals("json_schema", body.path("text").path("format").path("type").asText());
            assertEquals("magic_pet_plan", body.path("text").path("format").path("name").asText());
            assertTrue(body.path("text").path("format").path("strict").asBoolean());
            assertFalse(requestBody.get().contains("test-secret"));
            assertFalse(requestBody.get().contains("telegram"));
            assertFalse(requestBody.get().contains("username"));
        }
    }

    @Test
    void timeoutFallsBackAndWritesSafeLog() throws Exception {
        try (TestServer server = server(exchange -> {
            try { Thread.sleep(250); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            try { respond(exchange, 200, response(VALID)); } catch (IOException ignored) { }
        })) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            PlanGenerator generator = new ResilientPlanGenerator(
                    ai(server.uri(), Duration.ofMillis(30)), new FallbackPlanGenerator(),
                    new SafeLogger(new PrintStream(bytes, true, StandardCharsets.UTF_8)));

            assertFallback(generator, Scenario.STUDY);
            String log = bytes.toString(StandardCharsets.UTF_8);
            assertTrue(log.contains("ai.plan_fallback"));
            assertFalse(log.contains("test-secret"));
            assertFalse(log.contains("цель"));
        }
    }

    @Test
    void successfulGenerationWritesSafeLog() throws Exception {
        try (TestServer server = server(exchange -> respond(exchange, 200, response(VALID)))) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            PlanGenerator generator = new ResilientPlanGenerator(
                    ai(server.uri(), Duration.ofSeconds(1)), new FallbackPlanGenerator(),
                    new SafeLogger(new PrintStream(bytes, true, StandardCharsets.UTF_8)));

            assertEquals(PlanSource.AI, generator.generate(Scenario.BLOG, "секретная цель").source());
            String log = bytes.toString(StandardCharsets.UTF_8);
            assertTrue(log.contains("ai.plan_generated"));
            assertFalse(log.contains("секретная цель"));
        }
    }

    @Test
    void httpErrorFallsBack() throws Exception {
        try (TestServer server = server(exchange -> respond(exchange, 429, "{\"error\":{}}"))) {
            assertFallback(resilient(server.uri(), Duration.ofSeconds(1)), Scenario.BLOG);
        }
    }

    @Test
    void brokenStructuredOutputFallsBack() throws Exception {
        assertPayloadFallsBack("{broken", Scenario.STUDY);
    }

    @Test
    void twoTasksFallBack() throws Exception {
        assertPayloadFallsBack("""
                {"summary":"x","tasks":[
                  {"title":"1","description":"a"},{"title":"2","description":"b"}
                ]}
                """, Scenario.BLOG);
    }

    @Test
    void refusalFallsBack() throws Exception {
        try (TestServer server = server(exchange -> respond(exchange, 200, refusalResponse()))) {
            assertFallback(resilient(server.uri(), Duration.ofSeconds(1)), Scenario.STUDY);
        }
    }

    @Test
    void unsafeSportAdviceFallsBack() throws Exception {
        String unsafe = VALID.replace("Сделай первое короткое действие", "Тренируйся через боль и до отказа");
        assertPayloadFallsBack(unsafe, Scenario.SPORT);
    }

    private void assertPayloadFallsBack(String payload, Scenario scenario) throws Exception {
        try (TestServer server = server(exchange -> respond(exchange, 200, response(payload)))) {
            assertFallback(resilient(server.uri(), Duration.ofSeconds(1)), scenario);
        }
    }

    private void assertFallback(PlanGenerator generator, Scenario scenario) {
        var plan = generator.generate(scenario, "цель");
        assertEquals(PlanSource.FALLBACK, plan.source());
        assertEquals(3, plan.tasks().size());
    }

    private OpenAiResponsesPlanGenerator ai(URI uri, Duration timeout) {
        return new OpenAiResponsesPlanGenerator(uri, "test-secret", "test-model", timeout);
    }

    private PlanGenerator resilient(URI uri, Duration timeout) {
        return new ResilientPlanGenerator(ai(uri, timeout), new FallbackPlanGenerator());
    }

    private static String response(String outputText) throws IOException {
        var root = MAPPER.createObjectNode();
        root.put("status", "completed");
        var message = root.putArray("output").addObject();
        message.put("type", "message");
        var content = message.putArray("content").addObject();
        content.put("type", "output_text");
        content.put("text", outputText);
        return MAPPER.writeValueAsString(root);
    }

    private static String refusalResponse() throws IOException {
        var root = MAPPER.createObjectNode();
        root.put("status", "completed");
        var message = root.putArray("output").addObject();
        message.put("type", "message");
        var content = message.putArray("content").addObject();
        content.put("type", "refusal");
        content.put("refusal", "Не могу выполнить запрос");
        return MAPPER.writeValueAsString(root);
    }

    private TestServer server(ThrowingHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/responses", exchange -> {
            try { handler.handle(exchange); } finally { exchange.close(); }
        });
        server.start();
        return new TestServer(server);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    @FunctionalInterface
    private interface ThrowingHandler { void handle(HttpExchange exchange) throws IOException; }

    private record TestServer(HttpServer server) implements AutoCloseable {
        URI uri() { return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/responses"); }
        @Override public void close() { server.stop(0); }
    }
}
