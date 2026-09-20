package studio.pixelpoint.magicpet.infrastructure.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record AppConfig(
        String telegramBotToken,
        Path assetDirectory,
        Path databasePath,
        String llmApiUrl,
        String llmApiKey,
        String llmModel,
        int llmTimeoutMillis
) {
    public static AppConfig load(Path workingDirectory) {
        Map<String, String> values = new HashMap<>();
        Path envFile = workingDirectory.resolve(".env");
        if (Files.isRegularFile(envFile)) {
            try {
                parse(Files.readAllLines(envFile), values);
            } catch (IOException error) {
                throw new IllegalStateException("Не удалось прочитать .env", error);
            }
        }
        System.getenv().forEach(values::put);
        String token = values.getOrDefault("TELEGRAM_BOT_TOKEN", "").trim();
        if (token.isEmpty() || token.equals("replace_me")) {
            throw new IllegalStateException(
                    "Не задан TELEGRAM_BOT_TOKEN. Скопируйте .env.example в .env и вставьте токен от @BotFather.");
        }
        Path assets = workingDirectory.resolve(values.getOrDefault("ASSET_DIRECTORY", "./assets")).normalize();
        Path database = workingDirectory.resolve(values.getOrDefault("DATABASE_PATH", "./data/magic-pet.db")).normalize();
        int timeout = positiveInt(values.getOrDefault("LLM_TIMEOUT_MS", "5000"), "LLM_TIMEOUT_MS");
        return new AppConfig(token, assets, database,
                values.getOrDefault("LLM_API_URL", "").trim(),
                values.getOrDefault("LLM_API_KEY", "").trim(),
                values.getOrDefault("LLM_MODEL", "").trim(), timeout);
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
            throw new IllegalStateException(name + " должен быть положительным целым числом");
        }
    }
}
