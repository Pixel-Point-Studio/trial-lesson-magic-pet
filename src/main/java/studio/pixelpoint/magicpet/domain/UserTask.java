package studio.pixelpoint.magicpet.domain;

public record UserTask(
        long id,
        String title,
        String description,
        boolean completed,
        boolean custom,
        int position
) {}
