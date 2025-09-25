package org.hkijena.jipipe.plugins.parameters.library.primitives.vectors;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

public class Vector2dParameterList extends JIPipeListParameter<Vector2dParameter> {
    public Vector2dParameterList() {
        super(Vector2dParameter.class);
    }

    public Vector2dParameterList(Vector2dParameterList other) {
        super(Vector2dParameter.class);
        for (Vector2dParameter parameter : other) {
            add(new Vector2dParameter(parameter));
        }
    }
}
