package org.hkijena.jipipe.plugins.python.adapter;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

/**
 * A list of {@link JIPipePythonAdapterLibraryEnvironment}
 */
public class JIPipePythonAdapterLibraryEnvironmentList extends JIPipeListParameter<JIPipePythonAdapterLibraryEnvironment> {
    public JIPipePythonAdapterLibraryEnvironmentList() {
        super(JIPipePythonAdapterLibraryEnvironment.class);
    }

    public JIPipePythonAdapterLibraryEnvironmentList(JIPipePythonAdapterLibraryEnvironmentList other) {
        super(JIPipePythonAdapterLibraryEnvironment.class);
        for (JIPipePythonAdapterLibraryEnvironment environment : other) {
            add(new JIPipePythonAdapterLibraryEnvironment(environment));
        }
    }
}
