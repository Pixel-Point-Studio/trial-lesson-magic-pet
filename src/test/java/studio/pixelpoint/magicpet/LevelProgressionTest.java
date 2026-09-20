package studio.pixelpoint.magicpet;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import studio.pixelpoint.magicpet.domain.LevelProgression;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LevelProgressionTest {
    @ParameterizedTest
    @CsvSource({"0,1", "99,1", "100,2", "249,2", "250,3", "1000,3"})
    void calculatesThreeLevels(int xp, int expectedLevel) {
        assertEquals(expectedLevel, LevelProgression.levelFor(xp));
    }
}
