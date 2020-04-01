package org.hkijena.acaq5.api.parameters;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Parameter access for one entry in {@link CollectionParameter}
 */
public class CollectionEntryParameterAccess<T> implements ACAQParameterAccess {

    private List<T> entryList;
    private Class<T> entryType;
    private int index;
    private ACAQParameterAccess parent;

    /**
     * Creates a new instance
     * @param parent the parent access
     * @param entryList the list
     * @param entryType type of the content
     * @param index the list entry index
     */
    public CollectionEntryParameterAccess(ACAQParameterAccess parent, List<T> entryList, Class<T> entryType, int index) {
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
        return "Item " + (index + 1);
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
    public <U> U get() {
        return (U)entryList.get(index);
    }

    @Override
    public <U> boolean set(U value) {
        entryList.set(index, (T)value);
        return true;
    }

    @Override
    public ACAQParameterHolder getParameterHolder() {
        return parent.getParameterHolder();
    }

    @Override
    public String getHolderName() {
        return parent.getHolderName();
    }

    @Override
    public void setHolderName(String name) {

    }

    @Override
    public String getHolderDescription() {
        return parent.getHolderDescription();
    }

    @Override
    public void setHolderDescription(String description) {

    }
}
