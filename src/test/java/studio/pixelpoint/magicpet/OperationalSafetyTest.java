package studio.pixelpoint.magicpet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import studio.pixelpoint.magicpet.infrastructure.config.AppConfig;
import studio.pixelpoint.magicpet.infrastructure.config.AppMode;
import studio.pixelpoint.magicpet.infrastructure.logging.SafeLogger;
import studio.pixelpoint.magicpet.infrastructure.retry.RetryExecutor;
import studio.pixelpoint.magicpet.infrastructure.sqlite.SqliteDatabase;
import studio.pixelpoint.magicpet.infrastructure.sqlite.SqliteUserStore;
import studio.pixelpoint.magicpet.domain.UserState;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.*;

class OperationalSafetyTest {
    @TempDir Path tempDir;

    @Test
    void configurationIsValidatedAndResetDefaultsToDisabled() {
        AppConfig production = AppConfig.load(tempDir, Map.of("TELEGRAM_BOT_TOKEN", "secret-token"));
        assertEquals(AppMode.PRODUCTION, production.appMode());
        assertFalse(production.resetEnabled());

        AppConfig lesson = AppConfig.load(tempDir, Map.of(
                "TELEGRAM_BOT_TOKEN", "secret-token",
                "APP_MODE", "lesson",
                "TELEGRAM_RETRY_ATTEMPTS", "4",
                "TELEGRAM_RETRY_DELAY_MS", "10"));
        assertTrue(lesson.resetEnabled());
        assertEquals(4, lesson.telegramRetryAttempts());
    }

    @Test
    void invalidConfigurationDoesNotEchoSecrets() {
        var error = assertThrows(AppConfig.ConfigurationException.class, () -> AppConfig.load(tempDir, Map.of(
                "TELEGRAM_BOT_TOKEN", "super-secret-token",
                "LLM_API_URL", "super-secret-token://bad")));
        assertFalse(error.getMessage().contains("super-secret-token"));
    }

    @Test
    void safeLoggerNeverPrintsExceptionMessageOrStackTrace() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        SafeLogger logger = new SafeLogger(new PrintStream(bytes, true, StandardCharsets.UTF_8));
        logger.error("telegram.update_failed", new RuntimeException("token=top-secret goal=private"));

        String log = bytes.toString(StandardCharsets.UTF_8);
        assertTrue(log.contains("telegram.update_failed"));
        assertTrue(log.contains("RuntimeException"));
        assertFalse(log.contains("top-secret"));
        assertFalse(log.contains("private"));
        assertFalse(log.contains("at studio."));
    }

    @Test
    void retryRecoversAfterTemporaryFailureWithoutSleepingInTest() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        AtomicInteger sleeps = new AtomicInteger();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        SafeLogger logger = new SafeLogger(new PrintStream(bytes, true, StandardCharsets.UTF_8));

        RetryExecutor.run(3, Duration.ofMillis(5), () -> {
            if (attempts.incrementAndGet() < 3) throw new Exception("secret-token");
        }, ignored -> sleeps.incrementAndGet(), logger);

        assertEquals(3, attempts.get());
        assertEquals(2, sleeps.get());
        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("telegram.connection_restored"));
        assertFalse(bytes.toString(StandardCharsets.UTF_8).contains("secret-token"));
    }

    @Test
    void corruptedSqliteFailsBeforeApplicationCanStart() throws Exception {
        Path database = tempDir.resolve("corrupt.db");
        Files.writeString(database, "this is not sqlite");

        var error = assertThrows(SqliteDatabase.DatabaseStartupException.class,
                () -> SqliteDatabase.migrate(database));
        assertTrue(error.getMessage().contains("SQLite недоступна или повреждена"));
        assertFalse(error.getMessage().contains("this is not sqlite"));
    }

    @Test
    void busySqliteFailsWithoutDamageAndWorksAfterLockIsReleased() throws Exception {
        String url = SqliteDatabase.migrate(tempDir.resolve("busy.db"));
        SqliteUserStore store = new SqliteUserStore(url);
        var user = store.getOrCreate(55, "Лена");
        user.begin();

        try (var blocker = DriverManager.getConnection(url)) {
            blocker.createStatement().execute("BEGIN EXCLUSIVE");
            assertThrows(IllegalStateException.class, () -> store.save(user));
            blocker.createStatement().execute("ROLLBACK");
        }

        store.save(user);
        assertEquals(UserState.CHOOSING_SCENARIO, store.getOrCreate(55, "Лена").state());
    }
}
