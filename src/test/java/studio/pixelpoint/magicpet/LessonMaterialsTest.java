package studio.pixelpoint.magicpet;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LessonMaterialsTest {
    private final Path root = Path.of(System.getProperty("user.dir"));

    @Test
    void studentSurfaceContainsNoInfrastructureImportsAndRouteFilesStaySmall() throws Exception {
        for (String file : List.of("StudentBot.java", "route/StudyLesson.java", "route/SportLesson.java", "route/BlogLesson.java")) {
            String source = Files.readString(root.resolve("src/main/java/studio/pixelpoint/magicpet/lesson/" + file));
            for (String forbidden : List.of("telegrambots", "java.sql", "java.net.http", "jackson", "infrastructure.")) {
                assertFalse(source.contains(forbidden), file + " содержит инфраструктуру: " + forbidden);
            }
            assertTrue(source.lines().count() <= 150, file + " должен оставаться небольшим");
        }
    }

    @Test
    void workspaceContainsAllIssuesAndCrossPlatformPreparation() throws Exception {
        String lessonReadme = Files.readString(root.resolve("lesson/README.lesson.md"));
        assertEquals(12, lessonReadme.lines()
                .filter(line -> line.contains("github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/"))
                .count());
        assertTrue(lessonReadme.contains("смешать темы"));

        String tasks = Files.readString(root.resolve(".vscode/tasks.json"));
        assertTrue(tasks.contains("Magic Pet: подготовить пробный урок"));
        assertTrue(tasks.contains("gradlew.bat"));

        String build = Files.readString(root.resolve("build.gradle"));
        assertTrue(build.contains("prepareLesson"));
        assertFalse(Files.exists(root.resolve("НАЧАТЬ УРОК.app")));
    }
}
