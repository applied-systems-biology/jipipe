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

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

public class ProcessUtils {

    private ProcessUtils() {

    }

    /**
     * Queries standard output and error with a timeout
     * @param executable
     * @param args
     * @return
     */
    public static String queryAllFast(Path executable, String ...args) {
        CommandLine commandLine = new CommandLine(executable.toFile());
        commandLine.addArguments(args);
        DefaultExecutor executor = new DefaultExecutor();
        ExecuteWatchdog watchdog = new ExecuteWatchdog(5000);
        executor.setWatchdog(watchdog);

        // Capture stdout
        ByteArrayOutputStream standardOutputStream = new ByteArrayOutputStream();
        PumpStreamHandler outputStreamHandler = new PumpStreamHandler(standardOutputStream, standardOutputStream);
        executor.setStreamHandler(outputStreamHandler);

        try {
            int exitValue = executor.execute(commandLine);

            if(!executor.isFailure(exitValue)) {
                return new String(standardOutputStream.toByteArray());
            }
            else {
                return null;
            }

        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Queries standard output with a timeout
     * @param executable
     * @param args
     * @return
     */
    public static String queryFast(Path executable, String ...args) {
        CommandLine commandLine = new CommandLine(executable.toFile());
        commandLine.addArguments(args);
        DefaultExecutor executor = new DefaultExecutor();
        ExecuteWatchdog watchdog = new ExecuteWatchdog(5000);
        executor.setWatchdog(watchdog);

        // Capture stdout
        ByteArrayOutputStream standardOutputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorOutputStream = new ByteArrayOutputStream();
        PumpStreamHandler outputStreamHandler = new PumpStreamHandler(standardOutputStream, errorOutputStream);
        executor.setStreamHandler(outputStreamHandler);

        try {
            int exitValue = executor.execute(commandLine);

            if(!executor.isFailure(exitValue)) {
                return new String(standardOutputStream.toByteArray());
            }
            else {
                return null;
            }

        } catch (IOException e) {
            return null;
        }
    }

    public static int executeFast(Path executable, String... args) {
        CommandLine commandLine = new CommandLine(executable.toFile());
        commandLine.addArguments(args);
        DefaultExecutor executor = new DefaultExecutor();
        ExecuteWatchdog watchdog = new ExecuteWatchdog(5000);
        executor.setWatchdog(watchdog);

        try {
            return executor.execute(commandLine);
        } catch (IOException e) {
            return -1;
        }
    }

}
