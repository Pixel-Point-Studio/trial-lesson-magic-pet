package studio.pixelpoint.magicpet;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import studio.pixelpoint.magicpet.application.PetFacade;
import studio.pixelpoint.magicpet.application.PromptResource;
import studio.pixelpoint.magicpet.application.UiTexts;
import studio.pixelpoint.magicpet.application.port.PlanGenerator;
import studio.pixelpoint.magicpet.infrastructure.assets.FileAssetCatalog;
import studio.pixelpoint.magicpet.infrastructure.config.AppConfig;
import studio.pixelpoint.magicpet.infrastructure.plan.FallbackPlanGenerator;
import studio.pixelpoint.magicpet.infrastructure.plan.AiProxyPlanGenerator;
import studio.pixelpoint.magicpet.infrastructure.plan.ResilientPlanGenerator;
import studio.pixelpoint.magicpet.infrastructure.sqlite.SqliteDatabase;
import studio.pixelpoint.magicpet.infrastructure.sqlite.SqliteUserStore;
import studio.pixelpoint.magicpet.infrastructure.telegram.TelegramBotAdapter;
import studio.pixelpoint.magicpet.infrastructure.telegram.TelegramBotGateway;
import studio.pixelpoint.magicpet.lesson.StudentBot;

import java.nio.file.Path;
import java.net.URI;
import java.time.Duration;

public final class MagicPetApplication {
    private MagicPetApplication() {}

    public static void main(String[] args) {
        try {
            AppConfig config = AppConfig.load(Path.of(".").toAbsolutePath().normalize());
            UiTexts texts = UiTexts.load();
            PromptResource.validate();

            var telegramClient = new OkHttpTelegramClient(config.telegramBotToken());
            var gateway = new TelegramBotGateway(telegramClient);
            String jdbcUrl = SqliteDatabase.migrate(config.databasePath());
            var facade = new PetFacade(gateway, new SqliteUserStore(jdbcUrl), planGenerator(config),
                    new FileAssetCatalog(config.assetDirectory()), texts);
            var bot = new TelegramBotAdapter(new StudentBot(facade));

            try (var polling = new TelegramBotsLongPollingApplication()) {
                polling.registerBot(config.telegramBotToken(), bot);
                System.out.println("Magic Pet запущен. Остановить: Ctrl+C");
                Thread.currentThread().join();
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (Exception error) {
            System.err.println("Не удалось запустить Magic Pet: " + error.getMessage());
            System.exit(1);
        }
    }

    private static PlanGenerator planGenerator(AppConfig config) {
        PlanGenerator fallback = new FallbackPlanGenerator();
        if (config.llmApiUrl().isBlank()) return fallback;
        PlanGenerator ai = new AiProxyPlanGenerator(URI.create(config.llmApiUrl()), config.llmApiKey(),
                config.llmModel(), Duration.ofMillis(config.llmTimeoutMillis()));
        return new ResilientPlanGenerator(ai, fallback);
    }
}
