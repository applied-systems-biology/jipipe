package org.hkijena.jipipe.api.servers;

import java.nio.file.Path;
import java.time.Instant;

public abstract class SharedServerProcess {

    private final Path discoveryFile;
    private final long parentPid;
    private final int port;
    private final SharedInstanceRegistry<?> registry;
    private volatile boolean hasEverHadInstances = false;

    protected SharedServerProcess(Path discoveryFile, long parentPid, int port, SharedInstanceRegistry<?> registry) {
        this.discoveryFile = discoveryFile;
        this.parentPid = parentPid;
        this.port = port;
        this.registry = registry;
    }

    protected Path getDiscoveryFile() {
        return discoveryFile;
    }

    protected long getParentPid() {
        return parentPid;
    }

    protected int getPort() {
        return port;
    }

    protected abstract void startServer() throws Exception;

    protected abstract void stopServer();

    protected abstract SharedInstanceRegistry<?> getRegistry();

    public void run() throws Exception {
        startServer();

        long currentPid = ProcessHandle.current().pid();
        String startedAt = Instant.now().toString();
        SharedServerDiscoveryFile.write(discoveryFile, currentPid, port, startedAt);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stopServer();
            SharedServerDiscoveryFile.delete(discoveryFile);
        }, "Shared-Server-Shutdown-Hook"));

        if (parentPid > 0) {
            startParentMonitor();
        }

        startInstancePidMonitor();

        Thread.currentThread().join();
    }

    private void startParentMonitor() {
        Thread monitor = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    break;
                }
                if (!SharedServerDiscoveryFile.isPidAlive(parentPid)) {
                    if (registry.getInstanceCount() == 0) {
                        System.exit(0);
                    }
                }
            }
        }, "Parent-PID-Monitor");
        monitor.setDaemon(true);
        monitor.start();
    }

    private void startInstancePidMonitor() {
        Thread monitor = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    break;
                }
                registry.removeDeadInstances();
                int count = registry.getInstanceCount();
                if (count > 0) {
                    hasEverHadInstances = true;
                }
                if (count == 0 && hasEverHadInstances) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        break;
                    }
                    if (registry.getInstanceCount() == 0) {
                        System.exit(0);
                    }
                }
            }
        }, "Instance-PID-Monitor");
        monitor.setDaemon(true);
        monitor.start();
    }
}
