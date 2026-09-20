package studio.pixelpoint.magicpet.lesson.route;

import studio.pixelpoint.magicpet.application.Button;
import studio.pixelpoint.magicpet.application.IncomingMessage;
import studio.pixelpoint.magicpet.application.PetFacade;
import studio.pixelpoint.magicpet.domain.*;

import java.util.List;

public final class StudyLesson implements LessonRoute {
    @Override public Scenario scenario() { return Scenario.STUDY; }
    @Override public boolean selectionPressed(IncomingMessage message) { return message.buttonPressed("scenario:study"); }

    // ISSUE S1 — после выбора появляется другой Pet
    @Override public void select(PetFacade pet, UserSession user) {
        if (user.state() == UserState.CHOOSING_SCENARIO) pet.selectScenario(user, Scenario.STUDY);
        else pet.repeatPrompt(user);
    }

    // ISSUE S2 — бот запоминает придуманное имя
    @Override public void acceptPetName(PetFacade pet, UserSession user, String name) {
        pet.namePet(user, name);
    }

    // ISSUE S3 — кнопка «Мой Pet»
    // ISSUE S4 — переименование Pet
    @Override public List<Button> taskButtons() {
        return List.of(
                new Button("action:show_pet", "🐾 Мой Pet"),
                new Button("action:rename_pet", "✏️ Переименовать Pet"));
    }

    @Override public boolean handleButton(PetFacade pet, UserSession user, IncomingMessage message) {
        if (message.buttonPressed("action:show_pet")) {
            pet.showPet(user);
            return true;
        }
        if (message.buttonPressed("action:rename_pet")) {
            pet.beginPetRename(user);
            return true;
        }
        return false;
    }

    @Override public boolean handleText(PetFacade pet, UserSession user, IncomingMessage message) {
        if (user.state() != UserState.WAITING_PET_RENAME || !message.hasText()) return false;
        pet.renamePet(user, message.text());
        return true;
    }

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
