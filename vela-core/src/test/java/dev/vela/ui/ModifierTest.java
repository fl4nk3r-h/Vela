package dev.vela.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ModifierTest {
    @Test
    void chainsPropertiesWithoutMutatingTheOriginal() {
        Modifier base = Modifier.none().padding(12).background("#ffffff");
        Modifier interactive = base.then(Modifier.none().enabled(false).testTag("save"));

        assertFalse(base.properties().containsKey("state.enabled"));
        assertEquals("12dp", interactive.properties().get("style.padding"));
        assertEquals("false", interactive.properties().get("state.enabled"));
        assertEquals("save", interactive.properties().get("semantics.testTag"));
    }

    @Test
    void validatesPhysicalValues() {
        assertThrows(IllegalArgumentException.class, () -> Modifier.none().width(-1));
        assertThrows(IllegalArgumentException.class, () -> Modifier.none().opacity(1.1));
        assertThrows(IllegalArgumentException.class, () -> Modifier.none().aspectRatio(0));
    }
}
