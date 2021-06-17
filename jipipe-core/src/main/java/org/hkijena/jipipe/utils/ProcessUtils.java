/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Insitute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.jipipe.utils;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.Watchdog;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.BreadthFirstIterator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class ProcessUtils {

    private ProcessUtils() {

    }

    /**
     * Gets the process ID of a process
     *
     * @param p the process
     * @return the pid or -1 if it is not found
     */
    public static long getProcessID(Process p) {
        // Based on https://stackoverflow.com/a/43426878
        long result = -1;
        try {
            //for windows
            if (p.getClass().getName().equals("java.lang.Win32Process") ||
                    p.getClass().getName().equals("java.lang.ProcessImpl")) {
                Field f = p.getClass().getDeclaredField("handle");
                f.setAccessible(true);
                long handl = f.getLong(p);
                Kernel32 kernel = Kernel32.INSTANCE;
                WinNT.HANDLE hand = new WinNT.HANDLE();
                hand.setPointer(Pointer.createConstant(handl));
                result = kernel.GetProcessId(hand);
                f.setAccessible(false);
            }
            //for unix based operating systems
            else if (p.getClass().getName().equals("java.lang.UNIXProcess")) {
                Field f = p.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                result = f.getLong(p);
                f.setAccessible(false);
            }
        } catch (Exception ex) {
            result = -1;
        }
        return result;
    }

    /**
     * Queries standard output with a timeout
     *
     * @param executable
     * @param progressInfo
     * @param args
     * @return
     */
    public static String queryFast(Path executable, JIPipeProgressInfo progressInfo, String... args) {
        CommandLine commandLine = new CommandLine(executable.toFile());
        commandLine.addArguments(args);
        progressInfo.log("Running " + executable + " " + String.join(" ", args));
        ProcessUtils.ExtendedExecutor executor = new ProcessUtils.ExtendedExecutor(5000, progressInfo);

        // Capture stdout
        ByteArrayOutputStream standardOutputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorOutputStream = new ByteArrayOutputStream();
        PumpStreamHandler outputStreamHandler = new PumpStreamHandler(standardOutputStream, errorOutputStream);
        executor.setStreamHandler(outputStreamHandler);

        try {
            int exitValue = executor.execute(commandLine);

            if (!executor.isFailure(exitValue)) {
                return new String(standardOutputStream.toByteArray());
            } else {
                return null;
            }

        } catch (IOException e) {
            return null;
        }
    }

    public static void killProcessTree(long pid, JIPipeProgressInfo progressInfo) {
        if (pid != -1) {
            progressInfo.log("Cancelling process tree rooted at PID " + pid);
            if (SystemUtils.IS_OS_WINDOWS) {
                // Windows uses taskkill
                progressInfo.log(queryFast(Paths.get(System.getenv("WINDIR")).resolve("System32").resolve("taskkill"),
                        progressInfo,
                        "/F", "/PID", pid + "", "/T"));
            } else {
                // Unix provides pkill
                String psPath = StringUtils.nullToEmpty(ProcessUtils.queryFast(Paths.get("/usr/bin/which"), new JIPipeProgressInfo(), "ps")).trim();
                String killPath = StringUtils.nullToEmpty(ProcessUtils.queryFast(Paths.get("/usr/bin/which"), new JIPipeProgressInfo(), "kill")).trim();
                if (!StringUtils.isNullOrEmpty(psPath)) {
                    String psOutput = queryFast(Paths.get(psPath),
                            progressInfo,
                            "-A", "-o", "pid,ppid");
                    if (!StringUtils.isNullOrEmpty(psOutput)) {
                        psOutput = psOutput.trim();
                        DefaultDirectedGraph<Long, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
                        for (String line : psOutput.split("\n")) {
                            String line_ = line.trim();
                            if (line_.startsWith("P"))
                                continue;
                            String[] components = line_.split("\\s+");
                            long psPid = Long.parseLong(components[0]);
                            long psParentPid = Long.parseLong(components[1]);
                            if (!graph.containsVertex(psPid))
                                graph.addVertex(psPid);
                            if (psParentPid > 0) {
                                if (!graph.containsVertex(psParentPid))
                                    graph.addVertex(psParentPid);
                                graph.addEdge(psParentPid, psPid);
                            }
                        }

                        // List all children
                        BreadthFirstIterator<Long, DefaultEdge> breadthFirstIterator = new BreadthFirstIterator<>(graph, pid);
                        while (breadthFirstIterator.hasNext()) {
                            long toKill = breadthFirstIterator.next();
                            progressInfo.log("Killing orphaned PID " + toKill);
                            queryFast(Paths.get(killPath), progressInfo, "-9", toKill + "");
                        }
                    }
                } else {
                    progressInfo.log("Error: Could not find pkill.");
                }
            }
        } else {
            progressInfo.log("Error: PID is -1. Cannot cancel process tree.");
        }
    }

    /**
     * Wrapper around an existing process that models a process tree
     */
    public static class ProcessTree extends Process {
        private final Process process;
        private final long pid;
        private final JIPipeProgressInfo progressInfo;

        public ProcessTree(Process process, JIPipeProgressInfo progressInfo) {
            this.process = process;
            this.pid = getProcessID(process);
            this.progressInfo = progressInfo;
        }

        public Process getProcess() {
            return process;
        }

        public long getPid() {
            return pid;
        }


        @Override
        public OutputStream getOutputStream() {
            return process.getOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return process.getInputStream();
        }

        @Override
        public InputStream getErrorStream() {
            return process.getErrorStream();
        }

        @Override
        public int waitFor() throws InterruptedException {
            return process.waitFor();
        }

        @Override
        public int exitValue() {
            return process.exitValue();
        }

        @Override
        public void destroy() {
            killProcessTree(pid, progressInfo);
        }

        public JIPipeProgressInfo getProgressInfo() {
            return progressInfo;
        }
    }

    public static class ExtendedExecutor extends DefaultExecutor {

        private final JIPipeProgressInfo progressInfo;
        private ProcessTree process;

        public ExtendedExecutor(long timeout, JIPipeProgressInfo progressInfo) {
            super();
            this.progressInfo = progressInfo;
            setWatchdog(new RunCancellationExecuteWatchdog(timeout, progressInfo, this));
        }

        @Override
        protected Process launch(CommandLine command, Map<String, String> env, File dir) throws IOException {
            process = new ProcessTree(super.launch(command, env, dir), progressInfo);
            return process;
        }

        public long getPid() {
            return process.getPid();
        }

        public JIPipeProgressInfo getProgressInfo() {
            return progressInfo;
        }

        public ProcessTree getProcess() {
            return process;
        }
    }

    /**
     * Based on {@link ExecuteWatchdog}. Adapted to listed to {@link JIPipeProgressInfo} cancellation.
     */
    public static class RunCancellationExecuteWatchdog extends ExecuteWatchdog {

        private final JIPipeProgressInfo progressInfo;
        private final RunCancellationWatchdog cancellationWatchdog;
        private final ExtendedExecutor extendedExecutor;

        /**
         * Creates a new watchdog with a given timeout.
         *
         * @param timeout          the timeout for the process in milliseconds. It must be
         *                         greater than 0 or 'INFINITE_TIMEOUT'
         * @param extendedExecutor the executor
         */
        public RunCancellationExecuteWatchdog(long timeout, JIPipeProgressInfo progressInfo, ExtendedExecutor extendedExecutor) {
            super(timeout);
            this.progressInfo = progressInfo;
            this.cancellationWatchdog = new RunCancellationWatchdog(progressInfo);
            this.extendedExecutor = extendedExecutor;
            this.cancellationWatchdog.getEventBus().register(this);
        }

        @Override
        public synchronized void timeoutOccured(Watchdog w) {
            // Kill the process using the PID
            long pid = extendedExecutor.getPid();
            killProcessTree(pid, progressInfo);
            super.timeoutOccured(w);
        }

        @Subscribe
        public synchronized void onProcessCancelled(RunCancellationWatchdog.CancelledEvent event) {
            this.timeoutOccured(null);
        }

        @Override
        public synchronized void start(Process processToMonitor) {
            super.start(processToMonitor);
            cancellationWatchdog.start();
        }

        @Override
        public synchronized void stop() {
            cancellationWatchdog.stop();
            super.stop();
        }
    }

    /**
     * A watchdog that monitors a sub-process of a {@link org.hkijena.jipipe.api.JIPipeRunnable}
     * and watches for the {@link org.hkijena.jipipe.api.JIPipeRunnable} to be cancelled.
     * Based on {@link Watchdog}
     */
    public static class RunCancellationWatchdog implements Runnable {
        private final EventBus eventBus = new EventBus();
        private final JIPipeProgressInfo progressInfo;
        private boolean stopped = false;

        public RunCancellationWatchdog(JIPipeProgressInfo progressInfo) {
            this.progressInfo = progressInfo;
        }

        public synchronized void start() {
            stopped = false;
            final Thread t = new Thread(this, "WATCHDOG");
            t.setDaemon(true);
            t.start();
        }

        public synchronized void stop() {
            stopped = true;
            notifyAll();
        }

        public void run() {
            boolean isWaiting;
            synchronized (this) {
                isWaiting = true;
                while (!stopped && isWaiting) {
                    try {
                        wait(500);
                    } catch (final InterruptedException e) {
                    }
                    isWaiting = !progressInfo.isCancelled().get();
                }
            }

            // notify the listeners outside of the synchronized block (see EXEC-60)
            if (!isWaiting) {
                eventBus.post(new CancelledEvent(this));
            }
        }

        public EventBus getEventBus() {
            return eventBus;
        }

        public static class CancelledEvent {
            private final RunCancellationWatchdog watchdog;

            public CancelledEvent(RunCancellationWatchdog watchdog) {
                this.watchdog = watchdog;
            }

            public RunCancellationWatchdog getWatchdog() {
                return watchdog;
            }
        }
    }
}
