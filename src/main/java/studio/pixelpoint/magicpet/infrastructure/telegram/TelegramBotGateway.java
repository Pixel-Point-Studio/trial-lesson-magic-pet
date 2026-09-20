package studio.pixelpoint.magicpet.infrastructure.telegram;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import studio.pixelpoint.magicpet.application.Button;
import studio.pixelpoint.magicpet.application.port.TelegramGateway;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class TelegramBotGateway implements TelegramGateway {
    private final TelegramClient client;

    public TelegramBotGateway(TelegramClient client) {
        this.client = client;
    }

    @Override
    public void sendText(long userId, String text) {
        execute(SendMessage.builder().chatId(userId).text(text).build());
    }

    @Override
    public void sendButtons(long userId, String text, List<Button> buttons) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (Button button : buttons) {
            rows.add(new InlineKeyboardRow(InlineKeyboardButton.builder()
                    .text(button.label()).callbackData(button.id()).build()));
        }
        execute(SendMessage.builder()
                .chatId(userId)
                .text(text)
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(rows).build())
                .build());
    }

    @Override
    public void sendPhoto(long userId, Path path, String caption) {
        try {
            client.execute(SendPhoto.builder()
                    .chatId(userId)
                    .photo(new InputFile(path.toFile()))
                    .caption(caption)
                    .build());
        } catch (TelegramApiException error) {
            throw new TelegramGatewayException("Telegram не принял изображение", error);
        }
    }

    private void execute(SendMessage message) {
        try {
            client.execute(message);
        } catch (TelegramApiException error) {
            throw new TelegramGatewayException("Telegram не принял сообщение", error);
        }
    }

    public static final class TelegramGatewayException extends RuntimeException {
        TelegramGatewayException(String message, Throwable cause) { super(message, cause); }
    }
}
