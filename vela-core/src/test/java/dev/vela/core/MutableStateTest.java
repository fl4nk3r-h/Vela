package dev.vela.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MutableStateTest {
    @Test
    void nullableValuesUseEqualityAndUpdateExceptionsDoNotMutateState() {
        VelaComponent owner = new VelaComponent() {
            @Override public ViewNode render() { return ViewNode.text("Owner"); }
        };
        MutableState<String> value = owner.state(null);
        assertFalse(value.set(null));
        assertFalse(owner.isDirty());
        assertTrue(value.set("before"));
        owner.consumeDirty();
        assertFalse(value.set(new String("before")));
        assertFalse(owner.isDirty());
        assertThrows(IllegalArgumentException.class, () -> value.update(previous -> {
            throw new IllegalArgumentException("Cannot update");
        }));
        assertEquals("before", value.get());
        assertFalse(owner.isDirty());
    }
}
