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

package org.hkijena.jipipe.api.data;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.python.core.PyDictionary;
import org.python.core.PyString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A single annotation
 */
public class JIPipeAnnotation implements Comparable<JIPipeAnnotation> {

    private String name;
    private String value;

    /**
     * Creates an empty instance
     */
    public JIPipeAnnotation() {
    }

    /**
     * Initializes a new instance
     *
     * @param name  the name
     * @param value the value
     */
    public JIPipeAnnotation(String name, String value) {
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
    public int compareTo(JIPipeAnnotation o) {
        return NaturalOrderComparator.INSTANCE.compare(getValue(), o.getValue());
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
        JIPipeAnnotation that = (JIPipeAnnotation) o;
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
    public boolean nameEquals(JIPipeAnnotation other) {
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
    public static boolean nameEquals(JIPipeAnnotation lhs, JIPipeAnnotation rhs) {
        if (lhs == null || rhs == null)
            return false;
        return lhs.nameEquals(rhs);
    }

    /**
     * Converts a map of annotations into a Python dictionary
     *
     * @param annotationMap the annotations
     * @return the Python dictionary
     */
    public static PyDictionary annotationMapToPython(Map<String, JIPipeAnnotation> annotationMap) {
        PyDictionary annotationDict = new PyDictionary();
        for (Map.Entry<String, JIPipeAnnotation> entry : annotationMap.entrySet()) {
            annotationDict.put(new PyString(entry.getKey()), new PyString(entry.getValue().getValue()));
        }
        return annotationDict;
    }

    /**
     * Sets annotations from a Python dictionary
     *
     * @param annotationDict the dictionary
     * @param target         the target map
     */
    public static void setAnnotationsFromPython(PyDictionary annotationDict, Map<String, JIPipeAnnotation> target) {
        for (Object key : annotationDict.keys()) {
            String keyString = "" + key;
            String valueString = "" + annotationDict.get(key);
            target.put(keyString, new JIPipeAnnotation(keyString, valueString));
        }
    }

    /**
     * Sets annotations from a Python dictionary
     *
     * @param annotationDict the dictionary
     */
    public static List<JIPipeAnnotation> extractAnnotationsFromPython(PyDictionary annotationDict) {
        List<JIPipeAnnotation> result = new ArrayList<>();
        for (Object key : annotationDict.keys()) {
            String keyString = "" + key;
            String valueString = "" + annotationDict.get(key);
            result.add(new JIPipeAnnotation(keyString, valueString));
        }
        return result;
    }

    /**
     * Converts a list of annotations into a Python dictionary
     *
     * @param annotations the annotations
     * @return the Python dictionary
     */
    public static PyDictionary annotationListToPython(List<JIPipeAnnotation> annotations) {
        PyDictionary annotationDict = new PyDictionary();
        for (JIPipeAnnotation annotation : annotations) {
            annotationDict.put(new PyString(annotation.getName()), new PyString(annotation.getValue()));
        }
        return annotationDict;
    }
}
