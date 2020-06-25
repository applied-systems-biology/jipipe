/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

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
