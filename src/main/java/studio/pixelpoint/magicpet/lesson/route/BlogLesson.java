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
        return List.of(
                new Button("action:hint", "💡 Подсказка"),
                new Button("action:show_plan", "🗺 Весь план"));
    }

    @Override public boolean handleButton(PetFacade pet, UserSession user, IncomingMessage message) {
        // ISSUE B1 — «Мои задачи» показывает только активные
        if (message.buttonPressed("action:my_tasks")) {
            if (StudentBot.journeyReady(user)) pet.showTasks(user, false); else pet.repeatPrompt(user);
            return true;
        }

        if (message.buttonPressed("action:hint")) {
            pet.showHint(user);
            return true;
        }

        // ISSUE B2 — подтверждённое удаление не выполняет задачу и не начисляет XP
        Long taskId = StudentBot.callbackId(message.buttonId(), "task:delete:");
        if (taskId != null) {
            pet.deleteTask(user, taskId);
            return true;
        }
        if (message.buttonPressed("action:show_plan")) {
            pet.showPlan(user);
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
