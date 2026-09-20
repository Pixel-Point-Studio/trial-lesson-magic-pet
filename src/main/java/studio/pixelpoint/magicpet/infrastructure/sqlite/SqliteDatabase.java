package studio.pixelpoint.magicpet.infrastructure.sqlite;

import org.flywaydb.core.Flyway;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SqliteDatabase {
    private SqliteDatabase() {}

    public static String migrate(Path databasePath) {
        Path absolute = databasePath.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        try {
            if (parent != null) Files.createDirectories(parent);
        } catch (IOException error) {
            throw new IllegalStateException("Не удалось создать каталог базы данных", error);
        }
        String jdbcUrl = "jdbc:sqlite:" + absolute;
        Flyway.configure()
                .dataSource(jdbcUrl, null, null)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        return jdbcUrl;
    }
}
