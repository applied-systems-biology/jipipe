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

package org.hkijena.jipipe.api.annotation;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.python.core.PyDictionary;
import org.python.core.PyString;

import java.io.IOException;
import java.util.*;

/**
 * A single annotation
 */
public class JIPipeTextAnnotation implements Comparable<JIPipeTextAnnotation>, JIPipeAnnotation {

    private String name;
    private String value;

    /**
     * Creates an empty instance
     */
    public JIPipeTextAnnotation() {
    }

    /**
     * Initializes a new instance
     *
     * @param name  the name
     * @param value the value
     */
    public JIPipeTextAnnotation(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Creates a new instance from an array.
     * Automatically collapses the array if it contains only one item.
     * If the array is empty, the value is empty.
     * The array is converted into JSON
     *
     * @param name   the name
     * @param values the values
     */
    public JIPipeTextAnnotation(String name, Collection<String> values) {
        this(name, values.isEmpty() ? "" : (values.size() == 1 ? values.iterator().next() : JsonUtils.toJsonString(values)));
    }

    /**
     * Creates a new instance from an array.
     * Automatically collapses the array if it contains only one item.
     * If the array is empty, the value is empty.
     * The array is converted into JSON
     *
     * @param name   the name
     * @param values the values
     */
    public JIPipeTextAnnotation(String name, String[] values) {
        this(name, values.length == 0 ? "" : (values.length == 1 ? values[0] : JsonUtils.toJsonString(values)));
    }

    /**
     * Returns true if both have the same name.
     * Returns false if either are null
     *
     * @param lhs the first
     * @param rhs the second
     * @return if both have the same name and are not null
     */
    public static boolean nameEquals(JIPipeTextAnnotation lhs, JIPipeTextAnnotation rhs) {
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
    public static PyDictionary annotationMapToPython(Map<String, JIPipeTextAnnotation> annotationMap) {
        PyDictionary annotationDict = new PyDictionary();
        for (Map.Entry<String, JIPipeTextAnnotation> entry : annotationMap.entrySet()) {
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
    public static void setAnnotationsFromPython(PyDictionary annotationDict, Map<String, JIPipeTextAnnotation> target) {
        for (Object key : annotationDict.keys()) {
            String keyString = "" + key;
            String valueString = "" + annotationDict.get(key);
            target.put(keyString, new JIPipeTextAnnotation(keyString, valueString));
        }
    }

    /**
     * Sets annotations from a Python dictionary
     *
     * @param annotationDict the dictionary
     */
    public static List<JIPipeTextAnnotation> extractAnnotationsFromPython(PyDictionary annotationDict) {
        List<JIPipeTextAnnotation> result = new ArrayList<>();
        for (Object key : annotationDict.keys()) {
            String keyString = "" + key;
            String valueString = "" + annotationDict.get(key);
            result.add(new JIPipeTextAnnotation(keyString, valueString));
        }
        return result;
    }

    /**
     * Converts a list of annotations into a Python dictionary
     *
     * @param annotations the annotations
     * @return the Python dictionary
     */
    public static PyDictionary annotationListToPython(Collection<JIPipeTextAnnotation> annotations) {
        PyDictionary annotationDict = new PyDictionary();
        for (JIPipeTextAnnotation annotation : annotations) {
            annotationDict.put(new PyString(annotation.getName()), new PyString(annotation.getValue()));
        }
        return annotationDict;
    }

    /**
     * Converts a set of annotations to a map
     *
     * @param annotations the annotations
     * @return annotations as map
     */
    public static Map<String, String> annotationListToMap(Collection<JIPipeTextAnnotation> annotations, JIPipeTextAnnotationMergeMode mergeStrategy) {
        Map<String, String> result = new HashMap<>();
        for (JIPipeTextAnnotation annotation : mergeStrategy.merge(annotations)) {
            result.put(annotation.getName(), annotation.getValue());
        }
        return result;
    }

    /**
     * Converts an annotation map to a list
     *
     * @param map the map
     * @return the annotations
     */
    public static List<JIPipeTextAnnotation> mapToAnnotationList(Map<String, String> map) {
        List<JIPipeTextAnnotation> annotations = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            annotations.add(new JIPipeTextAnnotation(entry.getKey(), entry.getValue()));
        }
        return annotations;
    }

    @JsonGetter("name")
    @Override
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

    /**
     * Returns the value as array or an array with the value if it is not an array.
     *
     * @return the value inside an array or the value as array
     */
    public String[] getArray() {
        if (StringUtils.isNullOrEmpty(value))
            return new String[0];
        if (value.contains("[") && value.contains("]")) {
            try {
                return JsonUtils.getObjectMapper().readerFor(String[].class).readValue(value);
            } catch (IOException e) {
                return new String[]{value};
            }
        } else {
            return new String[]{value};
        }
    }

    @Override
    public int compareTo(JIPipeTextAnnotation o) {
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
        JIPipeTextAnnotation that = (JIPipeTextAnnotation) o;
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
    public boolean nameEquals(JIPipeTextAnnotation other) {
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
     * Returns true if the value is an array
     *
     * @return if the value is an array
     */
    public boolean isArray() {
        return getArray().length > 1;
    }
}
