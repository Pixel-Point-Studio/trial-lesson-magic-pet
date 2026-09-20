package studio.pixelpoint.magicpet;

import org.junit.jupiter.api.Test;
import studio.pixelpoint.magicpet.application.IncomingMessage;
import studio.pixelpoint.magicpet.application.PetFacade;
import studio.pixelpoint.magicpet.application.UiTexts;
import studio.pixelpoint.magicpet.application.port.AssetCatalog;
import studio.pixelpoint.magicpet.domain.UserState;
import studio.pixelpoint.magicpet.infrastructure.memory.InMemoryUserStore;
import studio.pixelpoint.magicpet.infrastructure.plan.FallbackPlanGenerator;
import studio.pixelpoint.magicpet.lesson.StudentBot;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SafeResetAndInputTest {
    @Test
    void startAndDisabledResetPreserveExistingProgressButLessonResetClearsIt() {
        InMemoryUserStore users = new InMemoryUserStore();
        FakeTelegramGateway telegram = new FakeTelegramGateway();
        StudentBot production = bot(users, telegram, false);
        onboard(production, 1);
        production.receiveMessage(button(1, "action:task_done", "done"));
        assertEquals(50, users.getOrCreate(1, "Аня").experience());

        production.receiveMessage(text(1, "/start", "start-again"));
        production.receiveMessage(text(1, "/reset", "disabled-reset"));
        assertEquals(50, users.getOrCreate(1, "Аня").experience());
        assertEquals(UserState.ACTIVE, users.getOrCreate(1, "Аня").state());

        StudentBot lesson = bot(users, telegram, true);
        lesson.receiveMessage(text(1, "/reset", "lesson-reset"));
        assertEquals(0, users.getOrCreate(1, "Аня").experience());
        assertEquals(UserState.CHOOSING_SCENARIO, users.getOrCreate(1, "Аня").state());
    }

    @Test
    void invalidAndUnexpectedInputKeepsCurrentState() {
        InMemoryUserStore users = new InMemoryUserStore();
        FakeTelegramGateway telegram = new FakeTelegramGateway();
        StudentBot bot = bot(users, telegram, false);
        bot.receiveMessage(text(2, "/start", "start"));
        bot.receiveMessage(button(2, "scenario:study", "scenario"));
        bot.receiveMessage(text(2, "x".repeat(33), "long-name"));
        assertEquals(UserState.WAITING_PET_NAME, users.getOrCreate(2, "Аня").state());

        bot.receiveMessage(text(2, "Руно", "name"));
        bot.receiveMessage(text(2, "x".repeat(501), "long-goal"));
        assertEquals(UserState.WAITING_GOAL, users.getOrCreate(2, "Аня").state());

        bot.receiveMessage(text(2, "Выучить Java", "goal"));
        bot.receiveMessage(text(2, "совсем неожиданный текст", "unexpected"));
        assertEquals(UserState.ACTIVE, users.getOrCreate(2, "Аня").state());
        assertTrue(telegram.contains("Задание 1"));
    }

    private StudentBot bot(InMemoryUserStore users, FakeTelegramGateway telegram, boolean reset) {
        AssetCatalog noAssets = (scenario, level) -> Optional.empty();
        return new StudentBot(new PetFacade(telegram, users, new FallbackPlanGenerator(), noAssets, UiTexts.load()), reset);
    }

    private void onboard(StudentBot bot, long id) {
        bot.receiveMessage(text(id, "/start", "start"));
        bot.receiveMessage(button(id, "scenario:study", "scenario"));
        bot.receiveMessage(text(id, "Руно", "name"));
        bot.receiveMessage(text(id, "Выучить Java", "goal"));
    }

    private IncomingMessage text(long id, String text, String delivery) {
        return new IncomingMessage(id, "Аня", text, null, delivery);
    }

    private IncomingMessage button(long id, String button, String delivery) {
        return new IncomingMessage(id, "Аня", null, button, delivery);
    }
}
