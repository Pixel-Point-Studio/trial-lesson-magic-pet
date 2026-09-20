package studio.pixelpoint.magicpet.infrastructure.retry;

import studio.pixelpoint.magicpet.infrastructure.logging.SafeLogger;

import java.time.Duration;

public final class RetryExecutor {
    private RetryExecutor() {}

    public static void run(int maxAttempts, Duration delay, CheckedAction action, Sleeper sleeper, SafeLogger logger)
            throws Exception {
        Exception lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                action.run();
                if (attempt > 1) logger.info("telegram.connection_restored");
                return;
            } catch (Exception error) {
                lastError = error;
                logger.warn("telegram.connection_failed", attempt, maxAttempts);
                if (attempt < maxAttempts) sleeper.sleep(delay);
            }
        }
        throw lastError;
    }

    @FunctionalInterface public interface CheckedAction { void run() throws Exception; }
    @FunctionalInterface public interface Sleeper { void sleep(Duration duration) throws InterruptedException; }

    public static void sleep(Duration duration) throws InterruptedException {
        Thread.sleep(duration.toMillis());
    }
}
