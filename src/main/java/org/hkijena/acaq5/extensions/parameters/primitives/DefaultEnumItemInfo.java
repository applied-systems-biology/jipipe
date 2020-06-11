package org.hkijena.acaq5.extensions.parameters.primitives;

import javax.swing.*;

/**
 * Default implementation of {@link EnumItemInfo}
 */
public class DefaultEnumItemInfo implements EnumItemInfo {
    @Override
    public Icon getIcon(Object value) {
        return null;
    }

    @Override
    public String getLabel(Object value) {
        if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        } else {
            return "<None selected>";
        }
    }

    @Override
    public String getTooltip(Object value) {
        return null;
    }
}
