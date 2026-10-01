package studio.pixelpoint.magicpet.lesson.route;

import studio.pixelpoint.magicpet.application.IncomingMessage;
import studio.pixelpoint.magicpet.application.PetFacade;
import studio.pixelpoint.magicpet.domain.Scenario;
import studio.pixelpoint.magicpet.domain.UserSession;

/**
 * Общая форма одного направления Magic Pet: «Учёба», «Спорт» или «Личный блог».
 *
 * <p>Направление выбирает внешний вид Pet и тему AI-плана. Учебные задания одинаковы
 * для всех направлений и находятся в одном файле {@code PetFeatures.java}.</p>
 */
public interface LessonRoute {
    Scenario scenario();
    default Scenario displayedPetScenario() { return scenario(); }
    boolean selectionPressed(IncomingMessage message);
    void select(PetFacade pet, UserSession user);
}
