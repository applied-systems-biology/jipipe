package org.hkijena.jipipe.extensions.scene3d.model;

import org.hkijena.jipipe.extensions.scene3d.utils.Scene3DUtils;

import java.util.List;

public class Scene3DMeshObject extends Scene3DObject {

    private final float[] vertices;
    private final float[] normals;

    /**
     * Creates a mesh object
     * @param vertices the vertices (unindexed, 9 values per vertex)
     * @param normals the normals (9 values per vertex)
     */
    public Scene3DMeshObject(float[] vertices, float[] normals) {
        this.vertices = vertices;
        this.normals = normals != null ? normals : Scene3DUtils.generateVertexNormalsFlat(vertices);
    }

    @Override
    public void toMesh(List<Scene3DMeshObject> target) {
        target.add(this);
    }
}
