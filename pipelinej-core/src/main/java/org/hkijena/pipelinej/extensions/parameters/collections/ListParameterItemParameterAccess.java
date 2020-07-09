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

package org.hkijena.pipelinej.extensions.parameters.collections;

import org.hkijena.pipelinej.api.parameters.ACAQParameterAccess;
import org.hkijena.pipelinej.api.parameters.ACAQParameterCollection;
import org.hkijena.pipelinej.api.parameters.ACAQParameterVisibility;
import org.scijava.Priority;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Parameter access for one entry in {@link ListParameter}
 */
public class ListParameterItemParameterAccess<T> implements ACAQParameterAccess {

    private List<T> entryList;
    private Class<T> entryType;
    private int index;
    private ACAQParameterAccess parent;

    /**
     * Creates a new instance
     *
     * @param parent    the parent access
     * @param entryList the list
     * @param entryType type of the content
     * @param index     the list entry index
     */
    public ListParameterItemParameterAccess(ACAQParameterAccess parent, List<T> entryList, Class<T> entryType, int index) {
        this.entryList = entryList;
        this.entryType = entryType;
        this.index = index;
        this.parent = parent;
    }

    @Override
    public String getKey() {
        return "" + index;
    }

    @Override
    public String getName() {
        return "Item #" + (index + 1);
    }

    @Override
    public String getDescription() {
        return "Item in the collection";
    }

    @Override
    public ACAQParameterVisibility getVisibility() {
        return parent.getVisibility();
    }

    @Override
    public <T extends Annotation> T getAnnotationOfType(Class<T> klass) {
        return parent.getAnnotationOfType(klass);
    }

    @Override
    public Class<?> getFieldClass() {
        return entryType;
    }

    @Override
    public <U> U get(Class<U> klass) {
        return (U) entryList.get(index);
    }

    @Override
    public <U> boolean set(U value) {
        entryList.set(index, (T) value);
        return true;
    }

    @Override
    public ACAQParameterCollection getSource() {
        return parent.getSource();
    }

    @Override
    public double getPriority() {
        return Priority.NORMAL;
    }

    @Override
    public String getShortKey() {
        return getKey();
    }

    @Override
    public int getUIOrder() {
        return 0;
    }
}
