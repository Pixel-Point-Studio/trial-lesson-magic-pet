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
    void everyRouteHasFourCompleteIssuesAndBundlePatch() throws Exception {
        for (String route : List.of("study", "sport", "blog")) {
            try (var issues = Files.list(root.resolve("lesson/issues/" + route))) {
                List<Path> cards = issues.filter(path -> path.toString().endsWith(".md")).sorted().toList();
                assertEquals(4, cards.size(), route + " должен содержать четыре карточки");
                for (Path card : cards) {
                    String issue = Files.readString(card);
                    for (String heading : List.of("## Что происходит сейчас", "## Как должно работать",
                            "## Как воспроизвести", "## С чего начать", "## Что сделать",
                            "## Подсказки", "## Готово, если")) {
                        assertTrue(issue.contains(heading), card + " не содержит раздел " + heading);
                    }
                }
            }
            String patch = Files.readString(root.resolve("lesson/patches/" + route + ".patch"));
            assertEquals(4, patch.lines().filter(line -> line.startsWith("+") && line.contains("TODO STUDENT")).count());
            assertTrue(patch.contains("Lesson.java") || patch.contains("LevelProgression.java"));
        }
    }
}
