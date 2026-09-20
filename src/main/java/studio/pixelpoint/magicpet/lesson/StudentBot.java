package studio.pixelpoint.magicpet.lesson;

import studio.pixelpoint.magicpet.application.IncomingMessage;
import studio.pixelpoint.magicpet.application.PetFacade;
import studio.pixelpoint.magicpet.domain.Scenario;
import studio.pixelpoint.magicpet.domain.UserSession;
import studio.pixelpoint.magicpet.domain.UserState;

/** Простая учебная поверхность: ввод, условия и вызовы методов уровня продукта. */
public final class StudentBot {
    private final PetFacade pet;
    private final boolean resetEnabled;

    public StudentBot(PetFacade pet) {
        this(pet, false);
    }

    public StudentBot(PetFacade pet, boolean resetEnabled) {
        this.pet = pet;
        this.resetEnabled = resetEnabled;
    }

    public void receiveMessage(IncomingMessage message) {
        UserSession user = pet.getOrCreateUser(message.userId(), message.displayName());

        if (message.isCommand("/start")) {
            if (user.state() == UserState.NEW) pet.start(user);
            else pet.repeatPrompt(user);
            return;
        }

        if (message.isCommand("/reset")) {
            if (resetEnabled) pet.start(user);
            else pet.repeatPrompt(user);
            return;
        }

        if (message.buttonPressed("scenario:study")) {
            chooseScenario(user, Scenario.STUDY);
            return;
        }
        if (message.buttonPressed("scenario:sport")) {
            chooseScenario(user, Scenario.SPORT);
            return;
        }
        if (message.buttonPressed("scenario:blog")) {
            chooseScenario(user, Scenario.BLOG);
            return;
        }

        if (message.buttonPressed("action:task_done")) {
            if (user.state() == UserState.ACTIVE) {
                pet.completeCurrentTask(user, message.deliveryId());
            } else {
                pet.repeatPrompt(user);
            }
            return;
        }

        if (message.buttonPressed("action:my_tasks")) {
            if (journeyReady(user)) pet.showTasks(user, false);
            else pet.repeatPrompt(user);
            return;
        }
        if (message.buttonPressed("action:completed_tasks")) {
            if (journeyReady(user)) pet.showTasks(user, true);
            else pet.repeatPrompt(user);
            return;
        }
        if (message.buttonPressed("action:add_task")) {
            pet.beginTaskCreation(user);
            return;
        }

        Long openedTaskId = callbackId(message.buttonId(), "task:open:");
        if (openedTaskId != null) {
            if (journeyReady(user)) pet.showTask(user, openedTaskId);
            else pet.repeatPrompt(user);
            return;
        }
        Long completedTaskId = callbackId(message.buttonId(), "task:done:");
        if (completedTaskId != null) {
            if (journeyReady(user)) pet.completeTask(user, completedTaskId, message.deliveryId());
            else pet.repeatPrompt(user);
            return;
        }
        Long deletedTaskId = callbackId(message.buttonId(), "task:delete:");
        if (deletedTaskId != null) {
            if (journeyReady(user)) pet.deleteTask(user, deletedTaskId);
            else pet.repeatPrompt(user);
            return;
        }

        if (message.hasText() && user.state() == UserState.WAITING_PET_NAME) {
            pet.namePet(user, message.text());
            return;
        }
        if (message.hasText() && user.state() == UserState.WAITING_GOAL) {
            pet.createPlan(user, message.text());
            return;
        }
        if (message.hasText() && user.state() == UserState.WAITING_TASK_TITLE) {
            pet.acceptTaskTitle(user, message.text());
            return;
        }
        if (message.hasText() && user.state() == UserState.WAITING_TASK_DESCRIPTION) {
            pet.acceptTaskDescription(user, message.text());
            return;
        }

        pet.repeatPrompt(user);
    }

    private void chooseScenario(UserSession user, Scenario scenario) {
        if (user.state() == UserState.CHOOSING_SCENARIO) {
            pet.selectScenario(user, scenario);
        } else {
            pet.repeatPrompt(user);
        }
    }

    private Long callbackId(String buttonId, String prefix) {
        if (buttonId == null || !buttonId.startsWith(prefix)) return null;
        try {
            return Long.parseLong(buttonId.substring(prefix.length()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean journeyReady(UserSession user) {
        return user.state() == UserState.ACTIVE || user.state() == UserState.PLAN_COMPLETED;
    }
}
