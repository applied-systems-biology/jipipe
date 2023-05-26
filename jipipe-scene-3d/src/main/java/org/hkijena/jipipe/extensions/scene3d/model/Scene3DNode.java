package org.hkijena.jipipe.extensions.scene3d.model;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.scene3d.model.geometries.Scene3DMeshGeometry;
import org.hkijena.jipipe.extensions.scene3d.model.geometries.Scene3DUnindexedMeshGeometry;

import java.util.List;

/**
 * A node in the 3D scene
 */
public interface Scene3DNode {

    /**
     * Gets the name of this node
     * @return the name
     */
    String getName();

    /**
     * Sets the name of this node
     * @param name the name
     */
    void setName(String name);

    /**
     * Converts this node into a mesh
     * @param target the list of target meshes
     * @param progressInfo the progress info
     */
    void toMesh(List<Scene3DMeshGeometry> target, JIPipeProgressInfo progressInfo);

    /**
     * Copies metadata from another node
     * @param other the other node
     */
    default void copyMetadataFrom(Scene3DNode other) {
        setName(other.getName());
    }
}
