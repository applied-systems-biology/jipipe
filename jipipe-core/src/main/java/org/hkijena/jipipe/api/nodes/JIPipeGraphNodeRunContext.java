package org.hkijena.jipipe.api.nodes;

import org.hkijena.jipipe.api.JIPipeFixedThreadPool;

public class JIPipeGraphNodeRunContext {
    private JIPipeFixedThreadPool threadPool;

    /**
     * Gets the thread pool for parallelization
     * @return the pool or null
     */
    public JIPipeFixedThreadPool getThreadPool() {
        return threadPool;
    }

    /**
     * Sets the thread tool for parallelization
     * @param threadPool the pool or null if parallelization should be disabled
     */
    public void setThreadPool(JIPipeFixedThreadPool threadPool) {
        this.threadPool = threadPool;
    }
}
