package studio.pixelpoint.magicpet.infrastructure.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.URI;

public record AppConfig(
        String telegramBotToken,
        Path assetDirectory,
        Path databasePath,
        String llmApiUrl,
        String llmApiKey,
        String llmModel,
        int llmTimeoutMillis,
        AppMode appMode,
        int telegramRetryAttempts,
        int telegramRetryDelayMillis
) {
    public static AppConfig load(Path workingDirectory) {
        return load(workingDirectory, System.getenv());
    }

    public static AppConfig load(Path workingDirectory, Map<String, String> environment) {
        Map<String, String> values = new HashMap<>();
        Path envFile = workingDirectory.resolve(".env");
        if (Files.isRegularFile(envFile)) {
            try {
                parse(Files.readAllLines(envFile), values);
            } catch (IOException error) {
                throw new IllegalStateException("Не удалось прочитать .env", error);
            }
        }
        environment.forEach(values::put);
        String token = values.getOrDefault("TELEGRAM_BOT_TOKEN", "").trim();
        if (token.isEmpty() || token.equals("replace_me")) {
            throw new ConfigurationException(
                    "Не задан TELEGRAM_BOT_TOKEN. Скопируйте .env.example в .env и вставьте токен от @BotFather.");
        }
        Path assets = workingDirectory.resolve(values.getOrDefault("ASSET_DIRECTORY", "./assets")).normalize();
        Path database = workingDirectory.resolve(values.getOrDefault("DATABASE_PATH", "./data/magic-pet.db")).normalize();
        int timeout = positiveInt(values.getOrDefault("LLM_TIMEOUT_MS", "5000"), "LLM_TIMEOUT_MS");
        String apiUrl = values.getOrDefault("LLM_API_URL", "").trim();
        validateApiUrl(apiUrl);
        String apiKey = values.getOrDefault("LLM_API_KEY", "").trim();
        String model = values.getOrDefault("LLM_MODEL", "").trim();
        if (!apiUrl.isBlank() && apiKey.isBlank()) {
            throw new ConfigurationException("Для прямого OpenAI API заполните LLM_API_KEY");
        }
        if (!apiUrl.isBlank() && model.isBlank()) {
            throw new ConfigurationException("Для прямого OpenAI API заполните LLM_MODEL");
        }
        AppMode appMode = AppMode.parse(values.getOrDefault("APP_MODE", "production"));
        int retryAttempts = boundedInt(values.getOrDefault("TELEGRAM_RETRY_ATTEMPTS", "3"),
                "TELEGRAM_RETRY_ATTEMPTS", 1, 10);
        int retryDelay = boundedInt(values.getOrDefault("TELEGRAM_RETRY_DELAY_MS", "2000"),
                "TELEGRAM_RETRY_DELAY_MS", 0, 30_000);
        return new AppConfig(token, assets, database,
                apiUrl,
                apiKey, model, timeout, appMode, retryAttempts, retryDelay);
    }

    private static void parse(List<String> lines, Map<String, String> target) {
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int separator = line.indexOf('=');
            if (separator <= 0) continue;
            String value = line.substring(separator + 1).trim();
            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            target.put(line.substring(0, separator).trim(), value);
        }
    }

    private static int positiveInt(String raw, String name) {
        try {
            int value = Integer.parseInt(raw.trim());
            if (value <= 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException error) {
            throw new ConfigurationException(name + " должен быть положительным целым числом");
        }
    }

    private static int boundedInt(String raw, String name, int min, int max) {
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < min || value > max) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException error) {
            throw new ConfigurationException(name + " должен быть целым числом от " + min + " до " + max);
        }
    }

    private static void validateApiUrl(String value) {
        if (value.isBlank()) return;
        try {
            String scheme = URI.create(value).getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) throw new RuntimeException();
        } catch (RuntimeException error) {
            throw new ConfigurationException("LLM_API_URL должен быть корректным http/https URL");
        }
    }

    public boolean resetEnabled() { return appMode == AppMode.LESSON; }

    public static final class ConfigurationException extends IllegalStateException {
        public ConfigurationException(String message) { super(message); }
    }
}
