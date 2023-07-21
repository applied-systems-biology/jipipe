package org.hkijena.jipipe.extensions.scene3d.model;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.scene3d.model.geometries.Scene3DMeshGeometry;

import java.awt.*;

public interface Scene3DGeometry extends Scene3DNode {

    /**
     * Returns a mesh equivalent of this geometry
     *
     * @param progressInfo the progress info
     * @return the mesh geometry
     */
    Scene3DMeshGeometry toMeshGeometry(JIPipeProgressInfo progressInfo);

    /**
     * Gets the color of this geometry
     *
     * @return the color
     */
    Color getColor();

    /**
     * Sets the color of this geometry
     *
     * @param color the color
     */
    void setColor(Color color);

    @Override
    default void copyMetadataFrom(Scene3DNode other) {
        Scene3DNode.super.copyMetadataFrom(other);
        if (other instanceof Scene3DGeometry) {
            setColor(((Scene3DGeometry) other).getColor());
        }
    }
}
