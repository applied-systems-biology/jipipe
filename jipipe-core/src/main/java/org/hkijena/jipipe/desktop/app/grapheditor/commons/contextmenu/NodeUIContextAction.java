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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu;

import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;

import javax.swing.*;
import java.util.Set;

/**
 * An action that is applied one or multiple algorithms
 */
public interface NodeUIContextAction {

    /**
     * Indicates that a separator is created
     */
    NodeUIContextAction SEPARATOR = null;

    /**
     * Returns if the action shows up
     *
     * @param selection the list of algorithm UIs
     * @return if the action shows up
     */
    boolean matches(Set<JIPipeDesktopGraphNodeUI> selection);

    /**
     * Runs the workload
     *
     * @param canvasUI  the canvas that contains all algorithm UIs
     * @param selection the current selection of algorithms
     */
    void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphNodeUI> selection);

    /**
     * @return the name
     */
    String getName();

    /**
     * @return the description
     */
    String getDescription();

    /**
     * @return the icon
     */
    Icon getIcon();

    /**
     * Determines if the item is never shown (even if it applies)
     * Keyboard shortcuts still work
     *
     * @return if the item is hidden
     */
    default boolean isHidden() {
        return false;
    }

    /**
     * Determines if an item should be disabled or removed if it does not match
     *
     * @return if an item should be disabled or removed if it does not match
     */
    default boolean disableOnNonMatch() {
        return false;
    }

    /**
     * Determines if the item should be shown in the multi-node selection panel
     *
     * @return if the item should be shown in the multi-node selection panel
     */
    default boolean showInMultiSelectionPanel() {
        return true;
    }

    /**
     * Returns an optional keyboard shortcut. Can be null
     *
     * @return Keyboard shortcut or null
     */
    default KeyStroke getKeyboardShortcut() {
        return null;
    }

    /**
     * Determines if the item is displayed in the compartment graph.
     *
     * @return if the item is displayed
     */
    default boolean showInCompartmentGraph() {
        return false;
    }

    /**
     * Determines if the item is displayed in the graph compartment (project/group/extension editor).
     *
     * @return if the item is displayed
     */
    default boolean showInGraphCompartment() {
        return true;
    }
}
