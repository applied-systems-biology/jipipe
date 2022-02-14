package org.hkijena.jipipe.api.parameters;

import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.awt.*;
import java.util.List;

/**
 * A class that generates parameters
 */
public interface JIPipeParameterGenerator {
    /**
     * @return name displayed in the menu
     */
    String getName();

    /**
     * @return description displayed in the menu
     */
    String getDescription();

    /**
     * Generates the values
     *
     * @param <T>       the generated field type
     * @param workbench the workbench
     * @param parent    the parent component for any dialogs
     * @param klass     the generated field class
     * @return the generated values
     */
    <T> List<T> generate(JIPipeWorkbench workbench, Component parent, Class<T> klass);
}
