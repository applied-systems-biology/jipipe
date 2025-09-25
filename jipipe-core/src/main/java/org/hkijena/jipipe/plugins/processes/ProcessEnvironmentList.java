package org.hkijena.jipipe.plugins.processes;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

/**
 * A list of {@link ProcessEnvironment}
 */
public class ProcessEnvironmentList extends JIPipeListParameter<ProcessEnvironment> {
    public ProcessEnvironmentList() {
        super(ProcessEnvironment.class);
    }

    public ProcessEnvironmentList(ProcessEnvironmentList other) {
        super(ProcessEnvironment.class);
        for (ProcessEnvironment environment : other) {
            add(new ProcessEnvironment(environment));
        }
    }
}
