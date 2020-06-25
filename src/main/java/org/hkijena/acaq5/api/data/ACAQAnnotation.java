/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.acaq5.api.data;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Objects;

/**
 * A single annotation
 */
public class ACAQAnnotation implements Comparable<ACAQAnnotation> {

    private String name;
    private String value;

    /**
     * Creates an empty instance
     */
    public ACAQAnnotation() {
    }

    /**
     * Initializes a new instance
     *
     * @param name  the name
     * @param value the value
     */
    public ACAQAnnotation(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @JsonGetter("name")
    public String getName() {
        if (name == null)
            return "";
        return name;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonGetter("value")
    public String getValue() {
        if (value == null)
            return "";
        return value;
    }

    @JsonSetter("value")
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public int compareTo(ACAQAnnotation o) {
        return getValue().compareTo(o.getValue());
    }

    /**
     * Returns true if the name and value are equal
     *
     * @param o the other
     * @return if the name and value are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ACAQAnnotation that = (ACAQAnnotation) o;
        return Objects.equals(getName(), that.getName()) &&
                Objects.equals(getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getValue());
    }

    /**
     * Returns true if the names are equal
     *
     * @param other the other
     * @return if the names are equal
     */
    public boolean nameEquals(ACAQAnnotation other) {
        return Objects.equals(getName(), other.getName());
    }

    /**
     * Returns true if the name equals the string
     *
     * @param name the string
     * @return if the name equals the string
     */
    public boolean nameEquals(String name) {
        return Objects.equals(getName(), name);
    }

    @Override
    public String toString() {
        return name + "=" + value;
    }

    /**
     * Returns true if both have the same name.
     * Returns false if either are null
     *
     * @param lhs the first
     * @param rhs the second
     * @return if both have the same name and are not null
     */
    public static boolean nameEquals(ACAQAnnotation lhs, ACAQAnnotation rhs) {
        if (lhs == null || rhs == null)
            return false;
        return lhs.nameEquals(rhs);
    }
}
