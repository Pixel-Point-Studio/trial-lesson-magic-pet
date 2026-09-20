package studio.pixelpoint.magicpet;

import org.junit.jupiter.api.Test;
import studio.pixelpoint.magicpet.application.IncomingMessage;
import studio.pixelpoint.magicpet.application.PetFacade;
import studio.pixelpoint.magicpet.application.UiTexts;
import studio.pixelpoint.magicpet.application.port.AssetCatalog;
import studio.pixelpoint.magicpet.domain.Scenario;
import studio.pixelpoint.magicpet.domain.UserState;
import studio.pixelpoint.magicpet.infrastructure.memory.InMemoryUserStore;
import studio.pixelpoint.magicpet.infrastructure.plan.FallbackPlanGenerator;
import studio.pixelpoint.magicpet.lesson.StudentBot;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LessonRouteContractTest {
    @Test
    void studyRouteSelectsStudyScenario() {
        Harness harness = harness();
        harness.bot.receiveMessage(text(1, "/start", "start"));
        harness.bot.receiveMessage(button(1, "scenario:study", "study"));

        var user = harness.users.getOrCreate(1, "Ученик");
        assertEquals(Scenario.STUDY, user.scenario());
        assertEquals(UserState.WAITING_PET_NAME, user.state());
    }

    @Test
    void sportRouteRecognizesExactCallback() {
        Harness harness = harness();
        harness.bot.receiveMessage(text(2, "/start", "start"));
        harness.bot.receiveMessage(button(2, "scenario:sport", "sport"));

        var user = harness.users.getOrCreate(2, "Ученик");
        assertEquals(Scenario.SPORT, user.scenario());
        assertEquals(UserState.WAITING_PET_NAME, user.state());
    }

    @Test
    void blogRouteDeletesTaskWithoutAwardingXp() {
        Harness harness = harness();
        long userId = 3;
        harness.bot.receiveMessage(text(userId, "/start", "start"));
        harness.bot.receiveMessage(button(userId, "scenario:blog", "blog"));
        harness.bot.receiveMessage(text(userId, "Скриба", "name"));
        harness.bot.receiveMessage(text(userId, "Запустить блог", "goal"));
        harness.bot.receiveMessage(button(userId, "action:add_task", "add"));
        harness.bot.receiveMessage(text(userId, "Черновик", "title"));
        harness.bot.receiveMessage(text(userId, "Написать два предложения", "description"));
        long taskId = harness.users.findTasks(userId, false).stream()
                .filter(task -> task.custom()).findFirst().orElseThrow().id();

        harness.bot.receiveMessage(button(userId, "task:delete:" + taskId, "delete"));

        assertTrue(harness.users.findTask(userId, taskId).isEmpty());
        assertEquals(0, harness.users.getOrCreate(userId, "Ученик").experience());
    }

    private Harness harness() {
        InMemoryUserStore users = new InMemoryUserStore();
        FakeTelegramGateway telegram = new FakeTelegramGateway();
        AssetCatalog noAssets = (scenario, level) -> Optional.empty();
        PetFacade facade = new PetFacade(telegram, users, new FallbackPlanGenerator(), noAssets, UiTexts.load());
        return new Harness(new StudentBot(facade), users);
    }

    private IncomingMessage text(long id, String value, String delivery) {
        return new IncomingMessage(id, "Ученик", value, null, delivery);
    }

    private IncomingMessage button(long id, String value, String delivery) {
        return new IncomingMessage(id, "Ученик", null, value, delivery);
    }

    private record Harness(StudentBot bot, InMemoryUserStore users) {}
}
