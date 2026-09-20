package studio.pixelpoint.magicpet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import studio.pixelpoint.magicpet.application.IncomingMessage;
import studio.pixelpoint.magicpet.application.PetFacade;
import studio.pixelpoint.magicpet.application.UiTexts;
import studio.pixelpoint.magicpet.domain.UserState;
import studio.pixelpoint.magicpet.infrastructure.assets.FileAssetCatalog;
import studio.pixelpoint.magicpet.infrastructure.plan.FallbackPlanGenerator;
import studio.pixelpoint.magicpet.infrastructure.sqlite.SqliteDatabase;
import studio.pixelpoint.magicpet.infrastructure.sqlite.SqliteUserStore;
import studio.pixelpoint.magicpet.lesson.StudentBot;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TaskManagementIntegrationTest {
    @TempDir Path tempDir;

    @Test
    void customTasksSurviveRestartMoveToCompletedAndUnlockLevelThree() {
        String url = SqliteDatabase.migrate(tempDir.resolve("tasks.db"));
        FakeTelegramGateway telegram = new FakeTelegramGateway();
        SqliteUserStore store = new SqliteUserStore(url);
        StudentBot bot = bot(store, telegram);
        onboard(bot, 11, "Руно", "Выучить Java");

        addTask(bot, 11, "Повторить классы", "Прочитать конспект и выписать один пример", "custom-1");
        addTask(bot, 11, "Решить задачу", "Написать небольшую программу", "custom-2");

        SqliteUserStore afterRestart = new SqliteUserStore(url);
        assertEquals(5, afterRestart.findTasks(11, false).size());
        assertTrue(afterRestart.findTasks(11, false).stream().anyMatch(task -> task.title().equals("Решить задачу")));

        StudentBot restartedBot = bot(afterRestart, telegram);
        var active = afterRestart.findTasks(11, false);
        for (int index = 0; index < active.size(); index++) {
            long taskId = active.get(index).id();
            restartedBot.receiveMessage(button(11, "task:done:" + taskId, "complete-" + index));
        }

        var user = afterRestart.getOrCreate(11, "Пользователь 11");
        assertEquals(250, user.experience());
        assertEquals(3, user.level());
        assertEquals(UserState.PLAN_COMPLETED, user.state());
        assertTrue(afterRestart.findTasks(11, false).isEmpty());
        assertEquals(5, afterRestart.findTasks(11, true).size());
        assertTrue(telegram.contains("достиг уровня 3"));
    }

    @Test
    void deletedTaskCannotAwardXpAndForeignUserCannotAccessIt() {
        String url = SqliteDatabase.migrate(tempDir.resolve("ownership.db"));
        SqliteUserStore store = new SqliteUserStore(url);
        FakeTelegramGateway telegram = new FakeTelegramGateway();
        StudentBot bot = bot(store, telegram);
        onboard(bot, 21, "Игнис", "Тренироваться", "scenario:sport");
        onboard(bot, 22, "Скриба", "Вести блог", "scenario:blog");
        addTask(bot, 21, "Личная задача", "Описание личной задачи", "private");
        long taskId = store.findTasks(21, false).stream().filter(task -> task.custom()).findFirst().orElseThrow().id();

        assertTrue(store.findTask(22, taskId).isEmpty());
        assertFalse(store.completeTask(22, taskId, "forged-complete").taskCompleted());
        assertFalse(store.deleteTask(22, taskId));
        assertEquals(0, store.getOrCreate(22, "Второй").experience());

        assertTrue(store.deleteTask(21, taskId));
        assertFalse(store.completeTask(21, taskId, "after-delete").taskCompleted());
        assertTrue(store.findTask(21, taskId).isEmpty());
        assertEquals(0, store.getOrCreate(21, "Первый").experience());
    }

    @Test
    void emptyListsAndTooLongTitleHaveFriendlyResponses() {
        String url = SqliteDatabase.migrate(tempDir.resolve("validation.db"));
        SqliteUserStore store = new SqliteUserStore(url);
        FakeTelegramGateway telegram = new FakeTelegramGateway();
        StudentBot bot = bot(store, telegram);
        onboard(bot, 31, "Руно", "Учиться");

        bot.receiveMessage(button(31, "action:completed_tasks", "empty"));
        assertTrue(telegram.contains("Выполненных задач пока нет"));
        bot.receiveMessage(button(31, "action:add_task", "add"));
        bot.receiveMessage(text(31, "x".repeat(61), "long-title"));

        assertEquals(UserState.WAITING_TASK_TITLE, store.getOrCreate(31, "Пользователь 31").state());
        assertTrue(telegram.contains("от 1 до 60 символов"));
    }

    private StudentBot bot(SqliteUserStore store, FakeTelegramGateway telegram) {
        return new StudentBot(new PetFacade(telegram, store, new FallbackPlanGenerator(),
                new FileAssetCatalog(tempDir.resolve("no-assets")), UiTexts.load()));
    }

    private void onboard(StudentBot bot, long userId, String pet, String goal) {
        onboard(bot, userId, pet, goal, "scenario:study");
    }

    private void onboard(StudentBot bot, long userId, String pet, String goal, String scenario) {
        bot.receiveMessage(text(userId, "/start", userId + "-start"));
        bot.receiveMessage(button(userId, scenario, userId + "-scenario"));
        bot.receiveMessage(text(userId, pet, userId + "-pet"));
        bot.receiveMessage(text(userId, goal, userId + "-goal"));
    }

    private void addTask(StudentBot bot, long userId, String title, String description, String key) {
        bot.receiveMessage(button(userId, "action:add_task", key + "-start"));
        bot.receiveMessage(text(userId, title, key + "-title"));
        bot.receiveMessage(text(userId, description, key + "-description"));
    }

    private IncomingMessage text(long id, String text, String delivery) {
        return new IncomingMessage(id, "Пользователь " + id, text, null, delivery);
    }

    private IncomingMessage button(long id, String button, String delivery) {
        return new IncomingMessage(id, "Пользователь " + id, null, button, delivery);
    }
}
