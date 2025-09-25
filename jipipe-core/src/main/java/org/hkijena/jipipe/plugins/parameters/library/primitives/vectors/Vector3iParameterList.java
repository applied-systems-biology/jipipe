package org.hkijena.jipipe.plugins.parameters.library.primitives.vectors;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

public class Vector3iParameterList extends JIPipeListParameter<Vector3iParameter> {
    public Vector3iParameterList() {
        super(Vector3iParameter.class);
    }

    public Vector3iParameterList(Vector3iParameterList other) {
        super(Vector3iParameter.class);
        for (Vector3iParameter parameter : other) {
            add(new Vector3iParameter(parameter));
        }
    }
}
