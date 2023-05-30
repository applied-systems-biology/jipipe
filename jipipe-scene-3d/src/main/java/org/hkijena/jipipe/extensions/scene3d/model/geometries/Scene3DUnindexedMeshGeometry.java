package org.hkijena.jipipe.extensions.scene3d.model.geometries;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import gnu.trove.impl.Constants;
import gnu.trove.list.TFloatList;
import gnu.trove.map.TFloatIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TFloatIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TFloatSet;
import gnu.trove.set.hash.TFloatHashSet;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.scene3d.model.Scene3DGeometry;
import org.hkijena.jipipe.extensions.scene3d.utils.Scene3DUtils;
import org.joml.Vector3f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Scene3DUnindexedMeshGeometry implements Scene3DMeshGeometry {

    private String name;
    private float[] vertices;
    private float[] normals;
    private int[] index;
    private Color color = Color.RED;

    /**
     * Creates a mesh object
     * @param vertices the vertices (unindexed, 9 values per vertex)
     * @param normals the normals (9 values per vertex)
     */
    public Scene3DUnindexedMeshGeometry(float[] vertices, float[] normals) {
        this.vertices = Objects.requireNonNull(vertices);
        this.normals = Objects.requireNonNull(normals);
        Scene3DUtils.checkUnindexedNormalsArray(vertices, normals);
    }

    @Override
    public Scene3DMeshGeometry toMeshGeometry(JIPipeProgressInfo progressInfo) {
        return this;
    }

    @JsonGetter("vertices")
    @Override
    public float[] getVertices() {
        return vertices;
    }

    @JsonGetter("normals")
    @Override
    public float[] getNormals() {
        return normals;
    }

    @Override
    public int[] getVerticesIndex() {
        ensureIndex();
        return index;
    }

    @JsonSetter("vertices")
    public void setVertices(float[] vertices) {
        this.vertices = vertices;
    }

    @JsonSetter("normals")
    public void setNormals(float[] normals) {
        this.normals = normals;
    }

    private void ensureIndex() {
        if(index == null) {
            index = new int[getNumVertices() * 3];
            for (int i = 0; i < getNumVertices() * 3; i++) {
                index[i] = i;
            }
        }
    }

    @Override
    public int[] getNormalsIndex() {
        ensureIndex();
        return index;
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
        return vertices.length / 9;
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

    /**
     * Converts this unindexed mesh geometry into an optimized, indexed mesh geometry
     * @param progressInfo the progress info
     * @return the indexed geometry
     */
    public Scene3DIndexedMeshGeometry toIndexedMeshGeometry(JIPipeProgressInfo progressInfo) {
        TObjectIntMap<Vector3f> uniqueVertices = new TObjectIntHashMap<>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1);
        TObjectIntMap<Vector3f> uniqueNormals = new TObjectIntHashMap<>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1);

        int[] verticesIndex = new int[vertices.length / 3];
        int[] normalsIndex = new int[normals.length / 3];

        int lastPercentage = 0;

        for (int i = 0; i < vertices.length / 3; i++) {

            int nextPercentage = (int)(100.0 * i / (vertices.length / 3));

            if(nextPercentage != lastPercentage) {

                if(progressInfo.isCancelled()) {
                    return null;
                }

                progressInfo.log("Finding unique vertices/normals ... " + nextPercentage + "%");
                lastPercentage = nextPercentage;
            }

            Vector3f vertex = new Vector3f(vertices[i * 3], vertices[i * 3 + 1], vertices[i * 3 + 2]);
            Vector3f normal = new Vector3f(normals[i * 3], normals[i * 3 + 1], normals[i * 3 + 2]);

            int vertexIndex = uniqueVertices.get(vertex);
            int normalIndex = uniqueNormals.get(normal);

            if(vertexIndex < 0) {
                vertexIndex = uniqueVertices.size();
                uniqueVertices.put(vertex, vertexIndex);
            }
            if(normalIndex < 0) {
                normalIndex = uniqueNormals.size();
                uniqueNormals.put(normal, normalIndex);
            }

            verticesIndex[i] = vertexIndex;
            normalsIndex[i] = normalIndex;
        }

        if(progressInfo.isCancelled()) {
            return null;
        }

        float[] compressedVertices = new float[uniqueVertices.size() * 3];
        float[] compressedNormals = new float[uniqueNormals.size() * 3];

        progressInfo.log("Generating output arrays ...");
        for (Vector3f vec : uniqueVertices.keySet()) {
            int i = uniqueVertices.get(vec);
            compressedVertices[i * 3] = vec.x;
            compressedVertices[i * 3 + 1] = vec.y;
            compressedVertices[i * 3 + 2] = vec.z;
        }
        for (Vector3f vec : uniqueNormals.keySet()) {
            int i = uniqueNormals.get(vec);
            compressedNormals[i * 3] = vec.x;
            compressedNormals[i * 3 + 1] = vec.y;
            compressedNormals[i * 3 + 2] = vec.z;
        }

        Scene3DIndexedMeshGeometry geometry = new Scene3DIndexedMeshGeometry(compressedVertices, compressedNormals, verticesIndex, normalsIndex);
        geometry.copyMetadataFrom(this);
        return geometry;
    }

    @Override
    public String toString() {
        return String.format("Unindexed mesh (%d faces, %d MB)", getNumVertices(), ((vertices.length + normals.length) * 32L) / 1024 / 1024 );
    }
}