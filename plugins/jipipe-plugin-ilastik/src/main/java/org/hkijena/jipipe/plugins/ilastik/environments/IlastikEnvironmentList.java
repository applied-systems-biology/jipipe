package org.hkijena.jipipe.plugins.ilastik.environments;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

/**
 * A list of {@link IlastikEnvironment}
 */
public class IlastikEnvironmentList extends JIPipeListParameter<IlastikEnvironment> {
    public IlastikEnvironmentList() {
        super(IlastikEnvironment.class);
    }

    public IlastikEnvironmentList(IlastikEnvironmentList other) {
        super(IlastikEnvironment.class);
        for (IlastikEnvironment environment : other) {
            add(new IlastikEnvironment(environment));
        }
    }
}
