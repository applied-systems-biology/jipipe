package org.hkijena.jipipe.servers.mcp;

import org.hkijena.jipipe.api.servers.PortManager;
import org.hkijena.jipipe.api.servers.SharedInstanceRegistry;
import org.hkijena.jipipe.api.servers.SharedServerProcess;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Standalone MCP broker process: parses CLI arguments, resolves the final listen port, and
 * blocks in {@link #run()} (discovery file write, parent and instance PID monitors).
 * <p>
 * Arguments: {@code --discovery-file <path>} (required), {@code --parent-pid <long>},
 * {@code --port <int>} (0 or occupied: fall back to {@link PortManager#assignFreePort()}
 * with a loud stderr line and a ring buffer note), and {@code --token-file <path>}
 * (optional; the trimmed file content is the bearer token, read at startup).
 * <p>
 * The final port is resolved in {@link #main(String[])} BEFORE the process is constructed:
 * {@link SharedServerProcess#run()} writes the discovery file with the constructor port, so
 * that port must already be the port the broker actually binds.
 */
public class MCPServerProcess extends SharedServerProcess {

    private final MCPInstanceRegistry registry;
    private final String bearerToken;
    private final List<String> pendingLogNotes = new ArrayList<>();
    private MCPBrokerServer broker;

    public MCPServerProcess(Path discoveryFile, long parentPid, int port,
                            MCPInstanceRegistry registry, String bearerToken) {
        super(discoveryFile, parentPid, port, registry);
        this.registry = registry;
        this.bearerToken = bearerToken;
    }

    @Override
    protected void startServer() throws Exception {
        broker = new MCPBrokerServer(getPort(), registry, bearerToken);
        broker.start();
        for (String note : pendingLogNotes) {
            broker.getLogs().add("info", note);
        }
        pendingLogNotes.clear();
    }

    @Override
    protected void stopServer() {
        if (broker != null) {
            broker.stop();
            broker = null;
        }
    }

    @Override
    protected SharedInstanceRegistry<?> getRegistry() {
        return registry;
    }

    public MCPBrokerServer getBroker() {
        return broker;
    }

    public static void main(String[] args) throws Exception {
        Path discoveryFile = null;
        long parentPid = 0;
        int configuredPort = 0;
        Path tokenFile = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--discovery-file" -> discoveryFile = Path.of(args[++i]);
                case "--parent-pid" -> parentPid = Long.parseLong(args[++i]);
                case "--port" -> configuredPort = Integer.parseInt(args[++i]);
                case "--token-file" -> tokenFile = Path.of(args[++i]);
                default -> throw new IllegalArgumentException("Unknown argument: " + args[i]);
            }
        }
        Objects.requireNonNull(discoveryFile, "--discovery-file is required");

        String bearerToken = null;
        if (tokenFile != null) {
            bearerToken = Files.readString(tokenFile).trim();
            if (bearerToken.isEmpty()) {
                bearerToken = null;
            }
        }

        PortManager portManager = new PortManager();
        int port;
        if (configuredPort <= 0) {
            port = portManager.assignFreePort();
            System.err.println("[jipipe-mcp] No port configured; assigned free port " + port);
        } else if (!portManager.isPortAvailable(configuredPort)) {
            port = portManager.assignFreePort();
            System.err.println("[jipipe-mcp] Configured port " + configuredPort
                    + " is occupied; falling back to port " + port);
        } else {
            port = configuredPort;
        }

        MCPInstanceRegistry registry = new MCPInstanceRegistry();
        MCPServerProcess process = new MCPServerProcess(discoveryFile, parentPid, port, registry, bearerToken);
        if (configuredPort <= 0) {
            process.pendingLogNotes.add("Spawn-time port assignment: no port configured, using " + port);
        } else if (configuredPort != port) {
            process.pendingLogNotes.add("Spawn-time port fallback: configured port " + configuredPort
                    + " was occupied, using " + port);
        }
        process.run();
    }
}
