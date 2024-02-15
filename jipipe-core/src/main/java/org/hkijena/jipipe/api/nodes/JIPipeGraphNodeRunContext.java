package org.hkijena.jipipe.api.nodes;

import org.hkijena.jipipe.api.JIPipeFixedThreadPool;
import org.hkijena.jipipe.api.run.JIPipeGraphRun;

public class JIPipeGraphNodeRunContext {
    private JIPipeFixedThreadPool threadPool;
    private JIPipeGraphRun graphRun;

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

    /**
     * Gets the {@link JIPipeGraphRun} that is currently executing
     * @return the graph run
     */
    public JIPipeGraphRun getGraphRun() {
        return graphRun;
    }

    /**
     * Sets the {@link JIPipeGraphRun} that is currently executing
     * @param graphRun the graph run
     */
    public void setGraphRun(JIPipeGraphRun graphRun) {
        this.graphRun = graphRun;
    }
}
