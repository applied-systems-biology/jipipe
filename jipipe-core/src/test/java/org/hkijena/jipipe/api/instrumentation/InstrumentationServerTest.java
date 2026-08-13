package org.hkijena.jipipe.api.instrumentation;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(10)
class InstrumentationServerTest {

    @Test
    void serverStartsAndAcceptsConnection() throws Exception {
        InstrumentationServer server = new InstrumentationServer(0);
        server.start();
        assertTrue(server.awaitStart(5, TimeUnit.SECONDS));

        int port = server.getPort();
        CountDownLatch connected = new CountDownLatch(1);
        AtomicReference<String> receivedMessage = new AtomicReference<>();

        WebSocketClient client = new WebSocketClient(new URI("ws://127.0.0.1:" + port)) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                connected.countDown();
            }

            @Override
            public void onMessage(String message) {
                receivedMessage.set(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
            }

            @Override
            public void onError(Exception ex) {
            }
        };
        client.connect();
        assertTrue(connected.await(5, TimeUnit.SECONDS));

        client.close();
        server.stop(0);
    }
}
