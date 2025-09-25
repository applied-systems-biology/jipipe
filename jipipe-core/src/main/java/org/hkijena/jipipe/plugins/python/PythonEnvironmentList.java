package org.hkijena.jipipe.plugins.python;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

/**
 * A list of {@link PythonEnvironment}
 */
public class PythonEnvironmentList extends JIPipeListParameter<PythonEnvironment> {
    public PythonEnvironmentList() {
        super(PythonEnvironment.class);
    }

    public PythonEnvironmentList(PythonEnvironmentList other) {
        super(PythonEnvironment.class);
        for (PythonEnvironment environment : other) {
            add(new PythonEnvironment(environment));
        }
    }
}
