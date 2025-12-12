package org.hkijena.jipipe.api.service;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.scijava.Context;

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

    public Context getContext() {
        return service.getContext();
    }

    public void postprocess(JIPipeProgressInfo progressInfo) {

    }
}
