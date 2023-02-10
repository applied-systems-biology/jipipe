package org.hkijena.jipipe.extensions.ijfilaments.util;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.scijava.vecmath.Vector3d;

import java.util.Objects;

public class FilamentLocation {
    private int x;
    private int y;
    private int z;
    private int c = -1;

    private int t = -1;

    public FilamentLocation() {
    }

    public FilamentLocation(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public FilamentLocation(int x, int y, int z, int c, int t) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.c = c;
        this.t = t;
    }

    public FilamentLocation(FilamentLocation other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        this.c = other.c;
        this.t = other.t;
    }

    @JsonGetter("x")
    public int getX() {
        return x;
    }

    @JsonSetter("x")
    public void setX(int x) {
        this.x = x;
    }

    @JsonGetter("y")
    public int getY() {
        return y;
    }

    @JsonSetter("y")
    public void setY(int y) {
        this.y = y;
    }

    @JsonGetter("z")
    public int getZ() {
        return z;
    }

    @JsonSetter("z")
    public void setZ(int z) {
        this.z = z;
    }

    @JsonGetter("c")
    public int getC() {
        return c;
    }

    @JsonSetter("c")
    public void setC(int c) {
        this.c = c;
    }

    @JsonGetter("t")
    public int getT() {
        return t;
    }

    @JsonSetter("t")
    public void setT(int t) {
        this.t = t;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilamentLocation that = (FilamentLocation) o;
        return x == that.x && y == that.y && z == that.z && c == that.c && t == that.t;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, c, t);
    }

    /**
     * Calculates the distances between two locations
     * Accounts for negative Z/C/T locations (location everywhere)
     * @param other the other location
     * @return the euclidean distance
     */
    public double distanceTo(FilamentLocation other) {
        int z1 = other.z;
        int c1 = other.c;
        int t1 = other.t;
        if(z1 < 0)
            z1 = z;
        if(c1 < 0)
            c1 = c;
        if(t1 < 0)
            t1 = t;
        return Math.sqrt(Math.pow(x - other.getX(), 2) + Math.pow(y - other.getY(), 2)
                + Math.pow(z - z1, 2) + Math.pow(c - c1, 2) + Math.pow(t - t1, 2));
    }

    public Vector3d toVector3d() {
        return new Vector3d(x,y,z);
    }

    public Vector3d toNormalizedVector3d() {
        Vector3d vector3d = toVector3d();
        vector3d.normalize();
        return vector3d;
    }
}
