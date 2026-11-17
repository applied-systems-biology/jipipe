package org.hkijena.jipipe.servers.mcp;

import org.hkijena.jipipe.api.automation.endpoint.MCPInstanceRegistration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests the {@link MCPInstanceRegistry} registration order: most recently registered first
 * for registry-global tool routing, and pruning of dead instances swept by
 * {@link MCPInstanceRegistry#removeDeadInstances()} (the sweep bypasses
 * {@link MCPInstanceRegistry#unregister}, so the order list must be pruned explicitly).
 */
public class MCPInstanceRegistryTest {

    private static MCPInstanceRegistration registration(String instanceId, long pid, String... toolIds) {
        MCPInstanceRegistration registration = new MCPInstanceRegistration();
        registration.setInstanceId(instanceId);
        registration.setJipipePid(pid);
        registration.setEndpointPort(1);
        registration.setEndpointSecret("secret");
        registration.setAppVersion("test");
        registration.setToolIds(List.of(toolIds));
        return registration;
    }

    /**
     * A pid that is guaranteed to be dead: a finished helper process.
     */
    private static long deadPid() throws IOException, InterruptedException {
        Process process = new ProcessBuilder("true").start();
        process.waitFor();
        assertNotEquals(ProcessHandle.current().pid(), process.pid());
        return process.pid();
    }

    @Test
    void globalToolRoutingPrefersTheMostRecentlyRegisteredInstance() {
        MCPInstanceRegistry registry = new MCPInstanceRegistry();
        registry.register(registration("a", ProcessHandle.current().pid(), "get_node_docs"));
        registry.register(registration("b", ProcessHandle.current().pid(), "get_node_docs"));

        assertSame(registry.get("b"), registry.findAnyByToolId("get_node_docs"),
                "the most recently registered live instance wins");
    }

    @Test
    void unregisterRemovesTheInstanceFromGlobalToolRouting() {
        MCPInstanceRegistry registry = new MCPInstanceRegistry();
        registry.register(registration("a", ProcessHandle.current().pid(), "get_node_docs"));
        registry.register(registration("b", ProcessHandle.current().pid(), "get_node_docs"));

        registry.unregister("b");

        assertSame(registry.get("a"), registry.findAnyByToolId("get_node_docs"));
    }

    @Test
    void deadInstanceSweepRemovesDeadRegistrationsAndKeepsRoutingAlive() throws Exception {
        MCPInstanceRegistry registry = new MCPInstanceRegistry();
        registry.register(registration("a", ProcessHandle.current().pid(), "get_node_docs"));
        registry.register(registration("b", deadPid(), "get_node_docs"));

        // The most recently registered instance died: the sweep removes it and the global
        // tool routes to the surviving (older) instance, never to a stale order entry.
        assertEquals(1, registry.removeDeadInstances());
        assertNull(registry.get("b"));
        assertNotNull(registry.get("a"));
        assertSame(registry.get("a"), registry.findAnyByToolId("get_node_docs"));

        // The sweep is idempotent.
        assertEquals(0, registry.removeDeadInstances());
    }

    @Test
    void deadInstanceSweepKeepsLiveRegistrationsUntouched() throws Exception {
        MCPInstanceRegistry registry = new MCPInstanceRegistry();
        registry.register(registration("a", deadPid(), "get_node_docs"));
        registry.register(registration("b", ProcessHandle.current().pid(), "get_node_docs"));

        assertEquals(1, registry.removeDeadInstances());
        assertNull(registry.get("a"));
        assertSame(registry.get("b"), registry.findAnyByToolId("get_node_docs"));
    }
}
