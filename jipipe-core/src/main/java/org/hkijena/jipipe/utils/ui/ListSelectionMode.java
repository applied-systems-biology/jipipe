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

package org.hkijena.jipipe.utils.ui;

import javax.swing.*;

/**
 * Enum wrapper for {@link javax.swing.ListSelectionModel} modes
 */
public enum ListSelectionMode {
    Single(ListSelectionModel.SINGLE_SELECTION),
    SingleInterval(ListSelectionModel.SINGLE_INTERVAL_SELECTION),
    MultipleInterval(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    private final int nativeValue;

    ListSelectionMode(int nativeValue) {

        this.nativeValue = nativeValue;
    }

    public int getNativeValue() {
        return nativeValue;
    }
}
