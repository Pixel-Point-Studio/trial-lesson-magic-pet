package studio.pixelpoint.magicpet.application;

import studio.pixelpoint.magicpet.application.port.AssetCatalog;
import studio.pixelpoint.magicpet.application.port.PlanGenerator;
import studio.pixelpoint.magicpet.application.port.TelegramGateway;
import studio.pixelpoint.magicpet.application.port.UserStore;
import studio.pixelpoint.magicpet.domain.*;

import java.util.List;
import java.util.Map;

public final class PetFacade {
    private final TelegramGateway telegram;
    private final UserStore users;
    private final PlanGenerator plans;
    private final AssetCatalog assets;
    private final UiTexts texts;

    public PetFacade(TelegramGateway telegram, UserStore users, PlanGenerator plans, AssetCatalog assets, UiTexts texts) {
        this.telegram = telegram;
        this.users = users;
        this.plans = plans;
        this.assets = assets;
        this.texts = texts;
    }

    public UserSession getOrCreateUser(long userId, String displayName) {
        return users.getOrCreate(userId, displayName);
    }

    public void start(UserSession user) {
        user.begin();
        users.save(user);
        telegram.sendButtons(user.userId(), texts.message("chooseScenario"), List.of(
                new Button("scenario:study", texts.button("scenarioStudy")),
                new Button("scenario:sport", texts.button("scenarioSport")),
                new Button("scenario:blog", texts.button("scenarioBlog"))));
    }

    public void selectScenario(UserSession user, Scenario scenario) {
        user.chooseScenario(scenario);
        users.save(user);
        telegram.sendText(user.userId(), texts.message("askPetName"));
    }

    public boolean namePet(UserSession user, String name) {
        String clean = clean(name);
        if (clean.isBlank() || clean.length() > 32) {
            telegram.sendText(user.userId(), texts.message("invalidName"));
            return false;
        }
        user.namePet(clean);
        users.save(user);
        telegram.sendText(user.userId(), texts.message("askGoal"));
        return true;
    }

    public boolean createPlan(UserSession user, String goalText) {
        String clean = clean(goalText);
        if (clean.isBlank() || clean.length() > 500) {
            telegram.sendText(user.userId(), texts.message("invalidGoal"));
            return false;
        }
        GeneratedPlan plan = plans.generate(user.scenario(), clean);
        user.activatePlan(clean, plan);
        users.save(user);
        if (plan.source() == PlanSource.FALLBACK) {
            telegram.sendText(user.userId(), texts.message("fallbackPlanUsed"));
        }
        telegram.sendText(user.userId(), texts.template("planReady", Map.of("summary", plan.summary())));
        showPet(user);
        showCurrentTask(user);
        return true;
    }

    public void completeCurrentTask(UserSession user, String deliveryId) {
        ProgressResult result = users.completeCurrentTask(user.userId(), deliveryId);
        UserSession refreshed = users.getOrCreate(user.userId(), user.displayName());
        if (!result.taskCompleted()) {
            telegram.sendText(refreshed.userId(), texts.message("alreadyCompleted"));
            return;
        }
        telegram.sendText(refreshed.userId(), texts.template("progress", Map.of(
                "petName", refreshed.petName(), "xpAdded", result.xpAdded(), "totalXp", result.totalXp())));
        if (result.levelUp()) {
            telegram.sendText(refreshed.userId(), texts.template("levelUp", Map.of(
                    "petName", refreshed.petName(), "level", result.level())));
            showPet(refreshed);
        }
        if (result.planCompleted()) {
            telegram.sendButtons(refreshed.userId(), texts.message("planCompleted"), menuButtons());
        } else {
            showCurrentTask(refreshed);
        }
    }

    public void repeatPrompt(UserSession user) {
        switch (user.state()) {
            case NEW -> telegram.sendText(user.userId(), texts.message("sendStart"));
            case CHOOSING_SCENARIO -> start(user);
            case WAITING_PET_NAME -> telegram.sendText(user.userId(), texts.message("askPetName"));
            case WAITING_GOAL -> telegram.sendText(user.userId(), texts.message("askGoal"));
            case PLAN_READY, ACTIVE -> showCurrentTask(user);
            case PLAN_COMPLETED -> telegram.sendButtons(user.userId(), texts.message("planCompleted"), menuButtons());
            case WAITING_TASK_TITLE -> telegram.sendText(user.userId(), texts.message("askTaskTitle"));
            case WAITING_TASK_DESCRIPTION -> telegram.sendText(user.userId(), texts.message("askTaskDescription"));
        }
    }

    public void showCurrentTask(UserSession user) {
        PlanTask task = user.currentTask();
        if (task == null) return;
        String body = texts.template("task", Map.of(
                "number", user.currentTaskIndex() + 1,
                "title", task.title(),
                "description", task.description()));
        telegram.sendButtons(user.userId(), body, List.of(
                new Button("action:task_done", texts.button("taskDone")),
                new Button("action:my_tasks", texts.button("myTasks")),
                new Button("action:add_task", texts.button("addTask")),
                new Button("action:completed_tasks", texts.button("completedTasks"))));
    }

