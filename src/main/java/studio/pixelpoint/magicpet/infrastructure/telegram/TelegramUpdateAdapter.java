package studio.pixelpoint.magicpet.infrastructure.telegram;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import studio.pixelpoint.magicpet.application.IncomingMessage;

import java.util.Optional;

public final class TelegramUpdateAdapter {
    public Optional<IncomingMessage> normalize(Update update) {
        if (update.hasCallbackQuery()) {
            var callback = update.getCallbackQuery();
            User from = callback.getFrom();
            return Optional.of(new IncomingMessage(
                    from.getId(), displayName(from), null, callback.getData(), callback.getId()));
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            User from = update.getMessage().getFrom();
            return Optional.of(new IncomingMessage(
                    from.getId(), displayName(from), update.getMessage().getText(), null,
                    "update:" + update.getUpdateId()));
        }
        return Optional.empty();
    }

    private String displayName(User user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName();
        String last = user.getLastName() == null ? "" : " " + user.getLastName();
        String result = (first + last).trim();
        return result.isEmpty() ? "Путешественник" : result;
    }
}
