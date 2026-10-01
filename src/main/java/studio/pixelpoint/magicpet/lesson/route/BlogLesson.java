package studio.pixelpoint.magicpet.lesson.route;

import studio.pixelpoint.magicpet.application.IncomingMessage;
import studio.pixelpoint.magicpet.application.PetFacade;
import studio.pixelpoint.magicpet.domain.Scenario;
import studio.pixelpoint.magicpet.domain.UserSession;
import studio.pixelpoint.magicpet.domain.UserState;

/** Выбор Скрибы и направления «Личный блог». */
public final class BlogLesson implements LessonRoute {
    @Override public Scenario scenario() { return Scenario.BLOG; }
    @Override public boolean selectionPressed(IncomingMessage message) { return message.buttonPressed("scenario:blog"); }
    @Override public void select(PetFacade pet, UserSession user) {
        if (user.state() == UserState.CHOOSING_SCENARIO) pet.selectScenario(user, scenario());
        else pet.repeatPrompt(user);
    }
}
