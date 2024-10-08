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

package org.hkijena.jipipe.desktop.commons.components;

import javax.swing.*;

/**
 * Shows the current memory consumption
 */
public class JIPipeDesktopMemoryStatusUI extends JProgressBar {

    private static final long MEGABYTES = 1024 * 1024;

    /**
     * Creates a new instance
     */
    public JIPipeDesktopMemoryStatusUI() {
        initialize();
    }

    private void initialize() {
        setStringPainted(true);
        setString("- / -");
        Timer timer = new Timer(1000, e -> {
            long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / MEGABYTES;
            setMaximum((int) (Runtime.getRuntime().maxMemory() / MEGABYTES));
            setValue((int) usedMemory);
            setString(usedMemory + "MB / " + (Runtime.getRuntime().maxMemory() / MEGABYTES) + "MB");
        });
        timer.setRepeats(true);
        timer.start();
    }
}
