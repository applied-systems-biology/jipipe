package org.hkijena.jipipe.extensions.scene3d.model.geometries;

import org.hkijena.jipipe.extensions.scene3d.model.Scene3DGeometry;

public interface Scene3DMeshGeometry extends Scene3DGeometry {
    float[] getVertices();
    float[] getNormals();
    int[] getVerticesIndex();
    int[] getNormalsIndex();
    int getNumVertices();
}
