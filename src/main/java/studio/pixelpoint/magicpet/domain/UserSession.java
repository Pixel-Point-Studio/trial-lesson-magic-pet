package studio.pixelpoint.magicpet.domain;

import java.util.ArrayList;
import java.util.List;

public final class UserSession {
    private final long userId;
    private String displayName;
    private UserState state = UserState.NEW;
    private Scenario scenario;
    private String petName;
    private String goal;
    private String planSummary;
    private List<PlanTask> tasks = new ArrayList<>();
    private int currentTaskIndex;
    private int experience;
    private String draftTaskTitle;
    private UserState taskCreationReturnState;

    public UserSession(long userId, String displayName) {
        this.userId = userId;
        this.displayName = displayName == null ? "" : displayName;
    }

    public static UserSession restore(
            long userId,
            String displayName,
            UserState state,
            Scenario scenario,
            String petName,
            String goal,
            List<PlanTask> tasks,
            int experience,
            String draftTaskTitle,
            UserState taskCreationReturnState
    ) {
        UserSession session = new UserSession(userId, displayName);
        session.state = state;
        session.scenario = scenario;
        session.petName = petName;
        session.goal = goal;
        session.planSummary = goal;
        session.tasks = new ArrayList<>(tasks);
        session.currentTaskIndex = 0;
        while (session.currentTaskIndex < session.tasks.size()
                && session.tasks.get(session.currentTaskIndex).completed()) {
            session.currentTaskIndex++;
        }
        session.experience = experience;
        session.draftTaskTitle = draftTaskTitle;
        session.taskCreationReturnState = taskCreationReturnState;
        return session;
    }

    public long userId() { return userId; }
    public String displayName() { return displayName; }
    public UserState state() { return state; }
    public Scenario scenario() { return scenario; }
    public String petName() { return petName; }
    public String goal() { return goal; }
    public String planSummary() { return planSummary; }
    public List<PlanTask> tasks() { return List.copyOf(tasks); }
    public int currentTaskIndex() { return currentTaskIndex; }
    public int experience() { return experience; }
    public int level() { return LevelProgression.levelFor(experience); }
    public String draftTaskTitle() { return draftTaskTitle; }
    public UserState taskCreationReturnState() { return taskCreationReturnState; }

    public void updateDisplayName(String value) {
        if (value != null && !value.isBlank()) displayName = value;
    }

    public void begin() {
        state = UserState.CHOOSING_SCENARIO;
        scenario = null;
        petName = null;
        goal = null;
        planSummary = null;
        tasks = new ArrayList<>();
        currentTaskIndex = 0;
        experience = 0;
        draftTaskTitle = null;
        taskCreationReturnState = null;
    }

    public void chooseScenario(Scenario value) {
        scenario = value;
        state = UserState.WAITING_PET_NAME;
    }

    public void namePet(String value) {
        petName = value;
        state = UserState.WAITING_GOAL;
    }

    public void activatePlan(String goalText, GeneratedPlan plan) {
        goal = goalText;
        planSummary = plan.summary();
        tasks = new ArrayList<>(plan.tasks());
        currentTaskIndex = 0;
        state = UserState.PLAN_READY;
        state = UserState.ACTIVE;
    }

    public PlanTask currentTask() {
        return currentTaskIndex < tasks.size() ? tasks.get(currentTaskIndex) : null;
    }

    public ProgressResult completeCurrentTask() {
        int oldLevel = level();
        PlanTask task = currentTask();
        if (task == null || !task.complete()) {
            return new ProgressResult(false, 0, experience, level(), false, state == UserState.PLAN_COMPLETED);
        }
        experience += PlanTask.XP_REWARD;
        currentTaskIndex++;
        if (currentTaskIndex >= tasks.size()) state = UserState.PLAN_COMPLETED;
        return new ProgressResult(true, PlanTask.XP_REWARD, experience, level(), level() > oldLevel,
                state == UserState.PLAN_COMPLETED);
    }

    public ProgressResult awardTaskXp() {
        int oldLevel = level();
        experience += PlanTask.XP_REWARD;
        return new ProgressResult(true, PlanTask.XP_REWARD, experience, level(), level() > oldLevel,
                state == UserState.PLAN_COMPLETED);
    }

    public void beginTaskCreation() {
        if (state != UserState.ACTIVE && state != UserState.PLAN_COMPLETED) {
            throw new IllegalStateException("Сначала завершите создание питомца");
        }
        taskCreationReturnState = state;
        draftTaskTitle = null;
        state = UserState.WAITING_TASK_TITLE;
    }

    public void setDraftTaskTitle(String title) {
        if (state != UserState.WAITING_TASK_TITLE) throw new IllegalStateException("Название сейчас не ожидается");
        draftTaskTitle = title;
        state = UserState.WAITING_TASK_DESCRIPTION;
    }

    public void finishTaskCreation() {
        state = taskCreationReturnState == null ? UserState.PLAN_COMPLETED : taskCreationReturnState;
        draftTaskTitle = null;
        taskCreationReturnState = null;
    }
}
