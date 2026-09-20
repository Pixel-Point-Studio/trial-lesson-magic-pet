package studio.pixelpoint.magicpet;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import studio.pixelpoint.magicpet.application.Button;
import studio.pixelpoint.magicpet.application.PetFacade;
import studio.pixelpoint.magicpet.application.UiTexts;
import studio.pixelpoint.magicpet.application.port.AssetCatalog;
import studio.pixelpoint.magicpet.application.port.TelegramGateway;
import studio.pixelpoint.magicpet.domain.UserState;
import studio.pixelpoint.magicpet.infrastructure.logging.SafeLogger;
import studio.pixelpoint.magicpet.infrastructure.memory.InMemoryUserStore;
import studio.pixelpoint.magicpet.infrastructure.plan.FallbackPlanGenerator;
import studio.pixelpoint.magicpet.infrastructure.telegram.TelegramBotAdapter;
import studio.pixelpoint.magicpet.lesson.StudentBot;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ResilientTelegramFlowTest {
    @Test
    void oneTelegramFailureDoesNotBreakFollowingUpdatesOrLeakDetails() {
        InMemoryUserStore users = new InMemoryUserStore();
        FlakyGateway gateway = new FlakyGateway();
        AssetCatalog noAssets = (scenario, level) -> Optional.empty();
        PetFacade facade = new PetFacade(gateway, users, new FallbackPlanGenerator(), noAssets, UiTexts.load());
        ByteArrayOutputStream logs = new ByteArrayOutputStream();
        TelegramBotAdapter adapter = new TelegramBotAdapter(new StudentBot(facade),
                new SafeLogger(new PrintStream(logs, true, StandardCharsets.UTF_8)));

        adapter.consume(update(1, "/start"));
        assertEquals(UserState.CHOOSING_SCENARIO, users.getOrCreate(77, "Лена").state());
        adapter.consume(update(2, "неожиданный текст"));

        assertFalse(gateway.sent.isEmpty());
        assertTrue(gateway.sent.getLast().contains("Выбери путь"));
        assertTrue(logs.toString(StandardCharsets.UTF_8).contains("telegram.update_failed"));
        assertFalse(logs.toString(StandardCharsets.UTF_8).contains("sensitive-telegram-detail"));
    }

    private Update update(int id, String text) {
        User user = User.builder().id(77L).firstName("Лена").isBot(false).build();
        Chat chat = Chat.builder().id(77L).type("private").build();
        Message message = Message.builder().messageId(id).from(user).chat(chat).text(text).build();
        Update update = new Update();
        update.setUpdateId(id);
        update.setMessage(message);
        return update;
    }

    private static final class FlakyGateway implements TelegramGateway {
        private boolean fail = true;
        private final List<String> sent = new ArrayList<>();

        @Override public void sendText(long userId, String text) { send(text); }
        @Override public void sendButtons(long userId, String text, List<Button> buttons) { send(text); }
        @Override public void sendPhoto(long userId, Path path, String caption) { send(caption); }

        private void send(String value) {
            if (fail) {
                fail = false;
                throw new IllegalStateException("sensitive-telegram-detail");
            }
            sent.add(value);
        }
    }
}
