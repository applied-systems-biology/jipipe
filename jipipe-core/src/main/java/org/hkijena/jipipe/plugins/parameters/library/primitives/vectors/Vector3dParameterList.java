package org.hkijena.jipipe.plugins.parameters.library.primitives.vectors;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

public class Vector3dParameterList extends JIPipeListParameter<Vector3dParameter> {
    public Vector3dParameterList() {
        super(Vector3dParameter.class);
    }

    public Vector3dParameterList(Vector3dParameterList other) {
        super(Vector3dParameter.class);
        for (Vector3dParameter parameter : other) {
            add(new Vector3dParameter(parameter));
        }
    }
}
