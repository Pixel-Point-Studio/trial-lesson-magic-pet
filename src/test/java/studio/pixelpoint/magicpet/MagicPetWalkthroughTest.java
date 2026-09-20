package studio.pixelpoint.magicpet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import studio.pixelpoint.magicpet.application.IncomingMessage;
import studio.pixelpoint.magicpet.application.PetFacade;
import studio.pixelpoint.magicpet.application.UiTexts;
import studio.pixelpoint.magicpet.application.port.AssetCatalog;
import studio.pixelpoint.magicpet.domain.UserSession;
import studio.pixelpoint.magicpet.domain.UserState;
import studio.pixelpoint.magicpet.infrastructure.plan.FallbackPlanGenerator;
import studio.pixelpoint.magicpet.infrastructure.sqlite.SqliteDatabase;
import studio.pixelpoint.magicpet.infrastructure.sqlite.SqliteUserStore;
import studio.pixelpoint.magicpet.lesson.StudentBot;

import java.util.Optional;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MagicPetWalkthroughTest {
    @TempDir Path tempDir;

    @Test
    void fakeUserCompletesTwoTasksAndReachesLevelTwoExactlyOnce() {
        FakeTelegramGateway telegram = new FakeTelegramGateway();
        SqliteUserStore users = new SqliteUserStore(SqliteDatabase.migrate(tempDir.resolve("walkthrough.db")));
        AssetCatalog noAssets = (scenario, level) -> Optional.empty();
        PetFacade facade = new PetFacade(telegram, users, new FallbackPlanGenerator(), noAssets, UiTexts.load());
        StudentBot bot = new StudentBot(facade);
        long userId = 42L;

        bot.receiveMessage(text(userId, "/start", "start"));
        assertEquals(3, telegram.sent().getLast().buttons().size());
        bot.receiveMessage(button(userId, "scenario:study", "scenario"));
        bot.receiveMessage(text(userId, "Руна", "name"));
        bot.receiveMessage(text(userId, "Подготовиться к экзамену", "goal"));

        UserSession session = users.getOrCreate(userId, "Аня");
        assertEquals(UserState.ACTIVE, session.state());
        assertEquals(3, session.tasks().size());

        bot.receiveMessage(button(userId, "action:task_done", "done-1"));
        assertEquals(50, users.getOrCreate(userId, "Аня").experience());
        bot.receiveMessage(button(userId, "action:task_done", "done-1"));
        assertEquals(50, users.getOrCreate(userId, "Аня").experience(),
                "повторная доставка callback не должна начислять XP");
        bot.receiveMessage(button(userId, "action:task_done", "done-2"));

        session = users.getOrCreate(userId, "Аня");
        assertEquals(100, session.experience());
        assertEquals(2, session.level());
        assertEquals(2, session.currentTaskIndex());
        assertTrue(telegram.contains("достиг уровня 2"));
    }

    private IncomingMessage text(long id, String text, String delivery) {
        return new IncomingMessage(id, "Аня", text, null, delivery);
    }

    private IncomingMessage button(long id, String button, String delivery) {
        return new IncomingMessage(id, "Аня", null, button, delivery);
    }
}
