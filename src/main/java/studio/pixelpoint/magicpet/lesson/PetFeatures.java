package studio.pixelpoint.magicpet.lesson;

import studio.pixelpoint.magicpet.application.Button;
import studio.pixelpoint.magicpet.application.IncomingMessage;
import studio.pixelpoint.magicpet.application.PetFacade;
import studio.pixelpoint.magicpet.domain.UserSession;
import studio.pixelpoint.magicpet.domain.UserState;

import java.util.ArrayList;
import java.util.List;

/**
 * Единственный файл, который студент меняет на пробном уроке.
 *
 * <p>Все сложные части Telegram, базы данных и AI уже спрятаны в {@link PetFacade}.
 * Здесь мы только связываем понятное действие пользователя с готовым действием продукта.</p>
 */
public final class PetFeatures {

    /** Получает введённое пользователем имя и просит готовую часть продукта сохранить его. */
    public void savePetName(PetFacade pet, UserSession user, String enteredName) {
        // TODO STUDENT 1: передай в namePet имя из переменной enteredName вместо готового текста.
        pet.namePet(user, "Magic Pet");
    }

    /**
     * Собирает общие кнопки меню. Они показываются под кнопками текущего действия.
     *
     * <p>У Button два текста: слева скрытое имя действия для программы,
     * справа подпись, которую человек увидит в Telegram.</p>
     */
    public List<Button> menuButtons() {
        List<Button> buttons = new ArrayList<>();

        // TODO STUDENT 2: убери // у следующей строки, чтобы добавить кнопку переименования.
        // buttons.add(new Button("action:rename_pet", "✏️ Переименовать Pet"));

        // TODO STUDENT 4 (дополнительно): убери //, чтобы добавить кнопку показа Pet.
        // buttons.add(new Button("action:show_pet", "🐾 Показать моего Pet"));

        return buttons;
    }

    /** Реагирует на нажатие кнопки по её скрытому имени действия. */
    public boolean handleButton(PetFacade pet, UserSession user, IncomingMessage message) {
        if (message.buttonPressed("action:rename_pet")) {
            // ЗАДАНИЕ 2, часть 2: убери // — программа попросит пользователя написать новое имя.
            // pet.beginPetRename(user);
            return true; // true означает: «эту кнопку узнали, дальше её искать не нужно».
        }

        if (message.buttonPressed("action:show_pet")) {
            // ЗАДАНИЕ 4, часть 2: убери // — готовый метод покажет Pet, уровень и XP.
            // pet.showPet(user);
            return true;
        }

        // В скрытом имени кнопки удаления находится номер выбранной задачи.
        Long taskId = StudentBot.callbackId(message.buttonId(), "task:confirm_delete:");
        if (taskId != null) {
            // TODO STUDENT 3: вместо немедленного удаления вызови confirmTaskDeletion.
            pet.deleteTask(user, taskId, menuButtons());
            return true;
        }

        return false; // false означает: «это не наша кнопка, пусть программа проверит остальные».
    }

    /** Принимает обычный текст, если перед этим пользователь нажал «Переименовать Pet». */
    public boolean handleText(PetFacade pet, UserSession user, IncomingMessage message) {
        if (user.state() == UserState.WAITING_PET_RENAME && message.hasText()) {
            // ЗАДАНИЕ 2, часть 3: убери // — текст сообщения станет новым именем.
            // pet.renamePet(user, message.text());
            return true;
        }
        return false;
    }
}
