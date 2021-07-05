package org.hkijena.jipipe.api.parameters;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Writes values into multiple parameter accesses
 * The displayed value is obtained from the first entry
 */
public class JIPipeMultiParameterAccess implements JIPipeParameterAccess {

    private final List<JIPipeParameterAccess> accessList;

    public JIPipeMultiParameterAccess(List<JIPipeParameterAccess> accessList) {
        this.accessList = accessList;
    }

    public JIPipeParameterAccess getFirstAccess() {
        return accessList.get(0);
    }

    @Override
    public String getKey() {
        return getFirstAccess().getKey();
    }

    @Override
    public String getName() {
        return getFirstAccess().getName();
    }

    @Override
    public String getDescription() {
        return getFirstAccess().getDescription();
    }

    @Override
    public boolean isHidden() {
        return getFirstAccess().isHidden();
    }

    @Override
    public boolean isImportant() {
        return getFirstAccess().isImportant();
    }

    @Override
    public <T extends Annotation> T getAnnotationOfType(Class<T> klass) {
        return getFirstAccess().getAnnotationOfType(klass);
    }

    @Override
    public Class<?> getFieldClass() {
        return getFirstAccess().getFieldClass();
    }

    @Override
    public <T> T get(Class<T> klass) {
        return getFirstAccess().get(klass);
    }

    @Override
    public <T> boolean set(T value) {
        boolean success = true;
        for (JIPipeParameterAccess access : accessList) {
            success &= access.set(value);
        }
        return success;
    }

    @Override
    public JIPipeParameterCollection getSource() {
        return getFirstAccess().getSource();
    }

    @Override
    public double getPriority() {
        return getFirstAccess().getPriority();
    }

    @Override
    public String getShortKey() {
        return getFirstAccess().getShortKey();
    }

    @Override
    public int getUIOrder() {
        return getFirstAccess().getUIOrder();
    }

    public List<JIPipeParameterAccess> getAccessList() {
        return accessList;
    }
}
