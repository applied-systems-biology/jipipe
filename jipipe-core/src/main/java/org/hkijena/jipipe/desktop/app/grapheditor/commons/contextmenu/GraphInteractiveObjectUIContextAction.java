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
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;

import javax.swing.*;
import java.util.Set;

/**
 * An action that is applied one or multiple algorithms
 */
public interface GraphInteractiveObjectUIContextAction {

    /**
     * Indicates that a separator is created
     */
    GraphInteractiveObjectUIContextAction SEPARATOR = null;

    /**
     * Returns if the action shows up
     *
     * @param selection the list of algorithm UIs
     * @return if the action shows up
     */
    boolean matches(Set<JIPipeDesktopGraphInteractiveObjectUI> selection);

    /**
     * Runs the workload
     *
     * @param canvasUI  the canvas that contains all algorithm UIs
     * @param selection the current selection of algorithms
     */
    void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphInteractiveObjectUI> selection);

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
     * @deprecated the multi selection panel was removed
     */
    @Deprecated
    default boolean showInMultiSelectionPanel() {
        return true;
    }

    /**
     * Determines if the item should be displayed in the context toolbar
     *
     * @return if the item should be displayed in the context toolbar
     */
    default boolean isDisplayedInToolbar() {
        return false;
    }

    /**
     * Returns the id of the shortcut action in the shortcut registry that
     * triggers this action, or null when the action has no keyboard shortcut.
     * <p>
     * Implementing classes declare their id as {@code SHORTCUT_ACTION_ID}
     * (referencing the matching
     * {@link org.hkijena.jipipe.api.shortcuts.JIPipeShortcutActions} constant)
     * and return it here. The effective keystrokes are resolved from the
     * shortcut registry via
     * {@link org.hkijena.jipipe.api.shortcuts.JIPipeShortcutRegistry#bindingFor(String, org.hkijena.jipipe.api.shortcuts.KeymapSettings)},
     * not from this interface.
     *
     * @return the shortcut action id or null
     */
    default String getShortcutActionId() {
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
