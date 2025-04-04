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

package org.hkijena.jipipe.utils;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import org.apache.commons.exec.*;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.text.WordUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringQueryExpressionAndStringPairParameter;
import org.hkijena.jipipe.plugins.processes.ProcessEnvironment;
import org.hkijena.jipipe.utils.scripting.MacroUtils;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.BreadthFirstIterator;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ProcessUtils {

    private ProcessUtils() {

    }

    public static boolean systemIsMacM1() {
        return SystemUtils.IS_OS_MAC && ("aarch64".equals(SystemUtils.OS_ARCH) || "arm64".equals(SystemUtils.OS_ARCH));
    }

    /**
     * Test for handling quoting in addArgument
     *
     * @return if should handle quoting
     */
    public static boolean shouldHandleQuoting() {
        return SystemUtils.IS_OS_WINDOWS;
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
     * Runs a process
     *
     * @param environment                  the process environment
     * @param variables                    additional variables for the arguments (can be null)
     * @param overrideEnvironmentVariables additional environment variables
     * @param handleQuoting                if argument quoting is handled by commons exec (can be buggy)
     * @param progressInfo                 the progress info
     */
    public static void runProcess(ProcessEnvironment environment, JIPipeExpressionVariablesMap variables, Map<String, String> overrideEnvironmentVariables, boolean handleQuoting, JIPipeProgressInfo progressInfo) {
        CommandLine commandLine = new CommandLine(environment.getAbsoluteExecutablePath().toFile());

        Map<String, String> environmentVariables = new HashMap<>();
        JIPipeExpressionVariablesMap existingEnvironmentVariables = new JIPipeExpressionVariablesMap();
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            existingEnvironmentVariables.put(entry.getKey(), entry.getValue());
            environmentVariables.put(entry.getKey(), entry.getValue());
        }
        for (StringQueryExpressionAndStringPairParameter environmentVariable : environment.getEnvironmentVariables()) {
            String value = StringUtils.nullToEmpty(environmentVariable.getKey().evaluate(existingEnvironmentVariables));
            environmentVariables.put(environmentVariable.getValue(), value);
        }
        environmentVariables.putAll(overrideEnvironmentVariables);
        for (Map.Entry<String, String> entry : environmentVariables.entrySet()) {
            progressInfo.log("Setting environment variable " + entry.getKey() + "=" + entry.getValue());
        }

        if (variables == null) {
            variables = new JIPipeExpressionVariablesMap();
        }
        variables.set("executable", environment.getAbsoluteExecutablePath().toString());
        variables.set("executable_dir", environment.getAbsoluteExecutablePath().getParent().toString());
        Object evaluationResult = environment.getArguments().evaluate(variables);
        for (Object item : (Collection<?>) evaluationResult) {
            commandLine.addArgument(StringUtils.nullToEmpty(item), handleQuoting);
        }

        ProcessUtils.ExtendedExecutor executor = new ProcessUtils.ExtendedExecutor(ExecuteWatchdog.INFINITE_TIMEOUT, progressInfo);
        setupLogger(commandLine, executor, progressInfo);
        executor.setWorkingDirectory(Paths.get(environment.getWorkDirectory().evaluateToString(variables)).toFile());
        progressInfo.log("Work directory is " + executor.getWorkingDirectory());

        try {
            executor.execute(commandLine, environmentVariables);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Runs a process detached
     *
     * @param environment                  the process environment
     * @param variables                    additional variables for the arguments (can be null)
     * @param overrideEnvironmentVariables additional environment variables
     * @param handleQuoting                if argument quoting is handled by commons exec (can be buggy)
     * @param progressInfo                 the progress info
     */
    public static void launchProcess(ProcessEnvironment environment, JIPipeExpressionVariablesMap variables, Map<String, String> overrideEnvironmentVariables, boolean handleQuoting, JIPipeProgressInfo progressInfo) {
        CommandLine commandLine = new CommandLine(environment.getAbsoluteExecutablePath().toFile());

        Map<String, String> environmentVariables = new HashMap<>();
        JIPipeExpressionVariablesMap existingEnvironmentVariables = new JIPipeExpressionVariablesMap();
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            existingEnvironmentVariables.put(entry.getKey(), entry.getValue());
            environmentVariables.put(entry.getKey(), entry.getValue());
        }
        for (StringQueryExpressionAndStringPairParameter environmentVariable : environment.getEnvironmentVariables()) {
            String value = StringUtils.nullToEmpty(environmentVariable.getKey().evaluate(existingEnvironmentVariables));
            environmentVariables.put(environmentVariable.getValue(), value);
        }
        environmentVariables.putAll(overrideEnvironmentVariables);
        for (Map.Entry<String, String> entry : environmentVariables.entrySet()) {
            progressInfo.log("Setting environment variable " + entry.getKey() + "=" + entry.getValue());
        }

        if (variables == null) {
            variables = new JIPipeExpressionVariablesMap();
        }
        variables.set("executable", environment.getAbsoluteExecutablePath().toString());
        variables.set("executable_dir", environment.getAbsoluteExecutablePath().getParent().toString());
        Object evaluationResult = environment.getArguments().evaluate(variables);
        for (Object item : (Collection<?>) evaluationResult) {
            commandLine.addArgument(StringUtils.nullToEmpty(item), handleQuoting);
        }

        File workDirectory = Paths.get(environment.getWorkDirectory().evaluateToString(variables)).toFile();

        ProcessUtils.ExtendedExecutor executor = new ProcessUtils.ExtendedExecutor(ExecuteWatchdog.INFINITE_TIMEOUT, progressInfo);
        setupLogger(commandLine, executor, progressInfo);
        executor.setWorkingDirectory(workDirectory);
        progressInfo.log("Work directory is " + executor.getWorkingDirectory());

        try {
            executor.launch(commandLine, environmentVariables, workDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setupLogger(CommandLine commandLine, DefaultExecutor executor, JIPipeProgressInfo progressInfo) {
        progressInfo.log("Running " + Arrays.stream(commandLine.toStrings()).map(s -> {
            if (s.contains(" ")) {
                return "\"" + MacroUtils.escapeString(s) + "\"";
            } else {
                return MacroUtils.escapeString(s);
            }
        }).collect(Collectors.joining(" ")));

        LogOutputStream progressInfoLog = new LogOutputStream() {
            @Override
            protected void processLine(String s, int i) {
                for (String s1 : s.split("\\r")) {
                    progressInfo.log(WordUtils.wrap(s1, 120));
                }
            }
        };
        executor.setStreamHandler(new PumpStreamHandler(progressInfoLog, progressInfoLog));
    }

    /**
     * Queries standard output with a timeout.
     * Does not listen to cancellation signals
     *
     * @param executable   the executable
     * @param progressInfo the progress info
     * @param args         executable args
     * @return the stdout
     */
    public static String queryFast(Path executable, JIPipeProgressInfo progressInfo, String... args) {
        CommandLine commandLine = new CommandLine(executable.toFile());
        commandLine.addArguments(args);
        progressInfo.log("Running " + executable + " " + String.join(" ", args));
        DefaultExecutor executor = new DefaultExecutor();

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
                        graph.addVertex(pid);
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
                        try {
                            BreadthFirstIterator<Long, DefaultEdge> breadthFirstIterator = new BreadthFirstIterator<>(graph, pid);
                            while (breadthFirstIterator.hasNext()) {
                                long toKill = breadthFirstIterator.next();
                                progressInfo.log("Killing orphaned PID " + toKill);
                                queryFast(Paths.get(killPath), progressInfo, "-9", toKill + "");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
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
        public Process launch(CommandLine command, Map<String, String> env, File dir) throws IOException {
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
    public static class RunCancellationExecuteWatchdog extends ExecuteWatchdog implements RunCancellationWatchdog.CancelledEventListener {

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
            this.cancellationWatchdog.getCancelledEventEmitter().subscribe(this);
        }

        @Override
        public synchronized void timeoutOccured(Watchdog w) {
            // Kill the process using the PID
            long pid = extendedExecutor.getPid();
            killProcessTree(pid, progressInfo);
            super.timeoutOccured(w);
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

        @Override
        public void onCancelled(RunCancellationWatchdog.CancelledEvent event) {
            this.timeoutOccured(null);
        }
    }

    /**
     * A watchdog that monitors a sub-process of a {@link JIPipeRunnable}
     * and watches for the {@link JIPipeRunnable} to be cancelled.
     * Based on {@link Watchdog}
     */
    public static class RunCancellationWatchdog implements Runnable {
        private final CancelledEventEmitter cancelledEventEmitter = new CancelledEventEmitter();
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
                    isWaiting = !progressInfo.isCancelled();
                }
            }

            // notify the listeners outside of the synchronized block (see EXEC-60)
            if (!isWaiting) {
                cancelledEventEmitter.emit(new CancelledEvent(this));
            }
        }

        public CancelledEventEmitter getCancelledEventEmitter() {
            return cancelledEventEmitter;
        }

        public interface CancelledEventListener {
            void onCancelled(CancelledEvent event);
        }

        public static class CancelledEvent extends AbstractJIPipeEvent {
            private final RunCancellationWatchdog watchdog;

            public CancelledEvent(RunCancellationWatchdog watchdog) {
                super(watchdog);
                this.watchdog = watchdog;
            }

            public RunCancellationWatchdog getWatchdog() {
                return watchdog;
            }
        }

        public static class CancelledEventEmitter extends JIPipeEventEmitter<CancelledEvent, CancelledEventListener> {

            @Override
            protected void call(CancelledEventListener cancelledEventListener, CancelledEvent event) {
                cancelledEventListener.onCancelled(event);
            }
        }
    }
}
