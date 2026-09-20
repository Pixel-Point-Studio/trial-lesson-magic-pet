package studio.pixelpoint.magicpet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import studio.pixelpoint.magicpet.application.IncomingMessage;
import studio.pixelpoint.magicpet.application.PetFacade;
import studio.pixelpoint.magicpet.application.UiTexts;
import studio.pixelpoint.magicpet.application.port.AssetCatalog;
import studio.pixelpoint.magicpet.domain.UserState;
import studio.pixelpoint.magicpet.infrastructure.plan.FallbackPlanGenerator;
import studio.pixelpoint.magicpet.infrastructure.sqlite.SqliteDatabase;
import studio.pixelpoint.magicpet.infrastructure.sqlite.SqliteUserStore;
import studio.pixelpoint.magicpet.lesson.StudentBot;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SqliteUserStoreIntegrationTest {
    @TempDir Path tempDir;

    @Test
    void migrationCreatesAllProductTables() throws Exception {
        String url = SqliteDatabase.migrate(tempDir.resolve("migration.db"));
        try (var connection = DriverManager.getConnection(url);
             var statement = connection.prepareStatement("""
                     SELECT name FROM sqlite_master
                     WHERE type = 'table' AND name IN ('users', 'pets', 'goals', 'tasks', 'flyway_schema_history')
                     """);
             var result = statement.executeQuery()) {
            int count = 0;
            while (result.next()) count++;
            assertEquals(5, count);
        }
    }

    @Test
    void onboardingAndActivePlanSurviveRepositoryRestart() {
        Path database = tempDir.resolve("restart.db");
        String url = SqliteDatabase.migrate(database);
        FakeTelegramGateway telegram = new FakeTelegramGateway();
        StudentBot firstBot = bot(new SqliteUserStore(url), telegram);

        firstBot.receiveMessage(text(10, "/start", "1"));
        firstBot.receiveMessage(button(10, "scenario:blog", "2"));

        SqliteUserStore afterOnboardingRestart = new SqliteUserStore(url);
        assertEquals(UserState.WAITING_PET_NAME, afterOnboardingRestart.getOrCreate(10, "Лена").state());
        StudentBot secondBot = bot(afterOnboardingRestart, telegram);
        secondBot.receiveMessage(text(10, "Лис", "3"));
        secondBot.receiveMessage(text(10, "Начать вести блог", "4"));
        secondBot.receiveMessage(button(10, "action:task_done", "5"));

        // Telegram may redeliver the same callback after the Java process restarts.
        StudentBot afterCallbackRestart = bot(new SqliteUserStore(url), telegram);
        afterCallbackRestart.receiveMessage(button(10, "action:task_done", "5"));

        SqliteUserStore afterPlanRestart = new SqliteUserStore(url);
        var restored = afterPlanRestart.getOrCreate(10, "Лена");
        assertEquals(UserState.ACTIVE, restored.state());
        assertEquals(50, restored.experience());
        assertEquals(1, restored.currentTaskIndex());
        assertEquals(3, restored.tasks().size());
    }

    @Test
    void twoTelegramUsersRemainIsolated() {
        String url = SqliteDatabase.migrate(tempDir.resolve("isolation.db"));
        SqliteUserStore store = new SqliteUserStore(url);
        StudentBot bot = bot(store, new FakeTelegramGateway());

        completeOnboarding(bot, 101, "Искра", "Сдать экзамен", "scenario:study");
        completeOnboarding(bot, 202, "Фокс", "Создать блог", "scenario:blog");
        bot.receiveMessage(button(101, "action:task_done", "u1-done"));

        var first = store.getOrCreate(101, "Первый");
        var second = store.getOrCreate(202, "Второй");
        assertEquals(50, first.experience());
        assertEquals(1, first.currentTaskIndex());
        assertEquals(0, second.experience());
        assertEquals(0, second.currentTaskIndex());
        assertEquals("Фокс", second.petName());
    }

    private StudentBot bot(SqliteUserStore store, FakeTelegramGateway telegram) {
        AssetCatalog noAssets = (scenario, level) -> Optional.empty();
        return new StudentBot(new PetFacade(telegram, store, new FallbackPlanGenerator(), noAssets, UiTexts.load()));
    }

    private void completeOnboarding(StudentBot bot, long userId, String pet, String goal, String scenario) {
        bot.receiveMessage(text(userId, "/start", userId + "-start"));
        bot.receiveMessage(button(userId, scenario, userId + "-scenario"));
        bot.receiveMessage(text(userId, pet, userId + "-pet"));
        bot.receiveMessage(text(userId, goal, userId + "-goal"));
    }

    private IncomingMessage text(long id, String text, String delivery) {
        return new IncomingMessage(id, "Пользователь " + id, text, null, delivery);
    }

    private IncomingMessage button(long id, String button, String delivery) {
        return new IncomingMessage(id, "Пользователь " + id, null, button, delivery);
    }
}
