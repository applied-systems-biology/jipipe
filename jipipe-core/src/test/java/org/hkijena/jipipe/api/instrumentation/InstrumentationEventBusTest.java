package org.hkijena.jipipe.api.instrumentation;

import org.hkijena.jipipe.api.instrumentation.events.NodeAddedEvent;
import org.hkijena.jipipe.api.instrumentation.events.ParameterChangedEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InstrumentationEventBusTest {

    @Test
    void subscribeAndPublish_deliversToMatchingListener() {
        InstrumentationEventBus bus = new InstrumentationEventBus();
        List<NodeAddedEvent> received = new ArrayList<>();

        bus.subscribe(NodeAddedEvent.class, received::add);

        NodeAddedEvent event = new NodeAddedEvent("project-1", "compartment-1", "node-1", "import-image", "Import image");
        bus.publish(event);

        assertEquals(1, received.size());
        assertEquals("node-1", received.get(0).getNodeId());
    }

    @Test
    void subscribeAndPublish_doesNotDeliverToOtherType() {
        InstrumentationEventBus bus = new InstrumentationEventBus();
        List<NodeAddedEvent> nodeEvents = new ArrayList<>();
        List<ParameterChangedEvent> paramEvents = new ArrayList<>();

        bus.subscribe(NodeAddedEvent.class, nodeEvents::add);
        bus.subscribe(ParameterChangedEvent.class, paramEvents::add);

        bus.publish(new NodeAddedEvent("p1", "c1", "n1", "t1", "Name"));

        assertEquals(1, nodeEvents.size());
        assertTrue(paramEvents.isEmpty());
    }

    @Test
    void publish_nullEvent_doesNothing() {
        InstrumentationEventBus bus = new InstrumentationEventBus();
        assertDoesNotThrow(() -> bus.publish(null));
    }
}
