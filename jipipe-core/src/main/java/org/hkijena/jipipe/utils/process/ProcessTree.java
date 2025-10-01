package org.hkijena.jipipe.utils.process;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.utils.ProcessUtils;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Wrapper around an existing process that models a process tree
 */
public class ProcessTree extends Process {
    private final Process process;
    private final long pid;
    private final JIPipeProgressInfo progressInfo;

    public ProcessTree(Process process, JIPipeProgressInfo progressInfo) {
        this.process = process;
        this.pid = process.pid();
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
        ProcessUtils.killProcessTree(pid, progressInfo);
    }

    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }
}
