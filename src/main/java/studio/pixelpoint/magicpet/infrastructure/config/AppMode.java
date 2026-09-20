package studio.pixelpoint.magicpet.infrastructure.config;

public enum AppMode {
    LESSON,
    PRODUCTION;

    static AppMode parse(String value) {
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (RuntimeException error) {
            throw new AppConfig.ConfigurationException("APP_MODE должен быть lesson или production");
        }
    }
}
