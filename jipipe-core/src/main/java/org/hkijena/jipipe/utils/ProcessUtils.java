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
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringQueryExpressionAndStringPairParameter;
import org.hkijena.jipipe.plugins.processes.ProcessEnvironment;
import org.hkijena.jipipe.utils.process.ExtendedExecutor;
import org.hkijena.jipipe.utils.process.ProcessSidecarTask;
import org.hkijena.jipipe.utils.scripting.MacroUtils;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.BreadthFirstIterator;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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
     * @param sidecars  additional tasks that are handled during process execution
     * @param progressInfo                 the progress info
     */
    public static void runProcess(ProcessEnvironment environment, JIPipeExpressionVariablesMap variables, Map<String, String> overrideEnvironmentVariables, boolean handleQuoting, List<ProcessSidecarTask> sidecars, JIPipeProgressInfo progressInfo) {
        CommandLine commandLine = new CommandLine(environment.getAbsoluteExecutablePath().toFile());

        Map<String, String> environmentVariables = new HashMap<>();
        JIPipeExpressionVariablesMap existingEnvironmentVariables = new JIPipeExpressionVariablesMap();
        Map<String, String> systemEnv = System.getenv();
        for (Map.Entry<String, String> entry : systemEnv.entrySet()) {
            existingEnvironmentVariables.put(entry.getKey(), entry.getValue());
            environmentVariables.put(entry.getKey(), entry.getValue());
        }
        for (StringQueryExpressionAndStringPairParameter environmentVariable : environment.getEnvironmentVariables()) {
            String value = StringUtils.nullToEmpty(environmentVariable.getKey().evaluate(existingEnvironmentVariables));
            environmentVariables.put(environmentVariable.getValue(), value);
        }
        environmentVariables.putAll(overrideEnvironmentVariables);

        for (Map.Entry<String, String> entry : environmentVariables.entrySet()) {
            String existing = systemEnv.get(entry.getKey());
            if(existing == null || !existing.equals(entry.getValue())) {
                progressInfo.log("Setting environment variable " + entry.getKey() + "=" + entry.getValue());
            }
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

        ExtendedExecutor executor = new ExtendedExecutor(ExecuteWatchdog.INFINITE_TIMEOUT, progressInfo);
        setupLogger(commandLine, executor, progressInfo);
        executor.setWorkingDirectory(Paths.get(environment.getWorkDirectory().evaluateToString(variables)).toFile());
        progressInfo.log("Work directory is " + executor.getWorkingDirectory());

        try {
            for (ProcessSidecarTask sidecar : sidecars) {
                try {
                    sidecar.start(executor);
                } catch (Exception e) {
                    progressInfo.log("Failed to start sidecar: " + e.getMessage());
                    progressInfo.log(e);
                }
            }
            executor.execute(commandLine, environmentVariables);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            for (ProcessSidecarTask sidecar : sidecars) {
                sidecar.stop();
            }
        }
    }

    /**
     * Runs a process detached
     *
     * @param environment                  the process environment
     * @param variables                    additional variables for the arguments (can be null)
     * @param overrideEnvironmentVariables additional environment variables
     * @param handleQuoting                if argument quoting is handled by commons exec (can be buggy)
     * @param sidecars
     * @param progressInfo                 the progress info
     */
    public static void launchProcess(ProcessEnvironment environment, JIPipeExpressionVariablesMap variables, Map<String, String> overrideEnvironmentVariables, boolean handleQuoting, List<ProcessSidecarTask> sidecars, JIPipeProgressInfo progressInfo) {
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

        ExtendedExecutor executor = new ExtendedExecutor(ExecuteWatchdog.INFINITE_TIMEOUT, progressInfo);
        setupLogger(commandLine, executor, progressInfo);
        executor.setWorkingDirectory(workDirectory);
        progressInfo.log("Work directory is " + executor.getWorkingDirectory());

        try {
            for (ProcessSidecarTask sidecar : sidecars) {
                try {
                    sidecar.start(executor);
                } catch (Exception e) {
                    progressInfo.log("Failed to start sidecar: " + e.getMessage());
                    progressInfo.log(e);
                }
            }
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

}
