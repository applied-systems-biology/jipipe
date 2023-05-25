package org.hkijena.jipipe.extensions.scene3d.model;

import java.util.List;

/**
 * A node in the 3D scene
 */
public interface Scene3DNode {
    /**
     * Converts this node into a mesh
     * @param target the list of target meshes
     */
    void toMesh(List<Scene3DMeshObject> target);
}
