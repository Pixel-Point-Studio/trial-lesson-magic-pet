package studio.pixelpoint.magicpet.infrastructure.assets;

import studio.pixelpoint.magicpet.application.port.AssetCatalog;
import studio.pixelpoint.magicpet.domain.Scenario;

import java.nio.file.Files;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public final class FileAssetCatalog implements AssetCatalog {
    private static final byte[] PNG_SIGNATURE = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private final Path root;

    public FileAssetCatalog(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public Optional<Path> find(Scenario scenario, int level) {
        if (scenario == null) return Optional.empty();
        Path scenarioAsset = root.resolve(scenario.assetDirectory()).resolve("level-" + level + ".png");
        if (isReadablePng(scenarioAsset)) return Optional.of(scenarioAsset);

        Path fallback = root.resolve("common").resolve("fallback.png");
        return isReadablePng(fallback) ? Optional.of(fallback) : Optional.empty();
    }

    private boolean isReadablePng(Path path) {
        if (!path.startsWith(root) || !Files.isRegularFile(path) || !Files.isReadable(path)
                || !path.getFileName().toString().toLowerCase().endsWith(".png")) {
            return false;
        }
        try (var stream = Files.newInputStream(path)) {
            return Arrays.equals(PNG_SIGNATURE, stream.readNBytes(PNG_SIGNATURE.length));
        } catch (IOException error) {
            return false;
        }
    }
}
