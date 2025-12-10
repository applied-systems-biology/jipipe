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

package org.hkijena.jipipe.api.nodes;

import org.hkijena.jipipe.api.JIPipeFixedThreadPool;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentConfigurationCache;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeGraphRun;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;

public class JIPipeGraphNodeRunContext {
    private JIPipeFixedThreadPool threadPool;
    private JIPipeGraphRun graphRun;
    private JIPipeEnvironmentConfigurationCache environmentConfigurationCache;

    /**
     * Gets the thread pool for parallelization
     *
     * @return the pool or null
     */
    public JIPipeFixedThreadPool getThreadPool() {
        return threadPool;
    }

    /**
     * Sets the thread tool for parallelization
     *
     * @param threadPool the pool or null if parallelization should be disabled
     */
    public void setThreadPool(JIPipeFixedThreadPool threadPool) {
        this.threadPool = threadPool;
    }

    /**
     * Gets the {@link JIPipeGraphRun} that is currently executing
     *
     * @return the graph run
     */
    public JIPipeGraphRun getGraphRun() {
        return graphRun;
    }

    /**
     * Sets the {@link JIPipeGraphRun} that is currently executing
     *
     * @param graphRun the graph run
     */
    public void setGraphRun(JIPipeGraphRun graphRun) {
        this.graphRun = graphRun;
    }

    public JIPipeEnvironmentConfigurationCache getEnvironmentConfigurationCache() {
        return environmentConfigurationCache;
    }

    public void setEnvironmentConfigurationCache(JIPipeEnvironmentConfigurationCache environmentConfigurationCache) {
        this.environmentConfigurationCache = environmentConfigurationCache;
    }

    public JIPipeProject getProject() {
        return graphRun.getProject();
    }

    public JIPipeDesktopProjectWorkbench getWorkbench() {
        JIPipeProject project = getProject();
        if(project != null) {
            JIPipeDesktopProjectWindow window = JIPipeDesktopProjectWindow.getWindowFor(project);
            if (window != null) {
                return window.getProjectWorkbench();
            }
        }
        return null;
    }
}
