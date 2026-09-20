package studio.pixelpoint.magicpet.application;

public record IncomingMessage(
        long userId,
        String displayName,
        String text,
        String buttonId,
        String deliveryId
) {
    public IncomingMessage(long userId, String displayName, String text, String buttonId) {
        this(userId, displayName, text, buttonId, null);
    }

    public boolean isCommand(String command) {
        if (text == null) return false;
        String firstPart = text.trim().split("\\s+", 2)[0];
        return firstPart.equals(command) || firstPart.startsWith(command + "@");
    }

    public boolean buttonPressed(String id) {
        return id != null && id.equals(buttonId);
    }

    public boolean hasText() {
        return text != null && !text.isBlank();
    }
}
