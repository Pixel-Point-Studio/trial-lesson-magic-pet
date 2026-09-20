package studio.pixelpoint.magicpet.infrastructure.assets;

import studio.pixelpoint.magicpet.application.port.AssetCatalog;
import studio.pixelpoint.magicpet.domain.Scenario;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class FileAssetCatalog implements AssetCatalog {
    private final Path root;

    public FileAssetCatalog(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public Optional<Path> find(Scenario scenario, int level) {
        if (scenario == null) return Optional.empty();
        Path scenarioAsset = root.resolve(scenario.assetDirectory()).resolve("level-" + level + ".png");
        if (isReadablePng(scenarioAsset)) return Optional.of(scenarioAsset);

        // The initial repository ships neutral Runa art; scenario-specific assets can replace it later.
        Path bundledRuna = root.resolve("runa-" + Math.max(0, level - 1) + ".png");
        return isReadablePng(bundledRuna) ? Optional.of(bundledRuna) : Optional.empty();
    }

    private boolean isReadablePng(Path path) {
        return path.startsWith(root) && Files.isRegularFile(path) && Files.isReadable(path)
                && path.getFileName().toString().toLowerCase().endsWith(".png");
    }
}
