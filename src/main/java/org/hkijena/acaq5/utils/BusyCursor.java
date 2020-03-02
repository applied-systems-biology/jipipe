/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Insitute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.utils;

import javax.swing.*;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Window;

public class BusyCursor implements AutoCloseable {

    private Window window;

    public BusyCursor(Component component) {
        this.window = SwingUtilities.getWindowAncestor(component);
        if (window != null) {
            window.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
    }

    @Override
    public void close() {
        if (window != null) {
            window.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }
}
