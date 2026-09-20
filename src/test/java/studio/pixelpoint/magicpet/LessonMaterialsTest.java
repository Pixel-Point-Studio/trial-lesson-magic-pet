package studio.pixelpoint.magicpet;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LessonMaterialsTest {
    private final Path root = Path.of(System.getProperty("user.dir"));

    @Test
    void studentBotContainsNoInfrastructureImportsAndAtMostFourTodos() throws Exception {
        String source = Files.readString(root.resolve(
                "src/main/java/studio/pixelpoint/magicpet/lesson/StudentBot.java"));
        for (String forbidden : List.of("telegrambots", "java.sql", "java.net.http", "jackson", "infrastructure.")) {
            assertFalse(source.contains(forbidden), "Учебный файл содержит инфраструктуру: " + forbidden);
        }
        assertTrue(source.lines().filter(line -> line.contains("TODO")).count() <= 4);
    }

    @Test
    void everyRouteHasCompleteIssueAndSingleDefectPatch() throws Exception {
        for (String route : List.of("study", "sport", "blog")) {
            String issue = Files.readString(root.resolve("lesson/issues/" + route + ".md"));
            for (String heading : List.of("## Пользовательская история", "## Шаги воспроизведения",
                    "## Ожидаемое поведение", "## Фактическое поведение", "## Breakpoint",
                    "## Критерии готовности", "## Творческое изменение", "## Лестница подсказок")) {
                assertTrue(issue.contains(heading), route + " не содержит раздел " + heading);
            }
            String patch = Files.readString(root.resolve("lesson/patches/" + route + ".patch"));
            assertEquals(1, patch.lines().filter(line -> line.startsWith("+") && line.contains("TODO STUDENT")).count());
            assertTrue(patch.contains("StudentBot.java"));
        }
    }
}
