package studio.pixelpoint.magicpet;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import studio.pixelpoint.magicpet.application.port.PlanGenerator;
import studio.pixelpoint.magicpet.domain.PlanSource;
import studio.pixelpoint.magicpet.domain.Scenario;
import studio.pixelpoint.magicpet.infrastructure.plan.AiProxyPlanGenerator;
import studio.pixelpoint.magicpet.infrastructure.plan.FallbackPlanGenerator;
import studio.pixelpoint.magicpet.infrastructure.plan.ResilientPlanGenerator;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AiProxyPlanGeneratorTest {
    private static final String VALID = """
            {"summary":"Персональный маршрут","tasks":[
              {"title":"Шаг 1","description":"Сделай первое короткое действие"},
              {"title":"Шаг 2","description":"Закрепи результат за две минуты"},
              {"title":"Шаг 3","description":"Запланируй продолжение"}
            ]}
            """;

    @Test
    void validResponseProducesAiPlanAndRequestContainsNoIdentityOrToken() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        try (TestServer server = server(exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, VALID);
        })) {
            var generator = ai(server.uri(), Duration.ofSeconds(1));
            var plan = generator.generate(Scenario.STUDY, "выучить Java");

            assertEquals(PlanSource.AI, plan.source());
            assertEquals(3, plan.tasks().size());
            assertEquals("Bearer test-secret", authorization.get());
            assertTrue(body.get().contains("выучить Java"));
            assertFalse(body.get().contains("test-secret"));
            assertFalse(body.get().contains("telegram"));
            assertFalse(body.get().contains("username"));
        }
    }

    @Test
    void timeoutFallsBack() throws Exception {
        try (TestServer server = server(exchange -> {
            try { Thread.sleep(250); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            try { respond(exchange, 200, VALID); } catch (IOException ignored) { }
        })) {
            assertFallback(resilient(server.uri(), Duration.ofMillis(30)), Scenario.STUDY);
        }
    }

    @Test
    void httpErrorFallsBack() throws Exception {
        try (TestServer server = server(exchange -> respond(exchange, 503, "unavailable"))) {
            assertFallback(resilient(server.uri(), Duration.ofSeconds(1)), Scenario.BLOG);
        }
    }

    @Test
    void brokenJsonFallsBack() throws Exception {
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
    void fourTasksFallBack() throws Exception {
        assertPayloadFallsBack("""
                {"summary":"x","tasks":[
                  {"title":"1","description":"a"},{"title":"2","description":"b"},
                  {"title":"3","description":"c"},{"title":"4","description":"d"}
                ]}
                """, Scenario.STUDY);
    }

    @Test
    void unsafeSportAdviceFallsBack() throws Exception {
        String unsafe = VALID.replace("Сделай первое короткое действие", "Тренируйся через боль и до отказа");
        assertPayloadFallsBack(unsafe, Scenario.SPORT);
    }

    @Test
    void unexpectedResponseFieldFallsBack() throws Exception {
        assertPayloadFallsBack(VALID.replace("{\"summary\"", "{\"userId\":42,\"summary\""), Scenario.STUDY);
    }

    private void assertPayloadFallsBack(String payload, Scenario scenario) throws Exception {
        try (TestServer server = server(exchange -> respond(exchange, 200, payload))) {
            assertFallback(resilient(server.uri(), Duration.ofSeconds(1)), scenario);
        }
    }

    private void assertFallback(PlanGenerator generator, Scenario scenario) {
        var plan = generator.generate(scenario, "цель");
        assertEquals(PlanSource.FALLBACK, plan.source());
        assertEquals(3, plan.tasks().size());
    }

    private AiProxyPlanGenerator ai(URI uri, Duration timeout) {
        return new AiProxyPlanGenerator(uri, "test-secret", "test-model", timeout);
    }

    private PlanGenerator resilient(URI uri, Duration timeout) {
        return new ResilientPlanGenerator(ai(uri, timeout), new FallbackPlanGenerator());
    }

    private TestServer server(ThrowingHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/plan", exchange -> {
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
        URI uri() { return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/plan"); }
        @Override public void close() { server.stop(0); }
    }
}
