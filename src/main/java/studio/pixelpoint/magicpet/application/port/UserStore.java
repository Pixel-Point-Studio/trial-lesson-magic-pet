package studio.pixelpoint.magicpet.application.port;

import studio.pixelpoint.magicpet.domain.UserSession;
import studio.pixelpoint.magicpet.domain.ProgressResult;

public interface UserStore {
    UserSession getOrCreate(long userId, String displayName);
    void save(UserSession session);
    ProgressResult completeCurrentTask(long userId, String deliveryId);
}
