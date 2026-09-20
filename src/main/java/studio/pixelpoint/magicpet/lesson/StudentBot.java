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

        if (message.hasText() && user.state() == UserState.WAITING_PET_NAME) {
            pet.namePet(user, message.text());
            return;
        }
        if (message.hasText() && user.state() == UserState.WAITING_GOAL) {
            pet.createPlan(user, message.text());
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
}
