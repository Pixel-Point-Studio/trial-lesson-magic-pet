package studio.pixelpoint.magicpet.lesson;

import studio.pixelpoint.magicpet.application.IncomingMessage;
import studio.pixelpoint.magicpet.application.PetFacade;
import studio.pixelpoint.magicpet.domain.Scenario;
import studio.pixelpoint.magicpet.domain.UserSession;
import studio.pixelpoint.magicpet.domain.UserState;

/** Простая учебная поверхность: ввод, условия и вызовы методов уровня продукта. */
public final class StudentBot {
    private final PetFacade pet;

    public StudentBot(PetFacade pet) {
        this.pet = pet;
    }

    public void receiveMessage(IncomingMessage message) {
        UserSession user = pet.getOrCreateUser(message.userId(), message.displayName());

        if (message.isCommand("/start")) {
            pet.start(user);
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
            }
            return;
        }

        if (message.buttonPressed("action:my_tasks")) {
            pet.showTasks(user, false);
            return;
        }
        if (message.buttonPressed("action:completed_tasks")) {
            pet.showTasks(user, true);
            return;
        }
        if (message.buttonPressed("action:add_task")) {
            pet.beginTaskCreation(user);
            return;
        }

        Long openedTaskId = callbackId(message.buttonId(), "task:open:");
        if (openedTaskId != null) {
            pet.showTask(user, openedTaskId);
            return;
        }
        Long completedTaskId = callbackId(message.buttonId(), "task:done:");
        if (completedTaskId != null) {
            pet.completeTask(user, completedTaskId, message.deliveryId());
            return;
        }
        Long deletedTaskId = callbackId(message.buttonId(), "task:delete:");
        if (deletedTaskId != null) {
            pet.deleteTask(user, deletedTaskId);
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
}
