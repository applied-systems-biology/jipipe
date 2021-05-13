package org.hkijena.jipipe.api.environments;

import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;

import javax.swing.*;

public interface ExternalEnvironment extends JIPipeParameterCollection, JIPipeValidatable {
    /**
     * Returns the icon displayed in the UI for the current status
     * @return the icon
     */
    Icon getIcon();

    /**
     * Returns a status string displayed next to the icon in the UI
     * @return the status
     */
    String getStatus();

    /**
     * Returns more detailed information (e.g., the executed script environment path).
     * Displayed inside a text field.
     * @return the info string
     */
    String getInfo();
}
