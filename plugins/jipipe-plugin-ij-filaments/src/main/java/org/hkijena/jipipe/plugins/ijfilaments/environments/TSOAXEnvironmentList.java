package org.hkijena.jipipe.plugins.ijfilaments.environments;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

/**
 * A list of {@link TSOAXEnvironment}
 */
public class TSOAXEnvironmentList extends JIPipeListParameter<TSOAXEnvironment> {
    public TSOAXEnvironmentList() {
        super(TSOAXEnvironment.class);
    }

    public TSOAXEnvironmentList(TSOAXEnvironmentList other) {
        super(TSOAXEnvironment.class);
        for (TSOAXEnvironment environment : other) {
            add(new TSOAXEnvironment(environment));
        }
    }
}
