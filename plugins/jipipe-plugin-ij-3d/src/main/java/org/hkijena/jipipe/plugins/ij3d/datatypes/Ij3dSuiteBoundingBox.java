package org.hkijena.jipipe.plugins.ij3d.datatypes;

import mcib3d.geom.Object3D;
import mcib3d.geom.ObjectCreator3D;

public class Ij3dSuiteBoundingBox {

    private int xMin, xMax, yMin, yMax, zMin, zMax;

    public Ij3dSuiteBoundingBox() {

    }

    public Ij3dSuiteBoundingBox(Object3D object3D) {
        int[] boundingBox = object3D.getBoundingBox();
        this.xMin = boundingBox[0];
        this.xMax = boundingBox[1];
        this.yMin = boundingBox[2];
        this.yMax = boundingBox[3];
        this.zMin = boundingBox[4];
        this.zMax = boundingBox[5];
    }

    public Ij3dSuiteBoundingBox(Ij3dSuiteBoundingBox other) {
        this.xMin = other.xMin;
        this.xMax = other.xMax;
        this.yMin = other.yMin;
        this.yMax = other.yMax;
        this.zMin = other.zMin;
        this.zMax = other.zMax;
    }

    public static Ij3dSuiteBoundingBox intersect(Ij3dSuiteBoundingBox b1, Ij3dSuiteBoundingBox b2) {
        // Check if the bounding boxes intersect
        if (!b1.intersects(b2)) {
            return null;
        }
        
        // Calculate the intersection bounds
        int xMin = Math.max(b1.getxMin(), b2.getxMin());
        int xMax = Math.min(b1.getxMax(), b2.getxMax());
        int yMin = Math.max(b1.getyMin(), b2.getyMin());
        int yMax = Math.min(b1.getyMax(), b2.getyMax());
        int zMin = Math.max(b1.getzMin(), b2.getzMin());
        int zMax = Math.min(b1.getzMax(), b2.getzMax());
        
        // Create and return the intersection bounding box
        Ij3dSuiteBoundingBox intersection = new Ij3dSuiteBoundingBox();
        intersection.setxMin(xMin);
        intersection.setxMax(xMax);
        intersection.setyMin(yMin);
        intersection.setyMax(yMax);
        intersection.setzMin(zMin);
        intersection.setzMax(zMax);
        return intersection;
    }

    public int getxMin() {
        return xMin;
    }

    public void setxMin(int xMin) {
        this.xMin = xMin;
    }

    public int getxMax() {
        return xMax;
    }

    public void setxMax(int xMax) {
        this.xMax = xMax;
    }

    public int getyMin() {
        return yMin;
    }

    public void setyMin(int yMin) {
        this.yMin = yMin;
    }

    public int getyMax() {
        return yMax;
    }

    public void setyMax(int yMax) {
        this.yMax = yMax;
    }

    public int getzMin() {
        return zMin;
    }

    public void setzMin(int zMin) {
        this.zMin = zMin;
    }

    public int getzMax() {
        return zMax;
    }

    public void setzMax(int zMax) {
        this.zMax = zMax;
    }

    public double getVolume() {
        return (xMax -  xMin) * (yMax - yMin) * (zMax - zMin);
    }

    public boolean intersects(Ij3dSuiteBoundingBox other) {
        return this.xMax >= other.xMin && this.xMin <= other.xMax &&
               this.yMax >= other.yMin && this.yMin <= other.yMax &&
               this.zMax >= other.zMin && this.zMin <= other.zMax;
    }

    public Ij3dSuiteRoi toRoi() {
        ObjectCreator3D creator3D = new ObjectCreator3D(xMax + 1, yMax + 1, zMax + 1);
        creator3D.createBrick(xMin, xMax, yMin, yMax, zMin, zMax, 255);
        Ij3dSuiteRoi roi3D = new Ij3dSuiteRoi();
        roi3D.setObject3D(creator3D.getObject3DVoxels(roi3D.getObject3D().getValue()));
        return roi3D;
    }
}
