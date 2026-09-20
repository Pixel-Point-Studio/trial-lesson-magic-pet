package studio.pixelpoint.magicpet.lesson.route;

import studio.pixelpoint.magicpet.application.Button;
import studio.pixelpoint.magicpet.application.IncomingMessage;
import studio.pixelpoint.magicpet.application.PetFacade;
import studio.pixelpoint.magicpet.domain.*;

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
    // ISSUE I3 — кнопка «Подсказка»
    // ISSUE I4 — кнопка «До следующего уровня»
    @Override public List<Button> taskButtons() {
        return List.of(
                new Button("action:hint", "💡 Подсказка"),
                new Button("action:level_progress", "📈 До следующего уровня"));
    }

    @Override public boolean handleButton(PetFacade pet, UserSession user, IncomingMessage message) {
        if (message.buttonPressed("action:hint")) {
            pet.showHint(user);
            return true;
        }
        if (message.buttonPressed("action:level_progress")) {
            pet.showLevelProgress(user);
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
            pet.showPet(refreshed, result.level());
        }
        pet.continueAfterCompletion(refreshed, result, taskButtons());
    }
}
