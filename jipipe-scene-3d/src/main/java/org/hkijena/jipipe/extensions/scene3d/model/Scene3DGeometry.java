package org.hkijena.jipipe.extensions.scene3d.model;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.scene3d.model.geometries.Scene3DMeshGeometry;

public interface Scene3DGeometry extends Scene3DNode {

    /**
     * Returns a mesh equivalent of this geometry
     * @param progressInfo the progress info
     * @return the mesh geometry
     */
    Scene3DMeshGeometry toMeshGeometry(JIPipeProgressInfo progressInfo);

}
