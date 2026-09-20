package studio.pixelpoint.magicpet.domain;

import java.util.Objects;

public final class PlanTask {
    public static final int XP_REWARD = 50;

    private final String title;
    private final String description;
    private final PlanSource source;
    private boolean completed;

    public PlanTask(String title, String description) {
        this(title, description, PlanSource.FALLBACK, false);
    }

    public PlanTask(String title, String description, boolean completed) {
        this(title, description, PlanSource.FALLBACK, completed);
    }

    public PlanTask(String title, String description, PlanSource source) {
        this(title, description, source, false);
    }

    public PlanTask(String title, String description, PlanSource source, boolean completed) {
        this.title = Objects.requireNonNull(title);
        this.description = Objects.requireNonNull(description);
        this.source = Objects.requireNonNull(source);
        this.completed = completed;
    }

    public String title() { return title; }
    public String description() { return description; }
    public PlanSource source() { return source; }
    public boolean completed() { return completed; }

    public boolean complete() {
        if (completed) return false;
        completed = true;
        return true;
    }
}
