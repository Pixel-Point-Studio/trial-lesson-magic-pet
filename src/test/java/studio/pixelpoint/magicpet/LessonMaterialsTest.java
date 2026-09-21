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
    void everyRouteHasBundlePatchAndGitHubIssueLinks() throws Exception {
        String preparationScript = Files.readString(root.resolve("scripts/prepare-lesson.sh"));
        for (String route : List.of("study", "sport", "blog")) {
            String patch = Files.readString(root.resolve("lesson/patches/" + route + ".patch"));
            assertEquals(4, patch.lines().filter(line -> line.startsWith("+") && line.contains("TODO STUDENT")).count());
            assertTrue(patch.contains("Lesson.java") || patch.contains("LevelProgression.java"));
        }
        assertEquals(12, preparationScript.lines()
                .filter(line -> line.contains("github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/"))
                .count());
    }
}
