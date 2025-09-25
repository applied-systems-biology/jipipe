package org.hkijena.jipipe.plugins.r;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

/**
 * A list of {@link REnvironment}
 */
public class REnvironmentList extends JIPipeListParameter<REnvironment> {
    public REnvironmentList() {
        super(REnvironment.class);
    }

    public REnvironmentList(REnvironmentList other) {
        super(REnvironment.class);
        for (REnvironment environment : other) {
            add(new REnvironment(environment));
        }
    }
}
