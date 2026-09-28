package studio.pixelpoint.magicpet.lesson.route;

import studio.pixelpoint.magicpet.application.Button;
import studio.pixelpoint.magicpet.application.IncomingMessage;
import studio.pixelpoint.magicpet.application.PetFacade;
import studio.pixelpoint.magicpet.domain.*;

import java.util.List;

/**
 * Поведение Руни и направления «Учёба».
 *
 * <p>Ищи в файле комментарий с кодом своей задачи {@code S...}. Готовые действия уже находятся в объекте
 * {@code pet}: их можно исследовать через Cmd+клик или Ctrl+клик по названию метода.</p>
 */
public final class StudyLesson implements LessonRoute {
    @Override public Scenario scenario() { return Scenario.STUDY; }
    // TODO STUDENT S1: проверь, изображение какого Pet показывается после создания плана.
    @Override public Scenario displayedPetScenario() { return Scenario.SPORT; }
    @Override public boolean selectionPressed(IncomingMessage message) { return message.buttonPressed("scenario:study"); }

    // ISSUE S1 — после выбора появляется другой Pet
    @Override public void select(PetFacade pet, UserSession user) {
        if (user.state() == UserState.CHOOSING_SCENARIO) pet.selectScenario(user, Scenario.STUDY);
        else pet.repeatPrompt(user);
    }

    // ISSUE S2 — бот запоминает придуманное имя
    @Override public void acceptPetName(PetFacade pet, UserSession user, String name) {
        // TODO STUDENT S2: передай имя, которое написал пользователь.
        pet.namePet(user, "Руни");
    }

    // Дополнительные кнопки, которые пользователь видит под текущим заданием.
    // ISSUE S3 — кнопка «Мой Pet»
    // ISSUE S4 — переименование Pet
    @Override public List<Button> taskButtons() {
        // TODO STUDENT S3: добавь кнопку «Мой Pet» и её обработчик ниже.
        // TODO STUDENT S4: добавь кнопку переименования, обработчик и приём нового имени.
        return List.of();
    }

    // Скрытое имя кнопки связывает нажатие в Telegram с готовым действием у pet.
    @Override public boolean handleButton(PetFacade pet, UserSession user, IncomingMessage message) {
        return false;
    }

    // Этот метод нужен, когда после кнопки программа ждёт следующий текст пользователя.
    @Override public boolean handleText(PetFacade pet, UserSession user, IncomingMessage message) {
        return false;
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
