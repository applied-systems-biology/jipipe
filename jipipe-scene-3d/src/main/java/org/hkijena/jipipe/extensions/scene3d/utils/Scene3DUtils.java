package org.hkijena.jipipe.extensions.scene3d.utils;

import gnu.trove.list.TFloatList;
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

    public static float[] generateLineVertices(float x1, float y1, float z1, float radius1, float x2, float y2, float z2, float radius2, int subDiv) {
        int nPoints = (int) Math.max(1, Math.max(radius1, radius2) * Math.PI * 2 * subDiv);

        // The coordinate zero vector
        Vector3f start = new Vector3f(x1,y1,z1);
        Vector3f end = new Vector3f(x2,y2,z2);
        // The normal vector (the new Z)
        Vector3f newZ = new Vector3f(x2 - x1, y2 - y1, z2 - z1);
        // The perpendicular vectors (the new X and Y)
        Vector3f newX = getPerpendicularVector(newZ).normalize();
        Vector3f newY = new Vector3f(newZ).cross(newX).normalize();

        float lineLength = newZ.length();

        List<Vector3f> startVertices = new ArrayList<>();
        List<Vector3f> endVertices = new ArrayList<>();

        for (int i = 0; i < nPoints; i++) {
            double phi = (2 * Math.PI) * (1.0 * i / nPoints);
            float ix1 = (float) (Math.sin(phi) * radius1);
            float ix2 = (float) (Math.sin(phi) * radius2);
            float iy1 = (float) (Math.cos(phi) * radius1);
            float iy2 = (float) (Math.cos(phi) * radius2);

            Vector3f p1 = new Vector3f(start.x + ix1 * newX.x + iy1 * newY.x,
                    start.y + ix1 * newX.y + iy1 * newY.y,
                    start.z + ix1 * newX.z + iy1 * newY.z);
            Vector3f p2 = new Vector3f(end.x + ix2 * newX.x + iy2 * newY.x,
                    end.y + ix2 * newX.y + iy2 * newY.y,
                    end.z + ix2 * newX.z + iy2 * newY.z);

            startVertices.add(p1);
            endVertices.add(p2);
        }

        TFloatList vertices = new TFloatArrayList();

        for (int i = 0; i < nPoints; i++) {
            Vector3f p1 = startVertices.get(i);
            Vector3f p2 = endVertices.get(i);
            Vector3f p4 = startVertices.get((i + 1) % nPoints);
            Vector3f p3 = endVertices.get((i + 1) % nPoints);

            // Triangle 1
            vertices.add(p1.x);
            vertices.add(p1.y);
            vertices.add(p1.z);
            vertices.add(p2.x);
            vertices.add(p2.y);
            vertices.add(p2.z);
            vertices.add(p3.x);
            vertices.add(p3.y);
            vertices.add(p3.z);

            // Triangle 2
            vertices.add(p1.x);
            vertices.add(p1.y);
            vertices.add(p1.z);
            vertices.add(p3.x);
            vertices.add(p3.y);
            vertices.add(p3.z);
            vertices.add(p4.x);
            vertices.add(p4.y);
            vertices.add(p4.z);
        }

        return vertices.toArray();
    }

    public static float[] generateUVSphereVertices(float centerX, float centerY, float centerZ, float radiusX, float radiusY, float radiusZ, int subDiv) {

        TFloatList vertices = new TFloatArrayList();

        double circXY = Math.PI * (radiusX + radiusY);
        int numDivisionsLatitude = (int) Math.max(radiusZ * subDiv, 1);
        int numDivisionsLongtitude = (int)Math.max(circXY * subDiv, 1);

        double latitudeStep = Math.PI / numDivisionsLatitude; // North-South
        double longitudeStep = 2 * Math.PI / numDivisionsLongtitude; // East-West

        for (int lat = 0; lat < numDivisionsLatitude; lat++) {
            double theta = lat * latitudeStep;
            double thetaNext = (lat + 1) * latitudeStep;

            for (int lon = 0; lon < numDivisionsLongtitude; lon++) {
                double phi = lon * longitudeStep;
                double phiNext = (lon + 1) * longitudeStep;

                // Calculate the vertex positions of the current and adjacent latitudes and longitudes
                double x = radiusX * Math.sin(theta) * Math.cos(phi);
                double y = radiusY * Math.sin(theta) * Math.sin(phi);
                double z = radiusZ * Math.cos(theta);

                double xNext = radiusX * Math.sin(thetaNext) * Math.cos(phi);
                double yNext = radiusY * Math.sin(thetaNext) * Math.sin(phi);
                double zNext = radiusZ * Math.cos(thetaNext);

                double xLonNext = radiusX * Math.sin(theta) * Math.cos(phiNext);
                double yLonNext = radiusY * Math.sin(theta) * Math.sin(phiNext);
                double zLonNext = radiusZ * Math.cos(theta);

                double xNextLonNext = radiusX * Math.sin(thetaNext) * Math.cos(phiNext);
                double yNextLonNext = radiusY * Math.sin(thetaNext) * Math.sin(phiNext);
                double zNextLonNext = radiusZ * Math.cos(thetaNext);

                // Triangle 1
                vertices.add((float) (x + centerX));
                vertices.add((float) (y + centerY));
                vertices.add((float) (z + centerZ));
                vertices.add((float) (xNext + centerX));
                vertices.add((float) (yNext + centerY));
                vertices.add((float) (zNext + centerZ));
                vertices.add((float) (xLonNext + centerX));
                vertices.add((float) (yLonNext + centerY));
                vertices.add((float) (zLonNext + centerZ));

                // Triangle 2
                vertices.add((float) (xNext + centerX));
                vertices.add((float) (yNext + centerY));
                vertices.add((float) (zNext + centerZ));
                vertices.add((float) (xLonNext + centerX));
                vertices.add((float) (yLonNext + centerY));
                vertices.add((float) (zLonNext + centerZ));
                vertices.add((float) (xNextLonNext + centerX));
                vertices.add((float) (yNextLonNext + centerY));
                vertices.add((float) (zNextLonNext + centerZ));
            }
        }

        return vertices.toArray();
    }

}
