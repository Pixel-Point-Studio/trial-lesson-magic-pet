package studio.pixelpoint.magicpet.application.port;

import studio.pixelpoint.magicpet.domain.UserSession;
import studio.pixelpoint.magicpet.domain.ProgressResult;
import studio.pixelpoint.magicpet.domain.UserTask;

import java.util.List;
import java.util.Optional;

public interface UserStore {
    UserSession getOrCreate(long userId, String displayName);
    void save(UserSession session);
    ProgressResult completeCurrentTask(long userId, String deliveryId);
    List<UserTask> findTasks(long userId, boolean completed);
    Optional<UserTask> findTask(long userId, long taskId);
    long addCustomTask(long userId, String title, String description);
    ProgressResult completeTask(long userId, long taskId, String deliveryId);
    boolean deleteTask(long userId, long taskId);
}
