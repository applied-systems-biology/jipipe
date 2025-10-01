package org.hkijena.jipipe.utils.process;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ExtendedExecutor extends DefaultExecutor {

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
