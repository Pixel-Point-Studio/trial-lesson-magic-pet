package studio.pixelpoint.magicpet.infrastructure.memory;

import studio.pixelpoint.magicpet.application.port.UserStore;
import studio.pixelpoint.magicpet.domain.UserSession;
import studio.pixelpoint.magicpet.domain.ProgressResult;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Set;

public final class InMemoryUserStore implements UserStore {
    private final ConcurrentMap<Long, UserSession> sessions = new ConcurrentHashMap<>();
    private final Set<String> handledDeliveries = ConcurrentHashMap.newKeySet();

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

    @Override
    public ProgressResult completeCurrentTask(long userId, String deliveryId) {
        UserSession session = sessions.get(userId);
        if (session == null) throw new IllegalStateException("Пользователь не найден");
        synchronized (session) {
            if (deliveryId != null && !handledDeliveries.add(userId + ":" + deliveryId)) {
                return new ProgressResult(false, 0, session.experience(), session.level(), false,
                        session.state() == studio.pixelpoint.magicpet.domain.UserState.PLAN_COMPLETED);
            }
            return session.completeCurrentTask();
        }
    }
}
