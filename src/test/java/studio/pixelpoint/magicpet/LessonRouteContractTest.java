package studio.pixelpoint.magicpet;

import org.junit.jupiter.api.DisplayName;
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
import studio.pixelpoint.magicpet.lesson.route.StudyLesson;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LessonRouteContractTest {
    @TempDir Path tempDir;

    @Test void studyWrongPet() {
        Harness h = memoryHarness();
        startAndSelect(h, 1, "study");
        assertEquals(Scenario.STUDY, h.users.getOrCreate(1, "Ученик").scenario());
        h.bot.receiveMessage(text(1, "Руни", "name"));
        h.bot.receiveMessage(text(1, "Подготовиться к экзамену", "goal"));
        assertTrue(h.telegram.sent().stream().anyMatch(item ->
                Path.of("study", "level-1.png").equals(item.path())));
    }

    @Test void studySavesCustomName() {
        SqliteUserStore users = new SqliteUserStore(SqliteDatabase.migrate(tempDir.resolve("name.db")));
        Harness h = harness(users);
        UserSession user = users.getOrCreate(2, "Ученик");
        user.begin();
        user.chooseScenario(Scenario.STUDY);
        users.save(user);
        new StudyLesson().acceptPetName(h.facade, user, "Искорка");
        assertEquals("Искорка", users.getOrCreate(2, "Ученик").petName());
    }

    @Test void studyShowsPet() {
        Harness h = active(memoryHarness(), 3, "study");
        int before = h.users.getOrCreate(3, "Ученик").experience();
        h.bot.receiveMessage(button(3, "action:show_pet", "show"));
        assertEquals(Path.of("study", "level-1.png"), h.telegram.sent().getLast().path());
        assertTrue(h.telegram.sent().getLast().text().contains("Руни"));
        assertEquals(before, h.users.getOrCreate(3, "Ученик").experience());
    }

    @Test void studyRenamesPet() {
        SqliteUserStore users = new SqliteUserStore(SqliteDatabase.migrate(tempDir.resolve("rename.db")));
        Harness h = active(harness(users), 4, "study");
        h.bot.receiveMessage(button(4, "action:rename_pet", "rename"));
        h.bot.receiveMessage(text(4, "", "empty"));
        assertEquals(UserState.WAITING_PET_RENAME, users.getOrCreate(4, "Ученик").state());
        h.bot.receiveMessage(text(4, "x".repeat(33), "long"));
        assertEquals(UserState.WAITING_PET_RENAME, users.getOrCreate(4, "Ученик").state());
        h.bot.receiveMessage(text(4, "Искорка", "new-name"));
        assertEquals("Искорка", users.getOrCreate(4, "Ученик").petName());
        assertEquals(UserState.ACTIVE, users.getOrCreate(4, "Ученик").state());
    }

    @Test void sportLevelsAtOneHundred() {
        assertEquals(1, LevelProgression.levelFor(99));
        assertEquals(2, LevelProgression.levelFor(100));
        assertEquals(2, LevelProgression.levelFor(249));
        assertEquals(3, LevelProgression.levelFor(250));
    }

    @Test void sportShowsCurrentLevelImage() {
        Harness h = active(memoryHarness(), 5, "sport");
        for (int index = 0; index < 4; index++) {
            long task = h.users.addCustomTask(5, "Разминка " + index, "Описание");
            h.users.completeTask(5, task, "seed-" + index);
        }
        h.bot.receiveMessage(button(5, "action:task_done", "done-1"));
        assertTrue(h.telegram.sent().stream().anyMatch(item ->
                Path.of("sport", "level-3.png").equals(item.path())));
    }

    @Test
    @DisplayName("I3: Спорт просит подтверждение перед удалением")
    void sportConfirmsDeletion() {
        Harness h = active(memoryHarness(), 6, "sport");
        long task = h.users.addCustomTask(6, "Тренировка", "Описание");
        h.bot.receiveMessage(button(6, "task:confirm_delete:" + task, "ask"));
        assertTrue(h.users.findTask(6, task).isPresent());
        assertEquals(2, h.telegram.sent().getLast().buttons().size());
        h.bot.receiveMessage(button(6, "task:open:" + task, "no"));
        assertTrue(h.users.findTask(6, task).isPresent());
        h.bot.receiveMessage(button(6, "task:delete:" + task, "yes"));
        assertTrue(h.users.findTask(6, task).isEmpty());
        assertEquals(0, h.users.getOrCreate(6, "Ученик").experience());
    }

    @Test void sportShowsLevelProgress() {
        Harness h = active(memoryHarness(), 7, "sport");
        h.bot.receiveMessage(button(7, "action:task_done", "done"));
        h.bot.receiveMessage(button(7, "action:level_progress", "progress"));
        assertTrue(h.telegram.sent().getLast().text().contains("50 XP"));
        h.bot.receiveMessage(button(7, "action:task_done", "done-2"));
        h.bot.receiveMessage(button(7, "action:level_progress", "progress-2"));
        assertTrue(h.telegram.sent().getLast().text().contains("150 XP"));
    }

    @Test void blogShowsActiveTasks() {
        Harness h = active(memoryHarness(), 8, "blog");
        long active = h.users.addCustomTask(8, "Активная", "Описание");
        long done = h.users.addCustomTask(8, "Готовая", "Описание");
        h.users.completeTask(8, done, "complete");
        h.bot.receiveMessage(button(8, "action:my_tasks", "list"));
        var ids = h.telegram.sent().getLast().buttons().stream().map(Button::id).toList();
        assertTrue(ids.contains("task:open:" + active));
        assertFalse(ids.contains("task:open:" + done));
    }

    @Test void blogDeletesWithoutXp() {
        Harness h = active(memoryHarness(), 9, "blog");
        long task = h.users.addCustomTask(9, "Удалить", "Описание");
        h.bot.receiveMessage(button(9, "task:delete:" + task, "delete"));
        assertTrue(h.users.findTask(9, task).isEmpty());
        assertEquals(0, h.users.getOrCreate(9, "Ученик").experience());
    }

    @Test
    @DisplayName("B3: Блог показывает подсказку к текущей задаче")
    void blogShowsHint() {
        Harness h = active(memoryHarness(), 10, "blog");
        int before = h.users.getOrCreate(10, "Ученик").experience();
        int taskBefore = h.users.getOrCreate(10, "Ученик").currentTaskIndex();
        h.bot.receiveMessage(button(10, "action:hint", "hint"));
        assertTrue(h.telegram.sent().getLast().text().startsWith("💡"));
        assertEquals(before, h.users.getOrCreate(10, "Ученик").experience());
        assertEquals(taskBefore, h.users.getOrCreate(10, "Ученик").currentTaskIndex());
    }

    @Test void genericDeleteConfirmationWorksOutsideSport() {
        Harness h = active(memoryHarness(), 12, "study");
        long task = h.users.addCustomTask(12, "Конспект", "Описание");
        h.bot.receiveMessage(button(12, "task:confirm_delete:" + task, "ask"));
        assertTrue(h.users.findTask(12, task).isPresent());
        assertEquals(2, h.telegram.sent().getLast().buttons().size());
        h.bot.receiveMessage(button(12, "task:delete:" + task, "yes"));
        assertTrue(h.users.findTask(12, task).isEmpty());
        assertEquals(0, h.users.getOrCreate(12, "Ученик").experience());
    }

    @Test void blogShowsFullPlan() {
        Harness h = active(memoryHarness(), 11, "blog");
        h.bot.receiveMessage(button(11, "action:show_plan", "plan-before"));
        assertTrue(h.telegram.sent().getLast().text().contains("👉 1."));
        h.bot.receiveMessage(button(11, "action:task_done", "done"));
        int before = h.users.getOrCreate(11, "Ученик").experience();
        h.bot.receiveMessage(button(11, "action:show_plan", "plan"));
        String plan = h.telegram.sent().getLast().text();
        assertTrue(plan.contains("✅ 1."));
        assertTrue(plan.contains("👉 2."));
        assertTrue(plan.contains("⏳ 3."));
        assertEquals(before, h.users.getOrCreate(11, "Ученик").experience());
    }

    private Harness active(Harness h, long id, String route) {
        startAndSelect(h, id, route);
        h.bot.receiveMessage(text(id, switch (route) { case "study" -> "Руни"; case "sport" -> "Игнис"; default -> "Скриба"; }, "name"));
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
        return new Harness(new StudentBot(facade), users, telegram, facade);
    }

    private IncomingMessage text(long id, String value, String delivery) {
        return new IncomingMessage(id, "Ученик", value, null, delivery);
    }

    private IncomingMessage button(long id, String value, String delivery) {
        return new IncomingMessage(id, "Ученик", null, value, delivery);
    }

    private record Harness(StudentBot bot, UserStore users, FakeTelegramGateway telegram, PetFacade facade) {}
}
