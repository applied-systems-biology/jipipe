package org.hkijena.acaq5.ui.grapheditor.contextmenu;

import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;

import javax.swing.*;

/**
 * A set of features that are installed into {@link org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI} to provide context actions
 */
public interface ACAQAlgorithmUIContextMenuFeature {
    /**
     * Installs the feature
     *
     * @param ui          the ui
     * @param contextMenu the menu
     */
    void install(ACAQAlgorithmUI ui, JPopupMenu contextMenu);

    /**
     * Method that updates items
     *
     * @param ui the ui
     */
    void update(ACAQAlgorithmUI ui);

    /**
     * @return Create separator before adding install()
     */
    boolean withSeparator();
}
