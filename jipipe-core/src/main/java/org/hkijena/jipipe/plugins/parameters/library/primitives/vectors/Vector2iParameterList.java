package org.hkijena.jipipe.plugins.parameters.library.primitives.vectors;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

public class Vector2iParameterList extends JIPipeListParameter<Vector2iParameter> {
    public Vector2iParameterList() {
        super(Vector2iParameter.class);
    }

    public Vector2iParameterList(Vector2iParameterList other) {
        super(Vector2iParameter.class);
        for (Vector2iParameter parameter : other) {
            add(new Vector2iParameter(parameter));
        }
    }
}
