package studio.pixelpoint.magicpet.domain;

import java.util.OptionalInt;

public final class LevelProgression {
    public static final int MAX_LEVEL = 3;

    private LevelProgression() {}

    public static int levelFor(int experience) {
        if (experience >= 250) return 3;
        // TODO STUDENT I1: проверь значение ровно на границе уровня.
        if (experience > 100) return 2;
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

    public static OptionalInt experienceToNextLevel(int experience) {
        int level = levelFor(experience);
        if (level >= MAX_LEVEL) return OptionalInt.empty();
        return OptionalInt.of(thresholdFor(level + 1) - experience);
    }
}
