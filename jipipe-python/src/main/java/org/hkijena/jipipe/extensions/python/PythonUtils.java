package org.hkijena.jipipe.extensions.python;

import org.apache.commons.exec.*;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;

public class PythonUtils {
    private PythonUtils() {

    }

    public static void runPython(Path scriptFile, JIPipeProgressInfo progressInfo) {
        Path pythonExecutable = PythonExtensionSettings.getInstance().getPythonExecutable();
        CommandLine commandLine = new CommandLine(pythonExecutable.toFile());
        commandLine.addArgument(scriptFile.toString());

        LogOutputStream progressInfoLog = new LogOutputStream() {
            @Override
            protected void processLine(String s, int i) {
               progressInfo.log(s);
            }
        };

        DefaultExecutor executor = new DefaultExecutor();
        executor.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));
        executor.setStreamHandler(new PumpStreamHandler(progressInfoLog, progressInfoLog));

        try {
            executor.execute(commandLine);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
