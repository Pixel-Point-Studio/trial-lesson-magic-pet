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
        telegram.sendText(user.userId(), texts.message("fallbackPlanUsed"));
        telegram.sendText(user.userId(), texts.template("planReady", Map.of("summary", plan.summary())));
        showPet(user);
        showCurrentTask(user);
        return true;
    }

    public void completeCurrentTask(UserSession user) {
        ProgressResult result = user.completeCurrentTask();
        users.save(user);
        if (!result.taskCompleted()) {
            telegram.sendText(user.userId(), texts.message("alreadyCompleted"));
            return;
        }
        telegram.sendText(user.userId(), texts.template("progress", Map.of(
                "petName", user.petName(), "xpAdded", result.xpAdded(), "totalXp", result.totalXp())));
        if (result.levelUp()) {
            telegram.sendText(user.userId(), texts.template("levelUp", Map.of(
                    "petName", user.petName(), "level", result.level())));
            showPet(user);
        }
        if (result.planCompleted()) {
            telegram.sendText(user.userId(), texts.message("planCompleted"));
        } else {
            showCurrentTask(user);
        }
    }

    public void repeatPrompt(UserSession user) {
        switch (user.state()) {
            case NEW -> telegram.sendText(user.userId(), texts.message("sendStart"));
            case CHOOSING_SCENARIO -> start(user);
            case WAITING_PET_NAME -> telegram.sendText(user.userId(), texts.message("askPetName"));
            case WAITING_GOAL -> telegram.sendText(user.userId(), texts.message("askGoal"));
            case PLAN_READY, ACTIVE -> showCurrentTask(user);
            case PLAN_COMPLETED -> telegram.sendText(user.userId(), texts.message("planCompleted"));
        }
    }

    public void showCurrentTask(UserSession user) {
        PlanTask task = user.currentTask();
        if (task == null) return;
        String body = texts.template("task", Map.of(
                "number", user.currentTaskIndex() + 1,
                "title", task.title(),
                "description", task.description()));
        telegram.sendButtons(user.userId(), body,
                List.of(new Button("action:task_done", texts.button("taskDone"))));
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
}
