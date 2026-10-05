package studio.pixelpoint.magicpet.lesson;

import studio.pixelpoint.magicpet.application.Button;
import studio.pixelpoint.magicpet.application.IncomingMessage;
import studio.pixelpoint.magicpet.application.PetFacade;
import studio.pixelpoint.magicpet.domain.UserSession;
import studio.pixelpoint.magicpet.domain.UserState;

import java.util.ArrayList;
import java.util.List;

/**
 * Главный файл для работы со сценарием бота
 *
 * Интеграция с Telegram, базой данных и AI (ChatGPT) реализованы в
 * {@link PetFacade}.
 * Здесь мы связываем действие пользователя с реакцией продукта.
 */
public final class PetFeatures {

    /**
     * Метод получает введённое пользователем имя, записывает его в
     * переменную-параметр enteredName
     * вызывает метод сохранения нового имени namePet().
     */
    public void savePetName(PetFacade pet, UserSession user, String enteredName) {
        // TODO 1 - нужно использовать введённое пользователем имя, а не "Magic Pet"
        pet.namePet(user, "Magic Pet");
    }

    /**
     * Метод собирает кнопки меню. Они показываются под кнопками текущего действия.
     * Для добавления новых кнопок нужно создать новый объект Button и добавить его
     * в список buttons.
     */
    public List<Button> menuButtons() {
        List<Button> buttons = new ArrayList<>();

        // TODO 4.1 нужно добавить новую кнопку для переименования питомца со скрытым
        // идентификатором "action:rename_pet"

        // TODO 3.1 нужно добавить новую кнопку для показа питомца со скрытым
        // идентификатором "action:show_pet"

        return buttons;
    }

    /** Реагирует на нажатие кнопки по её скрытому имени действия. */
    public boolean handleButton(PetFacade pet, UserSession user, IncomingMessage message) {
        if (message.buttonPressed("action:rename_pet")) {
            // TODO 4.2 нужно попросить пользователя ввести новое имя питомца

            return true; // означает: «эту кнопку узнали и обработали, дальше её искать не нужно».
        }

        // TODO 3.2 нужно показать информацию о питомце пользователя - имя, уровень и
        // XP, при условии, что пользователь нажал кнопку "action:show_pet"

        // В скрытом имени кнопки удаления находится номер выбранной задачи.
        Long taskId = StudentBot.callbackId(message.buttonId(), "task:confirm_delete:");
        if (taskId != null) {
            // TODO 2 нужно использовать удаление задачи с подтверждением вместо прямого
            // удаления.
            pet.deleteTask(user, taskId, menuButtons());
            return true;
        }

        return false; // false означает: «эту кнопку в этом методе мы не обрабатываем».
    }

    /**
     * Принимает обычный текст, если перед этим пользователь нажал «Переименовать
     * питомца.
     * user.state() — текущий шаг разговора, а WAITING_PET_RENAME означает
     * «бот ждёт, что следующим сообщением человек пришлёт новое имя».
     */
    public boolean handleText(PetFacade pet, UserSession user, IncomingMessage message) {
        if (user.state() == UserState.WAITING_PET_RENAME) {
            // TODO 4.3 нужно вызвать метод переименования питомца с введённым пользователем
            // именем
            return true;
        }
        return false;
    }
}
