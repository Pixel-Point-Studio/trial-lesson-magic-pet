package studio.pixelpoint.magicpet.lesson.route;

import studio.pixelpoint.magicpet.application.Button;
import studio.pixelpoint.magicpet.application.IncomingMessage;
import studio.pixelpoint.magicpet.application.PetFacade;
import studio.pixelpoint.magicpet.domain.*;
import studio.pixelpoint.magicpet.lesson.StudentBot;

import java.util.List;

public final class SportLesson implements LessonRoute {
    @Override public Scenario scenario() { return Scenario.SPORT; }
    @Override public boolean selectionPressed(IncomingMessage message) { return message.buttonPressed("scenario:sport"); }
    @Override public void select(PetFacade pet, UserSession user) {
        if (user.state() == UserState.CHOOSING_SCENARIO) pet.selectScenario(user, Scenario.SPORT);
        else pet.repeatPrompt(user);
    }
    @Override public void acceptPetName(PetFacade pet, UserSession user, String name) { pet.namePet(user, name); }

    // ISSUE I1 — граница уровня находится в LevelProgression.levelFor
    // ISSUE I3 — удаление сначала просит подтверждение
    // ISSUE I4 — кнопка «До следующего уровня»
    @Override public List<Button> taskButtons() {
        // TODO STUDENT I4: добавь кнопку прогресса и её обработчик ниже.
        return List.of();
    }

    @Override public boolean handleButton(PetFacade pet, UserSession user, IncomingMessage message) {
        Long taskId = StudentBot.callbackId(message.buttonId(), "task:confirm_delete:");
        if (taskId != null) {
            // TODO STUDENT I3: сначала покажи вопрос с кнопками «Да» и «Нет».
            pet.deleteTask(user, taskId);
            return true;
        }
        return false;
    }

    @Override public boolean handleText(PetFacade pet, UserSession user, IncomingMessage message) { return false; }

    // ISSUE I2 — изображение использует фактический новый уровень
    @Override public void afterTaskCompleted(PetFacade pet, UserSession user, ProgressResult result) {
        UserSession refreshed = pet.refreshUser(user);
        pet.showProgress(refreshed, result);
        if (result.levelUp()) {
            pet.showLevelUp(refreshed, result);
            // TODO STUDENT I2: покажи изображение фактического нового уровня.
            pet.showPet(refreshed, 1);
        }
        pet.continueAfterCompletion(refreshed, result, taskButtons());
    }
}
