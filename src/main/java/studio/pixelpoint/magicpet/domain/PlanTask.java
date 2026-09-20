package studio.pixelpoint.magicpet.domain;

import java.util.Objects;

public final class PlanTask {
    public static final int XP_REWARD = 50;

    private final String title;
    private final String description;
    private boolean completed;

    public PlanTask(String title, String description) {
        this(title, description, false);
    }

    public PlanTask(String title, String description, boolean completed) {
        this.title = Objects.requireNonNull(title);
        this.description = Objects.requireNonNull(description);
        this.completed = completed;
    }

    public String title() { return title; }
    public String description() { return description; }
    public boolean completed() { return completed; }

    public boolean complete() {
        if (completed) return false;
        completed = true;
        return true;
    }
}
