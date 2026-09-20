package studio.pixelpoint.magicpet.infrastructure.memory;

import studio.pixelpoint.magicpet.application.port.UserStore;
import studio.pixelpoint.magicpet.domain.UserSession;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryUserStore implements UserStore {
    private final ConcurrentMap<Long, UserSession> sessions = new ConcurrentHashMap<>();

    @Override
    public UserSession getOrCreate(long userId, String displayName) {
        UserSession session = sessions.computeIfAbsent(userId, id -> new UserSession(id, displayName));
        session.updateDisplayName(displayName);
        return session;
    }

    @Override
    public void save(UserSession session) {
        sessions.put(session.userId(), session);
    }
}
