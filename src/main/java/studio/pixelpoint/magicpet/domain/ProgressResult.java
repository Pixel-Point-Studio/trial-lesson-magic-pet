package studio.pixelpoint.magicpet.domain;

public record ProgressResult(
        boolean taskCompleted,
        int xpAdded,
        int totalXp,
        int level,
        boolean levelUp,
        boolean planCompleted
) {}
