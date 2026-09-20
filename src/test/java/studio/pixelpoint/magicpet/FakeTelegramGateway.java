package studio.pixelpoint.magicpet;

import studio.pixelpoint.magicpet.application.Button;
import studio.pixelpoint.magicpet.application.port.TelegramGateway;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class FakeTelegramGateway implements TelegramGateway {
    record Outgoing(String kind, long userId, String text, List<Button> buttons, Path path) {}
    private final List<Outgoing> sent = new ArrayList<>();

    @Override public void sendText(long userId, String text) {
        sent.add(new Outgoing("text", userId, text, List.of(), null));
    }
    @Override public void sendButtons(long userId, String text, List<Button> buttons) {
        sent.add(new Outgoing("buttons", userId, text, List.copyOf(buttons), null));
    }
    @Override public void sendPhoto(long userId, Path path, String caption) {
        sent.add(new Outgoing("photo", userId, caption, List.of(), path));
    }

    List<Outgoing> sent() { return List.copyOf(sent); }
    boolean contains(String fragment) { return sent.stream().anyMatch(item -> item.text().contains(fragment)); }
}
