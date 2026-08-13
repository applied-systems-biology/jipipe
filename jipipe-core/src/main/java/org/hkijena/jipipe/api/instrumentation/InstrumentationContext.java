package org.hkijena.jipipe.api.instrumentation;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.project.JIPipeProject;

/**
 * Context passed to instrumentation operations.
 * <p>
 * The {@code graphOverride} allows callers (e.g., the AI agent's stage system)
 * to pass a working graph instead of the project graph. External WebSocket tools
 * always use the project graph (graphOverride is null).
 */
public class InstrumentationContext {
    private final JIPipeProject project;
    private final JIPipeGraph graphOverride;
    private final JIPipeProgressInfo progressInfo;
    private final InstrumentationEventBus eventBus;
    private final InstrumentationJobManager jobManager;
    private final String projectId;

    public InstrumentationContext(JIPipeProject project, String projectId,
                                   JIPipeProgressInfo progressInfo,
                                   InstrumentationEventBus eventBus,
                                   InstrumentationJobManager jobManager) {
        this(project, projectId, null, progressInfo, eventBus, jobManager);
    }

    public InstrumentationContext(JIPipeProject project, String projectId,
                                   JIPipeGraph graphOverride,
                                   JIPipeProgressInfo progressInfo,
                                   InstrumentationEventBus eventBus,
                                   InstrumentationJobManager jobManager) {
        this.project = project;
        this.projectId = projectId;
        this.graphOverride = graphOverride;
        this.progressInfo = progressInfo != null ? progressInfo : new JIPipeProgressInfo();
        this.eventBus = eventBus;
        this.jobManager = jobManager;
    }

    public JIPipeProject getProject() { return project; }
    public String getProjectId() { return projectId; }
    public JIPipeProgressInfo getProgressInfo() { return progressInfo; }
    public InstrumentationEventBus getEventBus() { return eventBus; }
    public InstrumentationJobManager getJobManager() { return jobManager; }

    /**
     * Returns the graph to operate on. If a graphOverride was set (e.g., a stage's
     * working graph), returns that; otherwise returns the project's graph.
     */
    public JIPipeGraph getGraph() {
        if (graphOverride != null) return graphOverride;
        if (project != null) return project.getGraph();
        return null;
    }
}
