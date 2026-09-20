package studio.pixelpoint.magicpet.application.port;

import studio.pixelpoint.magicpet.domain.UserSession;

public interface UserStore {
    UserSession getOrCreate(long userId, String displayName);
    void save(UserSession session);
}
