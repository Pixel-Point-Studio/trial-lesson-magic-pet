package studio.pixelpoint.magicpet.infrastructure.memory;

import studio.pixelpoint.magicpet.application.port.UserStore;
import studio.pixelpoint.magicpet.domain.UserSession;
import studio.pixelpoint.magicpet.domain.ProgressResult;
import studio.pixelpoint.magicpet.domain.UserTask;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Set;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public final class InMemoryUserStore implements UserStore {
    private final ConcurrentMap<Long, UserSession> sessions = new ConcurrentHashMap<>();
    private final Set<String> handledDeliveries = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<Long, ConcurrentMap<Long, MemoryTask>> customTasks = new ConcurrentHashMap<>();
    private final AtomicLong taskIds = new AtomicLong();

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

    @Override
    public List<UserTask> findTasks(long userId, boolean completed) {
        return customTasks.getOrDefault(userId, new ConcurrentHashMap<>()).values().stream()
                .filter(task -> task.completed == completed)
                .map(MemoryTask::view)
                .sorted(java.util.Comparator.comparingLong(UserTask::id))
                .toList();
    }

    @Override
    public Optional<UserTask> findTask(long userId, long taskId) {
        MemoryTask task = customTasks.getOrDefault(userId, new ConcurrentHashMap<>()).get(taskId);
        return task == null ? Optional.empty() : Optional.of(task.view());
    }

    @Override
    public long addCustomTask(long userId, String title, String description) {
        long id = taskIds.incrementAndGet();
        customTasks.computeIfAbsent(userId, ignored -> new ConcurrentHashMap<>())
                .put(id, new MemoryTask(id, title, description));
        return id;
    }

    @Override
    public ProgressResult completeTask(long userId, long taskId, String deliveryId) {
        UserSession session = sessions.get(userId);
        MemoryTask task = customTasks.getOrDefault(userId, new ConcurrentHashMap<>()).get(taskId);
        if (session == null || task == null || task.completed
                || (deliveryId != null && !handledDeliveries.add(userId + ":" + deliveryId))) {
            int xp = session == null ? 0 : session.experience();
            int level = session == null ? 1 : session.level();
            return new ProgressResult(false, 0, xp, level, false, false);
        }
        synchronized (session) {
            task.completed = true;
            return session.awardTaskXp();
        }
    }

    @Override
    public boolean deleteTask(long userId, long taskId) {
        return customTasks.getOrDefault(userId, new ConcurrentHashMap<>()).remove(taskId) != null;
    }

    private static final class MemoryTask {
        private final long id;
        private final String title;
        private final String description;
        private boolean completed;

        private MemoryTask(long id, String title, String description) {
            this.id = id;
            this.title = title;
            this.description = description;
        }

        private UserTask view() { return new UserTask(id, title, description, completed, true, (int) id); }
    }
}
