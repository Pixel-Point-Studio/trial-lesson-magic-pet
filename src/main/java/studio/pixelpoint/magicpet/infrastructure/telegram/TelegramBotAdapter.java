package studio.pixelpoint.magicpet.infrastructure.telegram;

import org.telegram.telegrambots.longpolling.util.DefaultLongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import studio.pixelpoint.magicpet.lesson.StudentBot;
import studio.pixelpoint.magicpet.infrastructure.logging.SafeLogger;

public final class TelegramBotAdapter extends DefaultLongPollingUpdateConsumer {
    private final TelegramUpdateAdapter adapter = new TelegramUpdateAdapter();
    private final StudentBot bot;
    private final SafeLogger logger;

    public TelegramBotAdapter(StudentBot bot) {
        this(bot, new SafeLogger(System.err));
    }

    public TelegramBotAdapter(StudentBot bot, SafeLogger logger) {
        this.bot = bot;
        this.logger = logger;
    }

    @Override
    public void consume(Update update) {
        try {
            adapter.normalize(update).ifPresent(bot::receiveMessage);
        } catch (RuntimeException error) {
            logger.error("telegram.update_failed", error);
        }
    }
}
