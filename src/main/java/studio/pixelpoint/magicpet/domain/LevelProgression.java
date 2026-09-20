package studio.pixelpoint.magicpet.domain;

public final class LevelProgression {
    public static final int MAX_LEVEL = 3;

    private LevelProgression() {}

    public static int levelFor(int experience) {
        if (experience >= 250) return 3;
        if (experience >= 100) return 2;
        return 1;
    }

    public static int thresholdFor(int level) {
        return switch (level) {
            case 1 -> 0;
            case 2 -> 100;
            case 3 -> 250;
            default -> throw new IllegalArgumentException("Поддерживаются уровни 1–3");
        };
    }
}
