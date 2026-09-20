package studio.pixelpoint.magicpet.application.port;

import studio.pixelpoint.magicpet.domain.Scenario;

import java.nio.file.Path;
import java.util.Optional;

public interface AssetCatalog {
    Optional<Path> find(Scenario scenario, int level);
}
