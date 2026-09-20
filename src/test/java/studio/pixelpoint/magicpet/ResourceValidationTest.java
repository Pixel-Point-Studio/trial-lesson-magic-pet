package studio.pixelpoint.magicpet;

import org.junit.jupiter.api.Test;
import studio.pixelpoint.magicpet.application.PromptResource;
import studio.pixelpoint.magicpet.application.UiTexts;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ResourceValidationTest {
    @Test void uiTextsContainRequiredKeys() {
        assertDoesNotThrow(UiTexts::load);
    }

    @Test void promptContainsRequiredContract() {
        assertDoesNotThrow(PromptResource::validate);
    }
}
