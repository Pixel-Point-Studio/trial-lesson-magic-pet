package studio.pixelpoint.magicpet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import studio.pixelpoint.magicpet.application.*;
import studio.pixelpoint.magicpet.application.port.AssetCatalog;
import studio.pixelpoint.magicpet.application.port.UserStore;
import studio.pixelpoint.magicpet.domain.*;
import studio.pixelpoint.magicpet.infrastructure.memory.InMemoryUserStore;
import studio.pixelpoint.magicpet.infrastructure.plan.FallbackPlanGenerator;
import studio.pixelpoint.magicpet.infrastructure.sqlite.SqliteDatabase;
import studio.pixelpoint.magicpet.infrastructure.sqlite.SqliteUserStore;
import studio.pixelpoint.magicpet.lesson.StudentBot;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LessonRouteContractTest {
    @TempDir Path tempDir;

    @Test
    void task1RemembersEnteredNameForEveryPet() {
        int index = 0;
        for (String route : List.of("study", "sport", "blog")) {
            long userId = 100 + index;
            String enteredName = "Имя " + route;
            SqliteUserStore users = new SqliteUserStore(
                    SqliteDatabase.migrate(tempDir.resolve("name-" + route + ".db")));
            Harness h = harness(users);
            startAndSelect(h, userId, route);

            h.bot.receiveMessage(text(userId, enteredName, "name-" + index));

            assertEquals(enteredName, users.getOrCreate(userId, "Ученик").petName());
            index++;
        }
    }

    @Test
    void task4RenamesEveryPetAndKeepsMenuBelowCurrentAction() {
        int index = 0;
        for (String route : List.of("study", "sport", "blog")) {
            long userId = 200 + index;
            Harness h = active(memoryHarness(), userId, route);
            List<String> initialButtons = h.telegram.sent().getLast().buttons().stream().map(Button::id).toList();

            assertEquals("action:task_done", initialButtons.getFirst());
            assertTrue(initialButtons.indexOf("action:rename_pet") > 0);
            assertTrue(initialButtons.indexOf("action:rename_pet") < initialButtons.indexOf("action:my_tasks"));

            h.bot.receiveMessage(button(userId, "action:rename_pet", "rename-" + index));
            assertEquals(UserState.WAITING_PET_RENAME, h.users.getOrCreate(userId, "Ученик").state());
            h.bot.receiveMessage(text(userId, "Новое имя " + index, "new-name-" + index));
            assertEquals("Новое имя " + index, h.users.getOrCreate(userId, "Ученик").petName());
            index++;
        }
    }

    @Test
    void task2ConfirmsDeletionForEveryPet() {
        int index = 0;
        for (String route : List.of("study", "sport", "blog")) {
            long userId = 300 + index;
            Harness h = active(memoryHarness(), userId, route);
            long taskId = h.users.addCustomTask(userId, "Удалить меня", "Описание");

            h.bot.receiveMessage(button(userId, "task:confirm_delete:" + taskId, "ask-" + index));

            assertTrue(h.users.findTask(userId, taskId).isPresent());
            List<String> confirmationButtons = h.telegram.sent().getLast().buttons().stream().map(Button::id).toList();
            assertEquals("task:delete:" + taskId, confirmationButtons.get(0));
            assertEquals("task:open:" + taskId, confirmationButtons.get(1));

            h.bot.receiveMessage(button(userId, "task:open:" + taskId, "no-" + index));
            assertTrue(h.users.findTask(userId, taskId).isPresent());
            h.bot.receiveMessage(button(userId, "task:confirm_delete:" + taskId, "ask-again-" + index));
            h.bot.receiveMessage(button(userId, "task:delete:" + taskId, "yes-" + index));
            assertTrue(h.users.findTask(userId, taskId).isEmpty());
            assertEquals(0, h.users.getOrCreate(userId, "Ученик").experience());
            index++;
        }
    }

    @Test
    void task3ShowsEveryPetAndKeepsButtonInCommonMenu() {
        int index = 0;
        for (String route : List.of("study", "sport", "blog")) {
            long userId = 400 + index;
            Harness h = active(memoryHarness(), userId, route);
            List<String> initialButtons = h.telegram.sent().getLast().buttons().stream().map(Button::id).toList();

            assertTrue(initialButtons.indexOf("action:show_pet") > 0);
            assertTrue(initialButtons.indexOf("action:show_pet") < initialButtons.indexOf("action:my_tasks"));

            h.bot.receiveMessage(button(userId, "action:show_pet", "show-" + index));
            assertEquals(Path.of(route, "level-1.png"), h.telegram.sent().getLast().path());
            assertEquals(0, h.users.getOrCreate(userId, "Ученик").experience());
            index++;
        }
    }

    @Test
    void levelBoundariesRemainCorrect() {
        assertEquals(1, LevelProgression.levelFor(99));
        assertEquals(2, LevelProgression.levelFor(100));
        assertEquals(2, LevelProgression.levelFor(249));
        assertEquals(3, LevelProgression.levelFor(250));
    }

    @Test
    void activeAndCompletedTaskListsRemainSeparate() {
        Harness h = active(memoryHarness(), 500, "blog");
        long active = h.users.addCustomTask(500, "Активная", "Описание");
        long done = h.users.addCustomTask(500, "Готовая", "Описание");
        h.users.completeTask(500, done, "complete");

        h.bot.receiveMessage(button(500, "action:my_tasks", "active-list"));
        var activeIds = h.telegram.sent().getLast().buttons().stream().map(Button::id).toList();
        assertTrue(activeIds.contains("task:open:" + active));
        assertFalse(activeIds.contains("task:open:" + done));

        h.bot.receiveMessage(button(500, "action:completed_tasks", "done-list"));
        var doneIds = h.telegram.sent().getLast().buttons().stream().map(Button::id).toList();
        assertFalse(doneIds.contains("task:open:" + active));
        assertTrue(doneIds.contains("task:open:" + done));
    }

    private Harness active(Harness h, long id, String route) {
        startAndSelect(h, id, route);
        h.bot.receiveMessage(text(id, "Искорка", "name"));
        h.bot.receiveMessage(text(id, "Моя цель", "goal"));
        return h;
    }

    private void startAndSelect(Harness h, long id, String route) {
        h.bot.receiveMessage(text(id, "/start", "start"));
        h.bot.receiveMessage(button(id, "scenario:" + route, "scenario"));
    }

    private Harness memoryHarness() { return harness(new InMemoryUserStore()); }

    private Harness harness(UserStore users) {
        FakeTelegramGateway telegram = new FakeTelegramGateway();
        AssetCatalog assets = (scenario, level) -> Optional.of(
                Path.of(scenario.name().toLowerCase(), "level-" + level + ".png"));
        PetFacade facade = new PetFacade(telegram, users, new FallbackPlanGenerator(), assets, UiTexts.load());
        return new Harness(new StudentBot(facade), users, telegram);
    }

    private IncomingMessage text(long id, String value, String delivery) {
        return new IncomingMessage(id, "Ученик", value, null, delivery);
    }

    private IncomingMessage button(long id, String value, String delivery) {
        return new IncomingMessage(id, "Ученик", null, value, delivery);
    }

    private record Harness(StudentBot bot, UserStore users, FakeTelegramGateway telegram) {}
}
