/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.servers;

import org.hkijena.jipipe.api.JIPipeProgressInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Spawns and monitors child processes for server instances.
 * 
 * <p>Handles:</p>
 * <ul>
 *     <li>Process spawning with environment variables and arguments</li>
 *     <li>Stdout/stderr capture for logging</li>
 *     <li>Graceful shutdown (SIGTERM) followed by force-kill</li>
 *     <li>Process monitoring and crash detection</li>
 * </ul>
 */
public class ProcessSupervisor {
    private final Map<Process, ProcessInfo> managedProcesses = new ConcurrentHashMap<>();
    private long shutdownTimeoutMs = 10000; // 10 seconds default

    /**
     * Information about a managed process.
     */
    public static class ProcessInfo {
        private final Process process;
        private final List<String> command;
        private final Map<String, String> environment;
        private final String displayName;
        private final JIPipeProgressInfo progressInfo;
        private volatile boolean alive = true;
        private final List<String> stdoutLines = Collections.synchronizedList(new ArrayList<>());
        private final List<String> stderrLines = Collections.synchronizedList(new ArrayList<>());

        /**
         * @param process     the managed process
         * @param command     the command and arguments used to spawn the process
         * @param environment additional environment variables
         * @param displayName a human-readable name for logging
         * @param progressInfo the progress info for logging
         */
        public ProcessInfo(Process process, List<String> command, Map<String, String> environment,
                           String displayName, JIPipeProgressInfo progressInfo) {
            this.process = process;
            this.command = command;
            this.environment = environment;
            this.displayName = displayName;
            this.progressInfo = progressInfo;
        }

        /**
         * @return the managed process
         */
        public Process getProcess() { return process; }

        /**
         * @return the command and arguments used to spawn the process
         */
        public List<String> getCommand() { return command; }

        /**
         * @return additional environment variables
         */
        public Map<String, String> getEnvironment() { return environment; }

        /**
         * @return a human-readable name for logging
         */
        public String getDisplayName() { return displayName; }

        /**
         * @return the progress info for logging
         */
        public JIPipeProgressInfo getProgressInfo() { return progressInfo; }

        /**
         * @return true if the process is alive
         */
        public boolean isAlive() { return alive && process.isAlive(); }

        /**
         * @return unmodifiable list of captured stdout lines
         */
        public List<String> getStdoutLines() { return Collections.unmodifiableList(stdoutLines); }

        /**
         * @return unmodifiable list of captured stderr lines
         */
        public List<String> getStderrLines() { return Collections.unmodifiableList(stderrLines); }
    }

    /**
     * Spawns a process with the given command and environment.
     * Stdout and stderr are captured and logged.
     *
     * @param command      the command and arguments
     * @param environment  additional environment variables (merged with system environment)
     * @param displayName  a human-readable name for logging
     * @param progressInfo the progress info for logging
     * @return the spawned process
     * @throws IOException if the process cannot be started
     */
    public Process spawnProcess(List<String> command, Map<String, String> environment,
                                 String displayName, JIPipeProgressInfo progressInfo) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        
        // Merge environment variables
        Map<String, String> processEnv = pb.environment();
        processEnv.putAll(environment);
        
        pb.redirectErrorStream(false);
        
        progressInfo.log("Starting process: " + String.join(" ", command));
        Process process = pb.start();
        
        ProcessInfo info = new ProcessInfo(process, command, environment, displayName, progressInfo);
        managedProcesses.put(process, info);
        
        // Start stdout reader thread
        Thread stdoutReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    info.stdoutLines.add(line);
                    progressInfo.log("[" + displayName + " stdout] " + line);
                }
            } catch (IOException e) {
                // Process terminated, this is expected
            }
        }, displayName + "-stdout-reader");
        stdoutReader.setDaemon(true);
        stdoutReader.start();
        
        // Start stderr reader thread
        Thread stderrReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    info.stderrLines.add(line);
                    progressInfo.log("[" + displayName + " stderr] " + line);
                }
            } catch (IOException e) {
                // Process terminated, this is expected
            }
        }, displayName + "-stderr-reader");
        stderrReader.setDaemon(true);
        stderrReader.start();
        
        return process;
    }

    /**
     * Stops a managed process gracefully. First attempts SIGTERM (via Process.destroy()),
     * then force-kills after the shutdown timeout.
     *
     * @param process     the process to stop
     * @param progressInfo the progress info for logging
     */
    public void stopProcess(Process process, JIPipeProgressInfo progressInfo) {
        ProcessInfo info = managedProcesses.get(process);
        String name = info != null ? info.getDisplayName() : "unknown";
        
        if (!process.isAlive()) {
            progressInfo.log("Process '" + name + "' is already terminated");
            if (info != null) info.alive = false;
            managedProcesses.remove(process);
            return;
        }
        
        // Attempt graceful shutdown
        progressInfo.log("Stopping process '" + name + "' gracefully ...");
        process.destroy();
        
        try {
            if (!process.waitFor(shutdownTimeoutMs, TimeUnit.MILLISECONDS)) {
                progressInfo.log("Process '" + name + "' did not terminate within " + shutdownTimeoutMs + "ms, force-killing ...");
                process.destroyForcibly();
                if (!process.waitFor(5000, TimeUnit.MILLISECONDS)) {
                    progressInfo.log("WARNING: Process '" + name + "' could not be terminated!");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
        
        if (info != null) info.alive = false;
        managedProcesses.remove(process);
        progressInfo.log("Process '" + name + "' stopped");
    }

    /**
     * Returns the process info for a managed process.
     *
     * @param process the process
     * @return the process info, or null if not managed
     */
    public ProcessInfo getProcessInfo(Process process) {
        return managedProcesses.get(process);
    }

    /**
     * Sets the shutdown timeout in milliseconds.
     *
     * @param shutdownTimeoutMs the timeout in milliseconds
     */
    public void setShutdownTimeoutMs(long shutdownTimeoutMs) {
        this.shutdownTimeoutMs = shutdownTimeoutMs;
    }

    /**
     * @return the shutdown timeout in milliseconds
     */
    public long getShutdownTimeoutMs() {
        return shutdownTimeoutMs;
    }

    /**
     * Stops all managed processes. Best-effort cleanup.
     */
    public void stopAll() {
        for (Map.Entry<Process, ProcessInfo> entry : managedProcesses.entrySet()) {
            try {
                stopProcess(entry.getKey(), entry.getValue().getProgressInfo());
            } catch (Exception e) {
                // Best effort
            }
        }
        managedProcesses.clear();
    }
}
