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

package org.hkijena.jipipe.desktop.app.grapheditor.commons;

import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

import javax.swing.*;
import java.util.Set;

/**
 * Base class for all interactive/selectable objects in a {@link JIPipeDesktopGraphCanvasUI}
 */
public interface JIPipeDesktopGraphInteractiveObjectUI {
    /**
     * Gets all associated nodes
     *
     * @return the associated nodes
     */
    Set<JIPipeGraphNode> getNodes();

    /**
     * Instructs the UI to update its display
     *
     * @param command the command
     */
    void updateView(JIPipeDesktopGraphInteractiveObjectUIUpdateViewCommand command);

    /**
     * Display name used for object manager
     * @return the display name
     */
    String getDisplayName();

    /**
     * Description for object manager
     * @return the description
     */
    String getDescription();

    /**
     * Icon for object manager
     * @return the icon
     */
    Icon getIcon();
}
