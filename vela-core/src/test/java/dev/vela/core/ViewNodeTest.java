package dev.vela.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import dev.vela.ui.Modifier;

class ViewNodeTest {
    @Test
    void buildsAnImmutableKeyedNodeWithEvents() {
        AtomicReference<String> changedValue = new AtomicReference<>();
        ViewNode child = ViewNode.text("Child");
        ViewNode node = ViewNode.builder("field")
                .key("email")
                .prop("value", "before")
                .modifier(Modifier.none().fillMaxWidth().testTag("email-field"))
                .child(child)
                .on("change", event -> changedValue.set(event.valueAs(String.class)))
                .build();

        assertEquals("email", node.key());
        assertEquals("100%", node.props().get("style.width"));
        assertEquals("email-field", node.props().get("semantics.testTag"));
        assertEquals(child, node.children().getFirst());
        assertTrue(node.dispatch("change", "after"));
        assertEquals("after", changedValue.get());
        assertFalse(node.dispatch("missing"));
        assertThrows(UnsupportedOperationException.class, () -> node.props().put("new", "value"));
        assertThrows(UnsupportedOperationException.class, () -> node.children().add(child));

        ViewNode rekeyed = node.withKey("new-email");
        assertEquals("new-email", rekeyed.key());
        assertEquals("email", node.key());
        assertTrue(rekeyed.dispatch("change", "rekeyed"));
        assertEquals("rekeyed", changedValue.get());
    }
}
