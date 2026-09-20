package studio.pixelpoint.magicpet.infrastructure.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record AppConfig(String telegramBotToken, Path assetDirectory) {
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
        return new AppConfig(token, assets);
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
}
