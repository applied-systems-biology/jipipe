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

package org.hkijena.jipipe.plugins.parameters.library.primitives.vectors;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class Vector3dParameter implements Vector3Parameter {
    private double x;
    private double y;
    private double z;

    public Vector3dParameter() {
    }

    public Vector3dParameter(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3dParameter(Vector3dParameter other) {
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
}
