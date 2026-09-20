package studio.pixelpoint.magicpet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import studio.pixelpoint.magicpet.domain.Scenario;
import studio.pixelpoint.magicpet.infrastructure.assets.FileAssetCatalog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class FileAssetCatalogTest {
    private static final byte[] PNG = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3
    };

    @TempDir Path root;

    @ParameterizedTest
    @MethodSource("scenarioLevels")
    void resolvesEveryScenarioAndLevelFromStrictPath(Scenario scenario, int level) throws IOException {
        Path expected = root.resolve(scenario.assetDirectory()).resolve("level-" + level + ".png");
        Files.createDirectories(expected.getParent());
        Files.write(expected, PNG);

        assertEquals(expected.toAbsolutePath().normalize(), new FileAssetCatalog(root).find(scenario, level).orElseThrow());
    }

    @Test
    void damagedAssetUsesCommonFallback() throws IOException {
        Path damaged = root.resolve("sport/level-1.png");
        Path fallback = root.resolve("common/fallback.png");
        Files.createDirectories(damaged.getParent());
        Files.createDirectories(fallback.getParent());
        Files.writeString(damaged, "not a png");
        Files.write(fallback, PNG);

        assertEquals(fallback.toAbsolutePath().normalize(),
                new FileAssetCatalog(root).find(Scenario.SPORT, 1).orElseThrow());
    }

    @Test
    void missingAssetAndFallbackProduceTextMode() {
        assertTrue(new FileAssetCatalog(root).find(Scenario.BLOG, 2).isEmpty());
    }

    private static Stream<Arguments> scenarioLevels() {
        return Stream.of(Scenario.values())
                .flatMap(scenario -> Stream.of(1, 2, 3).map(level -> Arguments.of(scenario, level)));
    }
}
