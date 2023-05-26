package org.hkijena.jipipe.extensions.scene3d.model.geometries;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.scene3d.utils.Scene3DUtils;

import java.awt.*;
import java.util.List;
import java.util.Objects;

public class Scene3DIndexedMeshGeometry implements Scene3DMeshGeometry {
    private String name;
    private float[] vertices;
    private float[] normals;
    private int[] verticesIndex;
    private int[] normalsIndex;
    private Color color = Color.RED;

    /**
     * Creates a mesh object
     *
     * @param vertices      the vertices (indexed)
     * @param normals       the normals (indexed)
     * @param verticesIndex  the vertex index
     * @param normalsIndex the normals index
     */
    public Scene3DIndexedMeshGeometry(float[] vertices, float[] normals, int[] verticesIndex, int[] normalsIndex) {
        this.vertices = Objects.requireNonNull(vertices);
        this.normals = Objects.requireNonNull(normals);
        this.verticesIndex = Objects.requireNonNull(verticesIndex);
        this.normalsIndex = Objects.requireNonNull(normalsIndex);
    }

    @Override
    public Scene3DMeshGeometry toMeshGeometry(JIPipeProgressInfo progressInfo) {
        return this;
    }

    @Override
    @JsonGetter("vertices")
    public float[] getVertices() {
        return vertices;
    }

    @JsonGetter("normals")
    @Override
    public float[] getNormals() {
        return normals;
    }

    @JsonGetter("vertices-index")
    @Override
    public int[] getVerticesIndex() {
        return verticesIndex;
    }

    @JsonGetter("normals-index")
    @Override
    public int[] getNormalsIndex() {
        return normalsIndex;
    }

    @JsonSetter("vertices")
    public void setVertices(float[] vertices) {
        this.vertices = vertices;
    }

    @JsonSetter("normals")
    public void setNormals(float[] normals) {
        this.normals = normals;
    }

    @JsonSetter("vertices-index")
    public void setVerticesIndex(int[] verticesIndex) {
        this.verticesIndex = verticesIndex;
    }

    @JsonSetter("normals-index")
    public void setNormalsIndex(int[] normalsIndex) {
        this.normalsIndex = normalsIndex;
    }

    @JsonGetter("name")
    @Override
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int getNumVertices() {
        return verticesIndex.length / 9;
    }

    @JsonGetter("color")
    @Override
    public Color getColor() {
        return color;
    }

    @JsonSetter("color")
    @Override
    public void setColor(Color color) {
        this.color = color;
    }
}
