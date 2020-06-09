package org.hkijena.acaq5.extensions.parameters.editors;

import javax.swing.*;

/**
 * Used by {@link EnumParameterEditorUI} to extract icons and other information for its enum value
 */
public interface EnumItemInfo {

    /**
     * Extracts an icon for the enum value
     *
     * @param value the enum value
     * @return the icon of the enum value
     */
    Icon getIcon(Object value);

    /**
     * Extracts a custom label for the enum value
     *
     * @param value the enum value
     * @return custom label
     */
    String getLabel(Object value);

    /**
     * Extracts a custom tooltip for the enum value
     *
     * @param value the enum value
     * @return custom tooltip
     */
    String getTooltip(Object value);
}
