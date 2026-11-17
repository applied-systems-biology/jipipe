package org.hkijena.jipipe.launcher.commands;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the pure {@code --mcp} command line parsing (no Swing involved).
 */
public class McpCliOptionsTest {

    @Test
    public void mcpAloneRequestsDefaultPort() {
        McpCliOptions options = GuiCommand.parseMcpArgs(List.of("--mcp"));
        assertTrue(options.requested());
        assertNull(options.port());
        assertTrue(options.allowIds().isEmpty());
        assertTrue(options.denyIds().isEmpty());
    }

    @Test
    public void mcpWithPortConsumesPort() {
        McpCliOptions options = GuiCommand.parseMcpArgs(List.of("--mcp", "9000"));
        assertTrue(options.requested());
        assertEquals(9000, options.port());
    }

    @Test
    public void mcpDoesNotSwallowNonNumericArgument() {
        // The next argument is not a port: it must stay available (e.g. as project path).
        McpCliOptions options = GuiCommand.parseMcpArgs(List.of("--mcp", "project.jipipe"));
        assertTrue(options.requested());
        assertNull(options.port());
    }

    @Test
    public void permissionsAllowAndDeny() {
        // Permission overrides alone do not request MCP startup (that is --mcp or autoStart).
        McpCliOptions options = GuiCommand.parseMcpArgs(List.of("--mcp-permissions", "allow=a,b", "deny=c"));
        assertFalse(options.requested());
        assertEquals(List.of("a", "b"), options.allowIds());
        assertEquals(List.of("c"), options.denyIds());
    }

    @Test
    public void repeatedFlagsAccumulate() {
        McpCliOptions options = GuiCommand.parseMcpArgs(List.of(
                "--mcp-permissions", "allow=a",
                "--mcp-permissions", "allow=b,  ",
                "--mcp-permissions", "deny=x",
                "--mcp-permissions", "deny=y"));
        assertEquals(List.of("a", "b"), options.allowIds());
        assertEquals(List.of("x", "y"), options.denyIds());
    }

    @Test
    public void unknownPermissionKeysAreIgnored() {
        McpCliOptions options = GuiCommand.parseMcpArgs(List.of("--mcp-permissions", "hide=a", "weird=b,c"));
        assertTrue(options.allowIds().isEmpty());
        assertTrue(options.denyIds().isEmpty());
    }

    @Test
    public void combinedFlags() {
        McpCliOptions options = GuiCommand.parseMcpArgs(List.of(
                "--mcp", "9000", "--mcp-permissions", "allow=a", "deny=b"));
        assertTrue(options.requested());
        assertEquals(9000, options.port());
        assertEquals(List.of("a"), options.allowIds());
        assertEquals(List.of("b"), options.denyIds());
    }

    @Test
    public void nonMcpArgumentsAreNotRequested() {
        McpCliOptions options = GuiCommand.parseMcpArgs(List.of("--instrumentation", "8780", "project.jipipe"));
        assertFalse(options.requested());
        assertNull(options.port());
        assertTrue(options.allowIds().isEmpty());
        assertTrue(options.denyIds().isEmpty());
    }

    @Test
    public void consumedCountMarksOnlyMcpArguments() {
        List<String> args = List.of("a", "--mcp", "9000", "b", "--mcp-permissions", "allow=x,y", "deny=z", "c");
        assertEquals(0, McpCliOptions.consumedCount(args, 0));
        assertEquals(2, McpCliOptions.consumedCount(args, 1));
        assertEquals(0, McpCliOptions.consumedCount(args, 3));
        assertEquals(3, McpCliOptions.consumedCount(args, 4));
        assertEquals(0, McpCliOptions.consumedCount(args, 7));
    }
}
