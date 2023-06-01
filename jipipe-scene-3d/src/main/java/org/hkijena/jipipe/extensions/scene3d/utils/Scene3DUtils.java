package org.hkijena.jipipe.extensions.scene3d.utils;

import gnu.trove.list.array.TFloatArrayList;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Scene3DUtils {

    public static Vector3f getPerpendicularVector(Vector3f vec) {
        return Math.abs(vec.z) < Math.abs(vec.x) ? new Vector3f(vec.y, -vec.x, 0) : new Vector3f(0, -vec.z, vec.y);
    }

    public static void checkUnindexedPolygonArray(float[] vertices) {
        if(vertices.length % 9 != 0) {
            throw new IllegalArgumentException("Invalid vertex array: length not divisable by 3");
        }
    }

    public static float[] generateUnindexedVertexNormalsFlat(float[] vertices) {
        checkUnindexedPolygonArray(vertices);
        float[] normals = new float[vertices.length];
        for (int i = 0; i < vertices.length / 9; i++) {
            float ax = vertices[i * 9];
            float ay = vertices[i * 9 + 1];
            float az = vertices[i * 9 + 2];
            float bx = vertices[i * 9 + 3];
            float by = vertices[i * 9 + 4];
            float bz = vertices[i * 9 + 5];
            float cx = vertices[i * 9 + 6];
            float cy = vertices[i * 9 + 7];
            float cz = vertices[i * 9 + 8];

            float bax = bx - ax;
            float bay = by - ay;
            float baz = bz - az;

            float cax = cx - ax;
            float cay = cy - ay;
            float caz = cz - az;

            float crossProductX = bay * caz - baz * cay;
            float crossProductY = baz * cax - bax * caz;
            float crossProductZ = bax * cay - bay * cax;

            float magnitude = (float) Math.sqrt(crossProductX * crossProductX + crossProductY * crossProductY + crossProductZ * crossProductZ);

            float normalizedCrossProductX = crossProductX / magnitude;
            float normalizedCrossProductY = crossProductY / magnitude;
            float normalizedCrossProductZ = crossProductZ / magnitude;

            normals[i * 9] = normalizedCrossProductX;
            normals[i * 9 + 1] = normalizedCrossProductY;
            normals[i * 9 + 2] = normalizedCrossProductZ;
            normals[i * 9 + 3] = normalizedCrossProductX;
            normals[i * 9 + 4] = normalizedCrossProductY;
            normals[i * 9 + 5] = normalizedCrossProductZ;
            normals[i * 9 + 6] = normalizedCrossProductX;
            normals[i * 9 + 7] = normalizedCrossProductY;
            normals[i * 9 + 8] = normalizedCrossProductZ;
        }

        return normals;
    }

    public static boolean[] findUnindexedNaNNormalVertices(float[] vertices, float[] normals) {
        checkUnindexedPolygonArray(vertices);
        checkUnindexedNormalsArray(vertices, normals);
        boolean[] mask = new boolean[vertices.length];
        for (int i = 0; i < vertices.length / 9; i++) {
            boolean invalid = false;
            for (int j = i * 9; j < (i + 1) * 9; j++) {
                if(Float.isNaN(normals[j])) {
                    invalid = true;
                    break;
                }
            }
            if(invalid) {
                for (int j = i * 9; j < (i + 1) * 9; j++) {
                    mask[j] = true;
                }
            }
        }
        return mask;
    }

    public static float[] filterArray(float[] arr, boolean[] mask, boolean maskToKeep) {
        TFloatArrayList masked = new TFloatArrayList(arr.length);
        for (int i = 0; i < arr.length; i++) {
            if(mask[i] == maskToKeep) {
                masked.add(arr[i]);
            }
        }
        return masked.toArray();
    }

    public static void checkUnindexedNormalsArray(float[] vertices, float[] normals) {
        if(vertices.length % 9 != 0) {
            throw new IllegalArgumentException("Invalid vertex array: length not divisable by 3");
        }
        if(vertices.length != normals.length) {
            throw new IllegalArgumentException("Invalid normals array: length not the same as number of vertex data!");
        }
    }

    public static float[] generateUVSphereVertices(float radiusX, float radiusY, float radiusZ, int subDiv) {
        int stacks = (int) Math.max(1, Math.ceil(radiusZ * subDiv));
        int slices = (int)Math.max(1, Math.max(Math.ceil(radiusX * subDiv), Math.ceil(radiusY * subDiv)));

        float[] vertices = new float[stacks * slices * 3];
        int vertexIndex = 0;
        for (int i = 0; i < stacks; i++) {
            float phi = (float) Math.PI * i / (stacks - 1);
            float sinPhi = (float) Math.sin(phi);
            float cosPhi = (float) Math.cos(phi);

            for (int j = 0; j < slices; j++) {
                float theta = (float) (2 * Math.PI * j / (slices - 1));
                float sinTheta = (float) Math.sin(theta);
                float cosTheta = (float) Math.cos(theta);

                float x = radiusX * sinPhi * cosTheta;
                float y = radiusY * sinPhi * sinTheta;
                float z = radiusZ * cosPhi;

                vertices[vertexIndex++] = x / subDiv;
                vertices[vertexIndex++] = y / subDiv;
                vertices[vertexIndex++] = z / subDiv;
            }
        }

        float[] triangles = new float[vertices.length * 3 * 2];
        int trianglesIndex = 0;

        for (int i = 0; i < stacks; i++) {
            int k1 = i * (slices + 1);
            int k2 = k1 + slices + 1;

            for (int j = 0; j < slices; j++, k1++, k2++) {
                if (i != 0) {
                    triangles[trianglesIndex++] = vertices[k1 * 3];
                    triangles[trianglesIndex++] = vertices[k1 * 3 + 1];
                    triangles[trianglesIndex++] = vertices[k1 * 3 + 2];
                    triangles[trianglesIndex++] = vertices[k2 * 3];
                    triangles[trianglesIndex++] = vertices[k2 * 3 + 1];
                    triangles[trianglesIndex++] = vertices[k2 * 3 + 2];
                    triangles[trianglesIndex++] = vertices[(k1 + 1) * 3];
                    triangles[trianglesIndex++] = vertices[(k1 + 1) * 3 + 1];
                    triangles[trianglesIndex++] = vertices[(k1 + 1) * 3 + 2];
                }

                if (i != stacks - 1) {
                    triangles[trianglesIndex++] = vertices[(k1 + 1) * 3];
                    triangles[trianglesIndex++] = vertices[(k1 + 1) * 3 + 1];
                    triangles[trianglesIndex++] = vertices[(k1 + 1) * 3 + 2];
                    triangles[trianglesIndex++] = vertices[k2 * 3];
                    triangles[trianglesIndex++] = vertices[k2 * 3 + 1];
                    triangles[trianglesIndex++] = vertices[k2 * 3 + 2];
                    triangles[trianglesIndex++] = vertices[(k2 + 1) * 3];
                    triangles[trianglesIndex++] = vertices[(k2 + 1) * 3 + 1];
                    triangles[trianglesIndex++] = vertices[(k2 + 1) * 3 + 2];
                }
            }
        }

        return triangles;
    }

}
