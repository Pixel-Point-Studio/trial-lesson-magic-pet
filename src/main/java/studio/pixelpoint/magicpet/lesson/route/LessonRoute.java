package studio.pixelpoint.magicpet.lesson.route;

import studio.pixelpoint.magicpet.application.Button;
import studio.pixelpoint.magicpet.application.IncomingMessage;
import studio.pixelpoint.magicpet.application.PetFacade;
import studio.pixelpoint.magicpet.domain.ProgressResult;
import studio.pixelpoint.magicpet.domain.Scenario;
import studio.pixelpoint.magicpet.domain.UserSession;

import java.util.List;

public interface LessonRoute {
    Scenario scenario();
    boolean selectionPressed(IncomingMessage message);
    void select(PetFacade pet, UserSession user);
    void acceptPetName(PetFacade pet, UserSession user, String name);
    List<Button> taskButtons();
    boolean handleButton(PetFacade pet, UserSession user, IncomingMessage message);
    boolean handleText(PetFacade pet, UserSession user, IncomingMessage message);
    void afterTaskCompleted(PetFacade pet, UserSession user, ProgressResult result);
}
