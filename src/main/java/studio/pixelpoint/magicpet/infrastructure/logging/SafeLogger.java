package studio.pixelpoint.magicpet.infrastructure.logging;

import java.io.PrintStream;
import java.time.Instant;
import java.util.Objects;

/** Logs only controlled event codes; exception messages may contain secrets and are never printed. */
public final class SafeLogger {
    private final PrintStream output;

    public SafeLogger(PrintStream output) {
        this.output = Objects.requireNonNull(output);
    }

    public void info(String event) {
        output.println(Instant.now() + " INFO " + safeEvent(event));
    }

    public void warn(String event, int attempt, int maxAttempts) {
        output.println(Instant.now() + " WARN " + safeEvent(event) + " attempt=" + attempt + "/" + maxAttempts);
    }

    public void error(String event, Throwable error) {
        String type = error == null ? "Unknown" : error.getClass().getSimpleName();
        output.println(Instant.now() + " ERROR " + safeEvent(event) + " type=" + type);
    }

    private String safeEvent(String event) {
        return event == null ? "unknown" : event.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }
}
