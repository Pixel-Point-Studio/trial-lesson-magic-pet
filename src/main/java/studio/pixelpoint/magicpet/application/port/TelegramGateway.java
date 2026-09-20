package studio.pixelpoint.magicpet.application.port;

import studio.pixelpoint.magicpet.application.Button;

import java.nio.file.Path;
import java.util.List;

public interface TelegramGateway {
    void sendText(long userId, String text);
    void sendButtons(long userId, String text, List<Button> buttons);
    void sendPhoto(long userId, Path path, String caption);
}
