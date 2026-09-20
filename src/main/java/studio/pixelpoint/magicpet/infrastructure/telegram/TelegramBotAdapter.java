package studio.pixelpoint.magicpet.infrastructure.telegram;

import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import studio.pixelpoint.magicpet.lesson.StudentBot;

public final class TelegramBotAdapter implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramUpdateAdapter adapter = new TelegramUpdateAdapter();
    private final StudentBot bot;

    public TelegramBotAdapter(StudentBot bot) {
        this.bot = bot;
    }

    @Override
    public void consume(Update update) {
        adapter.normalize(update).ifPresent(bot::receiveMessage);
    }
}
