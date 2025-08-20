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

package org.hkijena.jipipe.api.parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Standard implementation of {@link JIPipeParameterTypeInfo}
 */
public class JIPipeDefaultMutableParameterTypeInfo implements JIPipeParameterTypeInfo {

    private String id;
    private Class<?> fieldClass;
    private Supplier<Object> newInstanceGenerator;
    private Function<Object, Object> duplicateFunction;
    private String name;
    private String description;
    private List<JIPipeParameterTypeAllowedValueInfo> allowedValues = new ArrayList<>();
    private JIPipeParameterArchetype archetype = JIPipeParameterArchetype.Unknown;


    /**
     * @param id                   the unique ID
     * @param fieldClass           the parameter class
     * @param newInstanceGenerator a function that generates a new instance
     * @param duplicateFunction    a function that creates a deep copy
     * @param name                 the name
     * @param description          the description
     * @param archetype            the archetype
     */
    public JIPipeDefaultMutableParameterTypeInfo(String id, Class<?> fieldClass, Supplier<Object> newInstanceGenerator, Function<Object, Object> duplicateFunction, String name, String description, JIPipeParameterArchetype archetype) {
        this.id = id;
        this.fieldClass = fieldClass;
        this.newInstanceGenerator = newInstanceGenerator;
        this.duplicateFunction = duplicateFunction;
        this.name = name;
        this.description = description;
        this.archetype = archetype;
    }

    @Override
    public Object newInstance() {
        return newInstanceGenerator.get();
    }

    @Override
    public Object duplicate(Object original) {
        if (original == null)
            return newInstance();
        return duplicateFunction.apply(original);
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public Class<?> getFieldClass() {
        return fieldClass;
    }

    public void setFieldClass(Class<?> fieldClass) {
        this.fieldClass = fieldClass;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Supplier<Object> getNewInstanceGenerator() {
        return newInstanceGenerator;
    }

    public void setNewInstanceGenerator(Supplier<Object> newInstanceGenerator) {
        this.newInstanceGenerator = newInstanceGenerator;
    }

    public Function<Object, Object> getDuplicateFunction() {
        return duplicateFunction;
    }

    public void setDuplicateFunction(Function<Object, Object> duplicateFunction) {
        this.duplicateFunction = duplicateFunction;
    }

    @Override
    public List<JIPipeParameterTypeAllowedValueInfo> getAllowedValues() {
        return allowedValues;
    }

    public void setAllowedValues(List<JIPipeParameterTypeAllowedValueInfo> allowedValues) {
        this.allowedValues = allowedValues;
    }

    @Override
    public JIPipeParameterArchetype getArchetype() {
        return archetype;
    }

    public void setArchetype(JIPipeParameterArchetype archetype) {
        this.archetype = archetype;
    }
}
