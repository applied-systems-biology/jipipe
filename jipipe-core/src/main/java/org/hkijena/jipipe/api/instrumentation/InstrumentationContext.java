package org.hkijena.jipipe.api.instrumentation;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;

/**
 * Context passed to instrumentation operations.
 */
public class InstrumentationContext {
    private final JIPipeProject project;
    private final JIPipeProgressInfo progressInfo;

    public InstrumentationContext(JIPipeProject project, JIPipeProgressInfo progressInfo) {
        this.project = project;
        this.progressInfo = progressInfo != null ? progressInfo : new JIPipeProgressInfo();
    }

    public JIPipeProject getProject() {
        return project;
    }

    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }
}
