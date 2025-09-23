package org.hkijena.jipipe.api.initialization;

import org.hkijena.jipipe.JIPipe;

/**
 * Base class for all initialization tasks related to JIPipe
 */
public abstract class JIPipeServiceInitializer {
    private final JIPipe jiPipe;

    protected JIPipeServiceInitializer(JIPipe jiPipe) {
        this.jiPipe = jiPipe;
    }

    public JIPipe getJiPipe() {
        return jiPipe;
    }
}
