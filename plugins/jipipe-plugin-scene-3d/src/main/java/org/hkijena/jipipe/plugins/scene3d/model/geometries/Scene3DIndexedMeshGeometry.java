/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.scene3d.model.geometries;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.scene3d.model.Scene3DNode;

import java.awt.*;
import java.util.Arrays;
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
     * @param verticesIndex the vertex index
     * @param normalsIndex  the normals index
     */
    public Scene3DIndexedMeshGeometry(float[] vertices, float[] normals, int[] verticesIndex, int[] normalsIndex) {
        this.vertices = Objects.requireNonNull(vertices);
        this.normals = Objects.requireNonNull(normals);
        this.verticesIndex = Objects.requireNonNull(verticesIndex);
        this.normalsIndex = Objects.requireNonNull(normalsIndex);
    }

    public Scene3DIndexedMeshGeometry(Scene3DIndexedMeshGeometry other) {
        this.name = other.name;
        this.vertices = Arrays.copyOf(other.vertices, other.vertices.length);
        this.normals = Arrays.copyOf(other.normals, other.normals.length);
        this.verticesIndex = Arrays.copyOf(other.verticesIndex, other.verticesIndex.length);
        this.normalsIndex = Arrays.copyOf(other.normalsIndex, other.normalsIndex.length);
        this.color = other.color;
    }

    @Override
    public Scene3DNode duplicate() {
        return new Scene3DIndexedMeshGeometry(this);
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

    @JsonSetter("vertices")
    public void setVertices(float[] vertices) {
        this.vertices = vertices;
    }

    @JsonGetter("normals")
    @Override
    public float[] getNormals() {
        return normals;
    }

    @JsonSetter("normals")
    public void setNormals(float[] normals) {
        this.normals = normals;
    }

    @JsonGetter("vertices-index")
    @Override
    public int[] getVerticesIndex() {
        return verticesIndex;
    }

    @JsonSetter("vertices-index")
    public void setVerticesIndex(int[] verticesIndex) {
        this.verticesIndex = verticesIndex;
    }

    @JsonGetter("normals-index")
    @Override
    public int[] getNormalsIndex() {
        return normalsIndex;
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

    @Override
    public String toString() {
        return String.format("Indexed mesh (%d faces, %d MB)", getNumVertices(), ((vertices.length + normals.length + verticesIndex.length + normalsIndex.length) * 32L) / 1024 / 1024);
    }
}
