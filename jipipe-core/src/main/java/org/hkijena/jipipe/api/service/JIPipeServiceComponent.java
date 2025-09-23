package org.hkijena.jipipe.api.service;

import org.hkijena.jipipe.api.JIPipeProgressInfo;

/**
 * A JIPipe service component
 */
public abstract class JIPipeServiceComponent {
    private final JIPipeService service;

    public JIPipeServiceComponent(JIPipeService service) {
        this.service = service;
    }

    public JIPipeService getService() {
        return service;
    }

    public JIPipeProgressInfo getProgressInfo() {
        return service.getProgressInfo();
    }
}