    public void showTasks(UserSession user, boolean completed) {
        List<UserTask> tasks = users.findTasks(user.userId(), completed);
        List<Button> buttons = new java.util.ArrayList<>();
        for (UserTask task : tasks.stream().limit(20).toList()) {
            String label = task.title().length() <= 45 ? task.title() : task.title().substring(0, 42) + "…";
            buttons.add(new Button("task:open:" + task.id(), label));
        }
        if (!completed) buttons.add(new Button("action:add_task", texts.button("addTask")));
        buttons.add(new Button(completed ? "action:my_tasks" : "action:completed_tasks",
                texts.button(completed ? "myTasks" : "completedTasks")));
        String message = tasks.isEmpty()
                ? texts.message(completed ? "noCompletedTasks" : "noActiveTasks")
                : texts.message(completed ? "completedTasks" : "activeTasks");
        telegram.sendButtons(user.userId(), message, buttons);
    }

    public void showTask(UserSession user, long taskId) {
        users.findTask(user.userId(), taskId).ifPresentOrElse(task -> {
            String body = texts.template("taskCard", Map.of(
                    "title", task.title(), "description", task.description(),
                    "status", task.completed() ? "выполнено" : "активно"));
            List<Button> buttons = new java.util.ArrayList<>();
            if (!task.completed()) buttons.add(new Button("task:done:" + task.id(), texts.button("taskDone")));
            buttons.add(new Button("task:delete:" + task.id(), texts.button("deleteTask")));
            buttons.add(new Button(task.completed() ? "action:completed_tasks" : "action:my_tasks",
                    texts.button("back")));
            telegram.sendButtons(user.userId(), body, buttons);
        }, () -> telegram.sendText(user.userId(), texts.message("taskNotFound")));
    }

    public void beginTaskCreation(UserSession user) {
        if (user.state() != UserState.ACTIVE && user.state() != UserState.PLAN_COMPLETED) {
            repeatPrompt(user);
            return;
        }
        user.beginTaskCreation();
        users.save(user);
        telegram.sendText(user.userId(), texts.message("askTaskTitle"));
    }

    public void acceptTaskTitle(UserSession user, String title) {
        String clean = clean(title);
        if (clean.isBlank() || clean.length() > 60) {
            telegram.sendText(user.userId(), texts.message("invalidTaskTitle"));
            return;
        }
        user.setDraftTaskTitle(clean);
        users.save(user);
        telegram.sendText(user.userId(), texts.message("askTaskDescription"));
    }

    public void acceptTaskDescription(UserSession user, String description) {
        String clean = clean(description);
        if (clean.isBlank() || clean.length() > 500) {
            telegram.sendText(user.userId(), texts.message("invalidTaskDescription"));
            return;
        }
        users.addCustomTask(user.userId(), user.draftTaskTitle(), clean);
        user.finishTaskCreation();
        users.save(user);
        telegram.sendText(user.userId(), texts.message("taskAdded"));
        showTasks(user, false);
    }

    public void completeTask(UserSession user, long taskId, String deliveryId) {
        ProgressResult result = users.completeTask(user.userId(), taskId, deliveryId);
        UserSession refreshed = users.getOrCreate(user.userId(), user.displayName());
        if (!result.taskCompleted()) {
            telegram.sendText(user.userId(), texts.message("taskNotFound"));
            return;
        }
        telegram.sendText(refreshed.userId(), texts.template("progress", Map.of(
                "petName", refreshed.petName(), "xpAdded", result.xpAdded(), "totalXp", result.totalXp())));
        if (result.levelUp()) {
            telegram.sendText(refreshed.userId(), texts.template("levelUp", Map.of(
                    "petName", refreshed.petName(), "level", result.level())));
            showPet(refreshed);
        }
        showTasks(refreshed, false);
    }

    public void deleteTask(UserSession user, long taskId) {
        if (users.deleteTask(user.userId(), taskId)) {
            telegram.sendText(user.userId(), texts.message("taskDeleted"));
            showTasks(users.getOrCreate(user.userId(), user.displayName()), false);
        } else {
            telegram.sendText(user.userId(), texts.message("taskNotFound"));
        }
    }

    public void showPet(UserSession user) {
        String caption = texts.template("petText", Map.of(
                "petName", user.petName(), "level", user.level(), "xp", user.experience()));
        assets.find(user.scenario(), user.level())
                .ifPresentOrElse(path -> telegram.sendPhoto(user.userId(), path, caption),
                        () -> telegram.sendText(user.userId(), caption));
    }

    private static String clean(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", " ");
    }

    private List<Button> menuButtons() {
        return List.of(
                new Button("action:my_tasks", texts.button("myTasks")),
                new Button("action:add_task", texts.button("addTask")),
                new Button("action:completed_tasks", texts.button("completedTasks")));
    }
}
