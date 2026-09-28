package studio.pixelpoint.magicpet.lesson.route;

import studio.pixelpoint.magicpet.application.Button;
import studio.pixelpoint.magicpet.application.IncomingMessage;
import studio.pixelpoint.magicpet.application.PetFacade;
import studio.pixelpoint.magicpet.domain.ProgressResult;
import studio.pixelpoint.magicpet.domain.Scenario;
import studio.pixelpoint.magicpet.domain.UserSession;

import java.util.List;

/**
 * Общая форма одного направления Magic Pet: «Учёба», «Спорт» или «Личный блог».
 *
 * <p>Каждое направление реализовано отдельным маленьким классом. На занятии студент обычно
 * работает именно в таком классе, а готовые действия вызывает через {@link PetFacade}.</p>
 *
 * <p>{@code taskButtons} описывает дополнительные кнопки, {@code handleButton} реагирует
 * на нажатия, {@code handleText} принимает следующий текст пользователя, а
 * {@code afterTaskCompleted} показывает результат выполненного задания.</p>
 */
public interface LessonRoute {
    Scenario scenario();
    default Scenario displayedPetScenario() { return scenario(); }
    boolean selectionPressed(IncomingMessage message);
    void select(PetFacade pet, UserSession user);
    void acceptPetName(PetFacade pet, UserSession user, String name);
    List<Button> taskButtons();
    boolean handleButton(PetFacade pet, UserSession user, IncomingMessage message);
    boolean handleText(PetFacade pet, UserSession user, IncomingMessage message);
    void afterTaskCompleted(PetFacade pet, UserSession user, ProgressResult result);
}
