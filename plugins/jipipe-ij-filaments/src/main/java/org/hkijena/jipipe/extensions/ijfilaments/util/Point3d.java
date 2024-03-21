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

package org.hkijena.jipipe.extensions.ijfilaments.util;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.hkijena.jipipe.extensions.parameters.library.quantities.Quantity;
import org.scijava.vecmath.Vector3d;

import java.util.Objects;

public class Point3d {
    private double x;
    private double y;
    private double z;

    public Point3d() {
    }

    public Point3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Point3d(Point3d other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    @JsonGetter("x")
    public double getX() {
        return x;
    }

    @JsonSetter("x")
    public void setX(double x) {
        this.x = x;
    }

    @JsonGetter("y")
    public double getY() {
        return y;
    }

    @JsonSetter("y")
    public void setY(double y) {
        this.y = y;
    }

    @JsonGetter("z")
    public double getZ() {
        return z;
    }

    @JsonSetter("z")
    public void setZ(double z) {
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point3d point3d = (Point3d) o;
        return x == point3d.x && y == point3d.y && z == point3d.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    /**
     * Calculates the distances between two locations
     * Accounts for negative Z/C/T locations (location everywhere)
     *
     * @param other the other location
     * @return the euclidean distance
     */
    public double distanceTo(Point3d other) {
        return Math.sqrt(Math.pow(x - other.getX(), 2) + Math.pow(y - other.getY(), 2)
                + Math.pow(z - other.getZ(), 2));
    }

    public Vector3d toSciJavaVector3d() {
        return new Vector3d(x, y, z);
    }

    public Vector3D toApacheVector3d() {
        return new Vector3D(x, y, z);
    }

    public Vector3d toNormalizedVector3d() {
        Vector3d vector3d = toSciJavaVector3d();
        vector3d.normalize();
        return vector3d;
    }

    public Vector3d pixelsToUnit(Quantity vsx, Quantity vsy, Quantity vsz, String unit) {
        vsx = vsx.convertTo(unit);
        vsy = vsy.convertTo(unit);
        vsz = vsz.convertTo(unit);
        return new Vector3d(vsx.getValue() * x,
                vsy.getValue() * y,
                vsz.getValue() * z);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
