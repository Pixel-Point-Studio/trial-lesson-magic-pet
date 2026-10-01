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
        pet.namePet(user, enteredName);
    }

    /**
     * Собирает общие кнопки меню. Они показываются под кнопками текущего действия.
     *
     * <p>У Button два текста: слева скрытое имя действия для программы,
     * справа подпись, которую человек увидит в Telegram.</p>
     */
    public List<Button> menuButtons() {
        List<Button> buttons = new ArrayList<>();

        buttons.add(new Button("action:rename_pet", "✏️ Переименовать Pet"));
        buttons.add(new Button("action:show_pet", "🐾 Показать моего Pet"));

        return buttons;
    }

    /** Реагирует на нажатие кнопки по её скрытому имени действия. */
    public boolean handleButton(PetFacade pet, UserSession user, IncomingMessage message) {
        if (message.buttonPressed("action:rename_pet")) {
            pet.beginPetRename(user);
            return true; // true означает: «эту кнопку узнали, дальше её искать не нужно».
        }

        if (message.buttonPressed("action:show_pet")) {
            pet.showPet(user);
            return true;
        }

        // В скрытом имени кнопки удаления находится номер выбранной задачи.
        Long taskId = StudentBot.callbackId(message.buttonId(), "task:confirm_delete:");
        if (taskId != null) {
            pet.confirmTaskDeletion(user, taskId, menuButtons());
            return true;
        }

        return false; // false означает: «это не наша кнопка, пусть программа проверит остальные».
    }

    /**
     * Принимает обычный текст, если перед этим пользователь нажал «Переименовать Pet».
     * user.state() — текущий шаг разговора, а WAITING_PET_RENAME означает
     * «бот ждёт, что следующим сообщением человек пришлёт новое имя».
     */
    public boolean handleText(PetFacade pet, UserSession user, IncomingMessage message) {
        if (user.state() == UserState.WAITING_PET_RENAME && message.hasText()) {
            pet.renamePet(user, message.text());
            return true;
        }
        return false;
    }
}
