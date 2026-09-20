package studio.pixelpoint.magicpet.infrastructure.sqlite;

import studio.pixelpoint.magicpet.application.port.UserStore;
import studio.pixelpoint.magicpet.domain.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SqliteUserStore implements UserStore {
    private final String jdbcUrl;

    public SqliteUserStore(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    @Override
    public UserSession getOrCreate(long userId, String displayName) {
        try (Connection connection = open()) {
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO users(telegram_user_id, display_name, state)
                    VALUES (?, ?, 'NEW')
                    ON CONFLICT(telegram_user_id) DO UPDATE SET
                        display_name = CASE WHEN excluded.display_name <> '' THEN excluded.display_name ELSE users.display_name END,
                        updated_at = CURRENT_TIMESTAMP
                    """)) {
                insert.setLong(1, userId);
                insert.setString(2, displayName == null ? "" : displayName);
                insert.executeUpdate();
            }
            return load(connection, userId);
        } catch (SQLException error) {
            throw databaseError("Не удалось получить пользователя", error);
        }
    }

    @Override
    public void save(UserSession session) {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                long internalUserId = upsertUser(connection, session);
                if (session.scenario() == null) {
                    deleteJourney(connection, internalUserId);
                } else {
                    upsertPet(connection, internalUserId, session);
                    if (session.goal() != null && !session.tasks().isEmpty()
                            && !hasPlanTasks(connection, internalUserId)) {
                        savePlan(connection, internalUserId, session);
                    }
                }
                connection.commit();
            } catch (Exception error) {
                rollback(connection);
                throw error;
            }
        } catch (SQLException error) {
            throw databaseError("Не удалось сохранить пользователя", error);
        }
    }

    @Override
    public synchronized ProgressResult completeCurrentTask(long telegramUserId, String deliveryId) {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                CompletionTarget target = completionTarget(connection, telegramUserId);
                if (target != null && deliveryId != null
                        && isDuplicateDelivery(connection, target.internalUserId, deliveryId)) {
                    connection.rollback();
                    return new ProgressResult(false, 0, target.experience, levelFor(target.experience), false,
                            target.state == UserState.PLAN_COMPLETED);
                }
                if (target == null || target.taskId == null) {
                    connection.rollback();
                    int xp = target == null ? 0 : target.experience;
                    int level = levelFor(xp);
                    return new ProgressResult(false, 0, xp, level, false,
                            target != null && target.state == UserState.PLAN_COMPLETED);
                }

                int changed;
                try (PreparedStatement updateTask = connection.prepareStatement("""
                        UPDATE tasks SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP
                        WHERE id = ? AND user_id = ? AND status = 'PENDING'
                        """)) {
                    updateTask.setLong(1, target.taskId);
                    updateTask.setLong(2, target.internalUserId);
                    changed = updateTask.executeUpdate();
                }
                if (changed == 0) {
                    connection.rollback();
                    return new ProgressResult(false, 0, target.experience, levelFor(target.experience), false, false);
                }

                int newXp = target.experience + target.xpReward;
                int oldLevel = levelFor(target.experience);
                int newLevel = levelFor(newXp);
                boolean planCompleted = pendingTaskCount(connection, target.goalId) == 0;

                try (PreparedStatement updatePet = connection.prepareStatement("""
                        UPDATE pets SET experience = ?, level = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?
                        """)) {
                    updatePet.setInt(1, newXp);
                    updatePet.setInt(2, newLevel);
                    updatePet.setLong(3, target.internalUserId);
                    updatePet.executeUpdate();
                }
                if (deliveryId != null) rememberDelivery(connection, target.internalUserId, deliveryId);
                if (planCompleted) finishPlan(connection, target.internalUserId, target.goalId);
                connection.commit();
                return new ProgressResult(true, target.xpReward, newXp, newLevel,
                        newLevel > oldLevel, planCompleted);
            } catch (Exception error) {
                rollback(connection);
                throw error;
            }
        } catch (SQLException error) {
            throw databaseError("Не удалось завершить задачу", error);
        }
    }

    @Override
    public List<UserTask> findTasks(long telegramUserId, boolean completed) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT t.id, t.title, t.description, t.status, t.kind, t.position
                     FROM tasks t JOIN users u ON u.id = t.user_id
                     WHERE u.telegram_user_id = ? AND t.status = ?
                     ORDER BY t.created_at, t.position, t.id
                     """)) {
            statement.setLong(1, telegramUserId);
            statement.setString(2, completed ? "COMPLETED" : "PENDING");
            try (ResultSet result = statement.executeQuery()) {
                List<UserTask> tasks = new ArrayList<>();
                while (result.next()) tasks.add(toUserTask(result));
                return List.copyOf(tasks);
            }
        } catch (SQLException error) {
            throw databaseError("Не удалось получить задачи", error);
        }
    }

    @Override
    public Optional<UserTask> findTask(long telegramUserId, long taskId) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT t.id, t.title, t.description, t.status, t.kind, t.position
                     FROM tasks t JOIN users u ON u.id = t.user_id
                     WHERE u.telegram_user_id = ? AND t.id = ?
                     """)) {
            statement.setLong(1, telegramUserId);
            statement.setLong(2, taskId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(toUserTask(result)) : Optional.empty();
            }
        } catch (SQLException error) {
            throw databaseError("Не удалось открыть задачу", error);
        }
    }

    @Override
    public long addCustomTask(long telegramUserId, String title, String description) {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                long internalUserId = internalUserId(connection, telegramUserId);
                long goalId = latestGoalId(connection, internalUserId);
                int position;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT COALESCE(MAX(position), -1) + 1 FROM tasks WHERE goal_id = ?")) {
                    statement.setLong(1, goalId);
                    try (ResultSet result = statement.executeQuery()) {
                        result.next();
                        position = result.getInt(1);
                    }
                }
                long taskId;
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO tasks(user_id, goal_id, position, title, description, xp_reward, status, source, kind)
                        VALUES (?, ?, ?, ?, ?, 50, 'PENDING', 'FALLBACK', 'CUSTOM')
                        """, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setLong(1, internalUserId);
                    statement.setLong(2, goalId);
                    statement.setInt(3, position);
                    statement.setString(4, title);
                    statement.setString(5, description);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Не получен id задачи");
                        taskId = keys.getLong(1);
                    }
                }
                connection.commit();
                return taskId;
            } catch (Exception error) {
                rollback(connection);
                throw error;
            }
        } catch (SQLException error) {
            throw databaseError("Не удалось добавить задачу", error);
        }
    }

    @Override
    public synchronized ProgressResult completeTask(long telegramUserId, long taskId, String deliveryId) {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                CompletionTarget target = selectedTaskTarget(connection, telegramUserId, taskId);
                if (target == null || (deliveryId != null
                        && isDuplicateDelivery(connection, target.internalUserId, deliveryId))) {
                    connection.rollback();
                    int xp = target == null ? 0 : target.experience;
                    return new ProgressResult(false, 0, xp, levelFor(xp), false,
                            target != null && target.state == UserState.PLAN_COMPLETED);
                }
                int changed;
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE tasks SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP
                        WHERE id = ? AND user_id = ? AND status = 'PENDING'
                        """)) {
                    statement.setLong(1, target.taskId);
                    statement.setLong(2, target.internalUserId);
                    changed = statement.executeUpdate();
                }
                if (changed == 0) {
                    connection.rollback();
                    return new ProgressResult(false, 0, target.experience, levelFor(target.experience), false,
                            target.state == UserState.PLAN_COMPLETED);
                }
                int newXp = target.experience + target.xpReward;
                int oldLevel = levelFor(target.experience);
                int newLevel = levelFor(newXp);
                updateExperience(connection, target.internalUserId, newXp, newLevel);
                if (deliveryId != null) rememberDelivery(connection, target.internalUserId, deliveryId);
                boolean planCompleted = target.state == UserState.PLAN_COMPLETED;
                if (!target.custom && pendingTaskCount(connection, target.goalId) == 0) {
                    finishPlan(connection, target.internalUserId, target.goalId);
                    planCompleted = true;
                }
                connection.commit();
                return new ProgressResult(true, target.xpReward, newXp, newLevel,
                        newLevel > oldLevel, planCompleted);
            } catch (Exception error) {
                rollback(connection);
                throw error;
            }
        } catch (SQLException error) {
            throw databaseError("Не удалось завершить задачу", error);
        }
    }

    @Override
    public boolean deleteTask(long telegramUserId, long taskId) {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                CompletionTarget target = selectedTaskTarget(connection, telegramUserId, taskId);
                if (target == null) {
                    connection.rollback();
                    return false;
                }
                int changed;
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM tasks WHERE id = ? AND user_id = ?")) {
                    statement.setLong(1, taskId);
                    statement.setLong(2, target.internalUserId);
                    changed = statement.executeUpdate();
                }
                if (changed > 0 && !target.custom && pendingTaskCount(connection, target.goalId) == 0) {
                    finishPlan(connection, target.internalUserId, target.goalId);
                }
                connection.commit();
                return changed > 0;
            } catch (Exception error) {
                rollback(connection);
                throw error;
            }
        } catch (SQLException error) {
            throw databaseError("Не удалось удалить задачу", error);
        }
    }

    private UserSession load(Connection connection, long telegramUserId) throws SQLException {
        long internalId;
        String displayName;
        UserState state;
        String draftTaskTitle;
        UserState taskCreationReturnState;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, display_name, state, draft_task_title, task_creation_return_state FROM users WHERE telegram_user_id = ?")) {
            statement.setLong(1, telegramUserId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Пользователь не найден после создания");
                internalId = result.getLong("id");
                displayName = result.getString("display_name");
                state = UserState.valueOf(result.getString("state"));
                draftTaskTitle = result.getString("draft_task_title");
                String returnState = result.getString("task_creation_return_state");
                taskCreationReturnState = returnState == null ? null : UserState.valueOf(returnState);
            }
        }

        Scenario scenario = null;
        String petName = null;
        int experience = 0;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT name, scenario, experience FROM pets WHERE user_id = ?")) {
            statement.setLong(1, internalId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    petName = result.getString("name");
                    scenario = Scenario.valueOf(result.getString("scenario"));
                    experience = result.getInt("experience");
                }
            }
        }

        String goal = null;
        Long goalId = null;
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, goal_text FROM goals WHERE user_id = ?
                ORDER BY CASE status WHEN 'ACTIVE' THEN 0 ELSE 1 END, id DESC LIMIT 1
                """)) {
            statement.setLong(1, internalId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    goalId = result.getLong("id");
                    goal = result.getString("goal_text");
                }
            }
        }

        List<PlanTask> tasks = new ArrayList<>();
        if (goalId != null) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT title, description, source, status FROM tasks
                    WHERE goal_id = ? AND kind = 'PLAN' ORDER BY position
                    """)) {
                statement.setLong(1, goalId);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        tasks.add(new PlanTask(result.getString("title"), result.getString("description"),
                                PlanSource.valueOf(result.getString("source")),
                                "COMPLETED".equals(result.getString("status"))));
                    }
                }
            }
        }
        return UserSession.restore(telegramUserId, displayName, state, scenario, petName, goal, tasks, experience,
                draftTaskTitle, taskCreationReturnState);
    }

    private long upsertUser(Connection connection, UserSession session) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO users(telegram_user_id, display_name, state, draft_task_title, task_creation_return_state)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(telegram_user_id) DO UPDATE SET
                    display_name = excluded.display_name, state = excluded.state,
                    draft_task_title = excluded.draft_task_title,
                    task_creation_return_state = excluded.task_creation_return_state,
                    updated_at = CURRENT_TIMESTAMP
                """)) {
            statement.setLong(1, session.userId());
            statement.setString(2, session.displayName());
            statement.setString(3, session.state().name());
            statement.setString(4, session.draftTaskTitle());
            statement.setString(5, session.taskCreationReturnState() == null
                    ? null : session.taskCreationReturnState().name());
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM users WHERE telegram_user_id = ?")) {
            statement.setLong(1, session.userId());
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        }
    }

    private void deleteJourney(Connection connection, long userId) throws SQLException {
        try (PreparedStatement goals = connection.prepareStatement("DELETE FROM goals WHERE user_id = ?");
             PreparedStatement pets = connection.prepareStatement("DELETE FROM pets WHERE user_id = ?")) {
            goals.setLong(1, userId);
            goals.executeUpdate();
            pets.setLong(1, userId);
            pets.executeUpdate();
        }
    }

    private void upsertPet(Connection connection, long userId, UserSession session) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO pets(user_id, name, scenario, experience, level) VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(user_id) DO UPDATE SET name = excluded.name, scenario = excluded.scenario,
                    experience = excluded.experience, level = excluded.level, updated_at = CURRENT_TIMESTAMP
                """)) {
            statement.setLong(1, userId);
            statement.setString(2, session.petName());
            statement.setString(3, session.scenario().name());
            statement.setInt(4, session.experience());
            statement.setInt(5, session.level());
            statement.executeUpdate();
        }
    }

    private void savePlan(Connection connection, long userId, UserSession session) throws SQLException {
        Long goalId = activeGoalId(connection, userId);
        if (goalId == null) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO goals(user_id, scenario, goal_text, status) VALUES (?, ?, ?, 'ACTIVE')
                    """, Statement.RETURN_GENERATED_KEYS)) {
                statement.setLong(1, userId);
                statement.setString(2, session.scenario().name());
                statement.setString(3, session.goal());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Не получен id цели");
                    goalId = keys.getLong(1);
                }
            }
        }
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM tasks WHERE goal_id = ? AND kind = 'PLAN'")) {
            delete.setLong(1, goalId);
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO tasks(user_id, goal_id, position, title, description, xp_reward, status, source)
                VALUES (?, ?, ?, ?, ?, 50, ?, ?)
                """)) {
            for (int position = 0; position < session.tasks().size(); position++) {
                PlanTask task = session.tasks().get(position);
                insert.setLong(1, userId);
                insert.setLong(2, goalId);
                insert.setInt(3, position);
                insert.setString(4, task.title());
                insert.setString(5, task.description());
                insert.setString(6, task.completed() ? "COMPLETED" : "PENDING");
                insert.setString(7, task.source().name());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private boolean hasPlanTasks(Connection connection, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM tasks WHERE user_id = ? AND kind = 'PLAN' LIMIT 1")) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private Long activeGoalId(Connection connection, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM goals WHERE user_id = ? AND status = 'ACTIVE'")) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong(1) : null;
            }
        }
    }

    private long internalUserId(Connection connection, long telegramUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM users WHERE telegram_user_id = ?")) {
            statement.setLong(1, telegramUserId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Пользователь не найден");
                return result.getLong(1);
            }
        }
    }

    private long latestGoalId(Connection connection, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM goals WHERE user_id = ? ORDER BY id DESC LIMIT 1")) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Сначала нужно создать план");
                return result.getLong(1);
            }
        }
    }

    private UserTask toUserTask(ResultSet result) throws SQLException {
        return new UserTask(result.getLong("id"), result.getString("title"), result.getString("description"),
                "COMPLETED".equals(result.getString("status")),
                "CUSTOM".equals(result.getString("kind")), result.getInt("position"));
    }

    private CompletionTarget selectedTaskTarget(Connection connection, long telegramUserId, long taskId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT u.id AS user_id, u.state, u.last_completion_delivery_id, p.experience,
                       t.goal_id, t.id AS task_id, t.xp_reward, t.kind
                FROM users u
                JOIN pets p ON p.user_id = u.id
                JOIN tasks t ON t.user_id = u.id
                WHERE u.telegram_user_id = ? AND t.id = ?
                """)) {
            statement.setLong(1, telegramUserId);
            statement.setLong(2, taskId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                return new CompletionTarget(result.getLong("user_id"), UserState.valueOf(result.getString("state")),
                        result.getString("last_completion_delivery_id"), result.getInt("experience"),
                        result.getLong("goal_id"), result.getLong("task_id"), result.getInt("xp_reward"),
                        "CUSTOM".equals(result.getString("kind")));
            }
        }
    }

    private CompletionTarget completionTarget(Connection connection, long telegramUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT u.id AS user_id, u.state, u.last_completion_delivery_id, p.experience, g.id AS goal_id,
                       t.id AS task_id, COALESCE(t.xp_reward, 0) AS xp_reward
                FROM users u
                LEFT JOIN pets p ON p.user_id = u.id
                LEFT JOIN goals g ON g.user_id = u.id AND g.status = 'ACTIVE'
                LEFT JOIN tasks t ON t.id = (
                    SELECT id FROM tasks WHERE user_id = u.id AND goal_id = g.id
                        AND kind = 'PLAN' AND status = 'PENDING'
                    ORDER BY position LIMIT 1
                )
                WHERE u.telegram_user_id = ?
                """)) {
            statement.setLong(1, telegramUserId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                Long taskId = result.getObject("task_id") == null ? null : result.getLong("task_id");
                Long goalId = result.getObject("goal_id") == null ? null : result.getLong("goal_id");
                return new CompletionTarget(result.getLong("user_id"), UserState.valueOf(result.getString("state")),
                        result.getString("last_completion_delivery_id"), result.getInt("experience"),
                        goalId, taskId, result.getInt("xp_reward"), false);
            }
        }
    }

    private int pendingTaskCount(Connection connection, long goalId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM tasks WHERE goal_id = ? AND kind = 'PLAN' AND status = 'PENDING'")) {
            statement.setLong(1, goalId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private void updateExperience(Connection connection, long userId, int experience, int level) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE pets SET experience = ?, level = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?
                """)) {
            statement.setInt(1, experience);
            statement.setInt(2, level);
            statement.setLong(3, userId);
            statement.executeUpdate();
        }
    }

    private void finishPlan(Connection connection, long userId, long goalId) throws SQLException {
        try (PreparedStatement goal = connection.prepareStatement(
                "UPDATE goals SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP WHERE id = ? AND user_id = ?");
             PreparedStatement user = connection.prepareStatement(
                     "UPDATE users SET state = 'PLAN_COMPLETED', updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            goal.setLong(1, goalId);
            goal.setLong(2, userId);
            goal.executeUpdate();
            user.setLong(1, userId);
            user.executeUpdate();
        }
    }

    private void rememberDelivery(Connection connection, long userId, String deliveryId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE users SET last_completion_delivery_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """)) {
            statement.setString(1, deliveryId);
            statement.setLong(2, userId);
            statement.executeUpdate();
        }
    }

    private boolean isDuplicateDelivery(Connection connection, long userId, String deliveryId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO processed_deliveries(user_id, delivery_id) VALUES (?, ?)
                ON CONFLICT(user_id, delivery_id) DO NOTHING
                """)) {
            statement.setLong(1, userId);
            statement.setString(2, deliveryId);
            return statement.executeUpdate() == 0;
        }
    }

    private Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    private int levelFor(int xp) { return LevelProgression.levelFor(xp); }

    private void rollback(Connection connection) {
        try { connection.rollback(); } catch (SQLException ignored) { }
    }

    private IllegalStateException databaseError(String message, SQLException cause) {
        return new IllegalStateException(message, cause);
    }

    private record CompletionTarget(long internalUserId, UserState state, String lastDeliveryId, int experience,
                                    Long goalId, Long taskId, int xpReward, boolean custom) {}
}
