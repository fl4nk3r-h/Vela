package dev.vela.dev;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import org.junit.jupiter.api.Test;

class ReloadProtocolTest {
    @Test void payloadAndResponseRoundTrip() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        var request = new ReloadProtocol.Request(ReloadProtocol.Operation.CODE, "example.Main", new byte[]{1, 2}, new byte[]{3}, java.util.List.of("example.Main"));
        ReloadProtocol.writeRequest(new DataOutputStream(bytes), "secret", request);
        var decoded = ReloadProtocol.readRequest(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())), "secret");
        assertEquals(request.operation(), decoded.operation()); assertEquals("example.Main", decoded.entry());
        assertArrayEquals(request.dex(), decoded.dex()); assertArrayEquals(request.resources(), decoded.resources());
        assertEquals(request.classes(), decoded.classes());
        bytes.reset(); ReloadProtocol.writeResponse(new DataOutputStream(bytes), new ReloadProtocol.Response(false, "compile failed"));
        assertEquals(new ReloadProtocol.Response(false, "compile failed"), ReloadProtocol.readResponse(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));
    }
    @Test void rejectsUnauthenticatedTruncatedAndInvalidLengthRequests() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ReloadProtocol.writeRequest(new DataOutputStream(bytes), "secret", new ReloadProtocol.Request(ReloadProtocol.Operation.PING, "", new byte[0], new byte[0]));
        assertThrows(IOException.class, () -> ReloadProtocol.readRequest(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())), "wrong"));
        assertThrows(IOException.class, () -> ReloadProtocol.readRequest(new DataInputStream(new ByteArrayInputStream(new byte[2])), "secret"));
        bytes.reset(); var out = new DataOutputStream(bytes);
        out.writeInt(0x56454c41); out.writeInt(2); out.writeUTF("secret"); out.writeUTF("CODE"); out.writeUTF("example.Main"); out.writeInt(0); out.writeInt(-1);
        assertThrows(IOException.class, () -> ReloadProtocol.readRequest(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())), "secret"));
    }
    @Test void overlappingFrameworkAndDependencyPackagesStayShared() {
        var classes = new ReloadClasses(java.util.List.of("dev.vela.MainScreen", "dev.vela.Values"));
        assertTrue(classes.owns("dev.vela.MainScreen")); assertTrue(classes.owns("dev.vela.MainScreen$$ExternalSyntheticLambda0"));
        assertTrue(classes.owns("dev.vela.MainScreen$Child")); assertFalse(classes.owns("dev.vela.core.VelaComponent"));
        assertFalse(classes.owns("dev.vela.dependencies.Service")); assertFalse(classes.owns("dev.vela.R$string"));
    }
}
