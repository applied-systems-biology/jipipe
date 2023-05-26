package org.hkijena.jipipe.extensions.scene3d.utils;

import gnu.trove.list.array.TFloatArrayList;

public class Scene3DUtils {

    public static void checkUnindexedVertexArray(float[] vertices) {
        if(vertices.length % 9 != 0) {
            throw new IllegalArgumentException("Invalid vertex array: length not divisable by 3");
        }
    }

    public static float[] generateUnindexedVertexNormalsFlat(float[] vertices) {
        checkUnindexedVertexArray(vertices);
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
        checkUnindexedVertexArray(vertices);
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
}
