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
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;

public class Vector2iParameter implements Vector2Parameter {
    private int x;
    private int y;

    public Vector2iParameter() {
    }

    public Vector2iParameter(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Vector2iParameter(Vector2iParameter other) {
        this.x = other.x;
        this.y = other.y;
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

    @Override
    public String toString() {
        return "[" + x + ", " + y + "]";
    }

    public static class List extends ListParameter<Vector2iParameter> {
        public List() {
            super(Vector2iParameter.class);
        }

        public List(Vector2iParameter.List other) {
            super(Vector2iParameter.class);
            for (Vector2iParameter parameter : other) {
                add(new Vector2iParameter(parameter));
            }
        }
    }
}
