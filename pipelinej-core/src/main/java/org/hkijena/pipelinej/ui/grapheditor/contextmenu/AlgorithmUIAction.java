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

package org.hkijena.pipelinej.ui.grapheditor.contextmenu;

import org.hkijena.pipelinej.ui.grapheditor.ACAQGraphCanvasUI;
import org.hkijena.pipelinej.ui.grapheditor.ACAQNodeUI;

import javax.swing.*;
import java.util.Set;

/**
 * An action that is applied one or multiple algorithms
 */
public interface AlgorithmUIAction {

    /**
     * Indicates that a separator is created
     */
    AlgorithmUIAction SEPARATOR = null;

    /**
     * Returns if the action shows up
     *
     * @param selection the list of algorithm UIs
     * @return if the action shows up
     */
    boolean matches(Set<ACAQNodeUI> selection);

    /**
     * Runs the workload
     *
     * @param canvasUI  the canvas that contains all algorithm UIs
     * @param selection the current selection of algorithms
     */
    void run(ACAQGraphCanvasUI canvasUI, Set<ACAQNodeUI> selection);

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
     * If true, this item is shown in the toolbar overhang menu instead of the toolbar itself.
     *
     * @return show in toolbar overhang menu
     */
    boolean isShowingInOverhang();

    /**
     * Determines if an item should be disabled or removed if it does not match
     *
     * @return if an item should be disabled or removed if it does not match
     */
    default boolean disableOnNonMatch() {
        return true;
    }
}
