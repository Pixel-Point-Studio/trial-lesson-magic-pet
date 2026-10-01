package studio.pixelpoint.magicpet;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LessonMaterialsTest {
    private final Path root = Path.of(System.getProperty("user.dir"));

    @Test
    void studentSurfaceContainsNoInfrastructureImportsAndStaysSmall() throws Exception {
        for (String file : List.of("PetFeatures.java", "StudentBot.java",
                "route/StudyLesson.java", "route/SportLesson.java", "route/BlogLesson.java")) {
            String source = Files.readString(root.resolve("src/main/java/studio/pixelpoint/magicpet/lesson/" + file));
            for (String forbidden : List.of("telegrambots", "java.sql", "java.net.http", "jackson", "infrastructure.")) {
                assertFalse(source.contains(forbidden), file + " содержит инфраструктуру: " + forbidden);
            }
            int limit = file.equals("PetFeatures.java") ? 110 : file.contains("route/") ? 30 : 150;
            assertTrue(source.lines().count() <= limit, file + " должен оставаться небольшим");
        }
        String features = Files.readString(root.resolve(
                "src/main/java/studio/pixelpoint/magicpet/lesson/PetFeatures.java"));
        assertTrue(features.contains("true означает"));
        assertTrue(features.contains("скрытое имя действия"));
    }

    @Test
    void workspaceContainsAllIssuesAndCrossPlatformPreparation() throws Exception {
        String lessonReadme = Files.readString(root.resolve("lesson/README.lesson.md"));
        assertEquals(4, lessonReadme.lines()
                .filter(line -> line.contains("github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/"))
                .count());
        assertTrue(lessonReadme.contains("Код заданий одинаков для всех трёх Pet"));
        assertTrue(lessonReadme.contains("PetFeatures.java"));

        String tasks = Files.readString(root.resolve(".vscode/tasks.json"));
        assertTrue(tasks.contains("Magic Pet: подготовить пробный урок"));
        assertTrue(tasks.contains("gradlew.bat"));

        String build = Files.readString(root.resolve("build.gradle"));
        assertTrue(build.contains("prepareLesson"));
        assertFalse(Files.exists(root.resolve("НАЧАТЬ УРОК.app")));
    }
}
