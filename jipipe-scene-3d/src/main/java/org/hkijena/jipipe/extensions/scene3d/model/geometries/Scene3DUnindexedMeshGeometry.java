package org.hkijena.jipipe.extensions.scene3d.model.geometries;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import gnu.trove.list.TFloatList;
import gnu.trove.map.TFloatIntMap;
import gnu.trove.map.hash.TFloatIntHashMap;
import gnu.trove.set.TFloatSet;
import gnu.trove.set.hash.TFloatHashSet;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.scene3d.model.Scene3DGeometry;
import org.hkijena.jipipe.extensions.scene3d.utils.Scene3DUtils;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public class Scene3DUnindexedMeshGeometry implements Scene3DMeshGeometry {

    private String name;
    private float[] vertices;
    private float[] normals;
    private int[] index;

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
    public void toMesh(List<Scene3DMeshGeometry> target, JIPipeProgressInfo progressInfo) {
        target.add(this);
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

    /**
     * Converts this unindexed mesh geometry into an optimized, indexed mesh geometry
     * @param progressInfo the progress info
     * @return the indexed geometry
     */
    public Scene3DIndexedMeshGeometry toIndexedMeshGeometry(JIPipeProgressInfo progressInfo) {
        List<Vector3f> uniqueVertices = new ArrayList<>();
        List<Vector3f> uniqueNormals = new ArrayList<>();

        int[] verticesIndex = new int[vertices.length / 3];
        int[] normalsIndex = new int[normals.length / 3];

        for (int i = 0; i < vertices.length / 3; i++) {
            Vector3f vertex = new Vector3f(vertices[i * 3], vertices[i * 3 + 1], vertices[i * 3 + 2]);
            Vector3f normal = new Vector3f(normals[i * 3], normals[i * 3 + 1], normals[i * 3 + 2]);

            int vertexIndex = uniqueVertices.indexOf(vertex);
            int normalIndex = uniqueNormals.indexOf(normal);

            if(vertexIndex < 0) {
                vertexIndex = uniqueVertices.size();
                uniqueVertices.add(vertex);
            }
            if(normalIndex < 0) {
                normalIndex = uniqueNormals.size();
                uniqueNormals.add(normal);
            }

            verticesIndex[i] = vertexIndex;
            normalsIndex[i] = normalIndex;
        }

        float[] compressedVertices = new float[uniqueVertices.size() * 3];
        float[] compressedNormals = new float[uniqueNormals.size() * 3];

        for (int i = 0; i < uniqueVertices.size(); i++) {
            Vector3f vertex = uniqueVertices.get(i);
            compressedVertices[i * 3] = vertex.x;
            compressedVertices[i * 3 + 1] = vertex.y;
            compressedVertices[i * 3 + 2] = vertex.z;
        }
        for (int i = 0; i < uniqueNormals.size(); i++) {
            Vector3f normal = uniqueNormals.get(i);
            compressedNormals[i * 3] = normal.x;
            compressedNormals[i * 3 + 1] = normal.y;
            compressedNormals[i * 3 + 2] = normal.z;
        }

        Scene3DIndexedMeshGeometry geometry = new Scene3DIndexedMeshGeometry(compressedVertices, compressedNormals, verticesIndex, normalsIndex);
        geometry.copyMetadataFrom(this);
        return geometry;
    }
}
