package studio.pixelpoint.magicpet.lesson.route;

import studio.pixelpoint.magicpet.application.Button;
import studio.pixelpoint.magicpet.application.IncomingMessage;
import studio.pixelpoint.magicpet.application.PetFacade;
import studio.pixelpoint.magicpet.domain.*;
import studio.pixelpoint.magicpet.lesson.StudentBot;

import java.util.List;

public final class BlogLesson implements LessonRoute {
    @Override public Scenario scenario() { return Scenario.BLOG; }
    @Override public boolean selectionPressed(IncomingMessage message) { return message.buttonPressed("scenario:blog"); }
    @Override public void select(PetFacade pet, UserSession user) {
        if (user.state() == UserState.CHOOSING_SCENARIO) pet.selectScenario(user, Scenario.BLOG);
        else pet.repeatPrompt(user);
    }
    @Override public void acceptPetName(PetFacade pet, UserSession user, String name) { pet.namePet(user, name); }

    // ISSUE B3 — кнопка «Подсказка»
    // ISSUE B4 — кнопка «Весь план»
    @Override public List<Button> taskButtons() {
        // TODO STUDENT B3: добавь кнопку подсказки и её обработчик ниже.
        // TODO STUDENT B4: добавь кнопку «Весь план» и её обработчик ниже.
        return List.of();
    }

    @Override public boolean handleButton(PetFacade pet, UserSession user, IncomingMessage message) {
        // ISSUE B1 — «Мои задачи» показывает только активные
        if (message.buttonPressed("action:my_tasks")) {
            // TODO STUDENT B1: выбери правильный список задач.
            if (StudentBot.journeyReady(user)) pet.showTasks(user, true); else pet.repeatPrompt(user);
            return true;
        }

        // ISSUE B2 — подтверждённое удаление не выполняет задачу и не начисляет XP
        Long taskId = StudentBot.callbackId(message.buttonId(), "task:delete:");
        if (taskId != null) {
            // TODO STUDENT B2: вызови действие с правильным эффектом.
            pet.completeTask(user, taskId, message.deliveryId());
            return true;
        }
        return false;
    }

    @Override public boolean handleText(PetFacade pet, UserSession user, IncomingMessage message) { return false; }

    @Override public void afterTaskCompleted(PetFacade pet, UserSession user, ProgressResult result) {
        UserSession refreshed = pet.refreshUser(user);
        pet.showProgress(refreshed, result);
        if (result.levelUp()) {
            pet.showLevelUp(refreshed, result);
            pet.showPet(refreshed, result.level());
        }
        pet.continueAfterCompletion(refreshed, result, taskButtons());
    }
}
