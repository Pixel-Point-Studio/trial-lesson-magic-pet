package studio.pixelpoint.magicpet.lesson;

import studio.pixelpoint.magicpet.application.IncomingMessage;
import studio.pixelpoint.magicpet.application.PetFacade;
import studio.pixelpoint.magicpet.domain.ProgressResult;
import studio.pixelpoint.magicpet.domain.UserSession;
import studio.pixelpoint.magicpet.domain.UserState;
import studio.pixelpoint.magicpet.lesson.route.LessonRoute;
import studio.pixelpoint.magicpet.lesson.route.LessonRouteRegistry;

/** Общий порядок обработки. Учебные задачи находятся в lesson/route. */
public final class StudentBot {
    private final PetFacade pet;
    private final boolean resetEnabled;
    private final LessonRouteRegistry routes = new LessonRouteRegistry();

    public StudentBot(PetFacade pet) { this(pet, false); }

    public StudentBot(PetFacade pet, boolean resetEnabled) {
        this.pet = pet;
        this.resetEnabled = resetEnabled;
    }

    public void receiveMessage(IncomingMessage message) {
        UserSession user = pet.getOrCreateUser(message.userId(), message.displayName());
        if (message.isCommand("/start")) {
            if (user.state() == UserState.NEW) pet.start(user); else repeat(user);
            return;
        }
        if (message.isCommand("/reset")) {
            if (resetEnabled) pet.start(user); else repeat(user);
            return;
        }

        LessonRoute selected = routes.findSelection(message);
        if (selected != null) {
            selected.select(pet, user);
            return;
        }
        LessonRoute route = routes.forScenario(user.scenario());
        if (route != null && user.state() == UserState.WAITING_PET_NAME && message.hasText()) {
            route.acceptPetName(pet, user, message.text());
            return;
        }
        if (route != null && route.handleText(pet, user, message)) return;
        if (route != null && route.handleButton(pet, user, message)) return;

        if (message.buttonPressed("action:task_done") && route != null) {
            if (user.state() == UserState.ACTIVE) {
                ProgressResult result = pet.completeCurrentTask(user, message.deliveryId());
                route.afterTaskCompleted(pet, user, result);
            } else repeat(user);
            return;
        }
        if (message.buttonPressed("action:my_tasks")) {
            if (journeyReady(user)) pet.showTasks(user, false); else repeat(user);
            return;
        }
        if (message.buttonPressed("action:completed_tasks")) {
            if (journeyReady(user)) pet.showTasks(user, true); else repeat(user);
            return;
        }
        if (message.buttonPressed("action:add_task")) {
            pet.beginTaskCreation(user);
            return;
        }

        Long taskId = callbackId(message.buttonId(), "task:open:");
        if (taskId != null) {
            if (journeyReady(user)) pet.showTask(user, taskId); else repeat(user);
            return;
        }
        taskId = callbackId(message.buttonId(), "task:done:");
        if (taskId != null) {
            if (journeyReady(user)) pet.completeTask(user, taskId, message.deliveryId()); else repeat(user);
            return;
        }
        taskId = callbackId(message.buttonId(), "task:delete:");
        if (taskId != null) {
            if (journeyReady(user)) pet.deleteTask(user, taskId); else repeat(user);
            return;
        }

        if (message.hasText() && user.state() == UserState.WAITING_GOAL) {
            pet.createPlan(user, message.text());
            return;
        }
        if (message.hasText() && user.state() == UserState.WAITING_TASK_TITLE) {
            pet.acceptTaskTitle(user, message.text());
            return;
        }
        if (message.hasText() && user.state() == UserState.WAITING_TASK_DESCRIPTION) {
            pet.acceptTaskDescription(user, message.text());
            return;
        }
        repeat(user);
    }

    private void repeat(UserSession user) {
        LessonRoute route = routes.forScenario(user.scenario());
        if (route != null && user.state() == UserState.ACTIVE) pet.showCurrentTask(user, route.taskButtons());
        else pet.repeatPrompt(user);
    }

    public static Long callbackId(String buttonId, String prefix) {
        if (buttonId == null || !buttonId.startsWith(prefix)) return null;
        try { return Long.parseLong(buttonId.substring(prefix.length())); }
        catch (NumberFormatException ignored) { return null; }
    }

    public static boolean journeyReady(UserSession user) {
        return user.state() == UserState.ACTIVE || user.state() == UserState.PLAN_COMPLETED;
    }
}
