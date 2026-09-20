package studio.pixelpoint.magicpet.domain;

public enum Scenario {
    STUDY("study"), SPORT("sport"), BLOG("blog");

    private final String assetDirectory;

    Scenario(String assetDirectory) {
        this.assetDirectory = assetDirectory;
    }

    public String assetDirectory() {
        return assetDirectory;
    }
}
