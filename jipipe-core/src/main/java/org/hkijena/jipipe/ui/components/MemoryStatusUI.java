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

package org.hkijena.jipipe.ui.components;

import javax.swing.*;

/**
 * Shows the current memory consumption
 */
public class MemoryStatusUI extends JProgressBar {

    private static final long MEGABYTES = 1024 * 1024;
    private Timer timer;

    /**
     * Creates a new instance
     */
    public MemoryStatusUI() {
        initialize();
    }

    private void initialize() {
        setStringPainted(true);
        setString("- / -");
        timer = new Timer(1000, e -> {
            setMaximum((int) (Runtime.getRuntime().maxMemory() / MEGABYTES));
            setValue((int) (Runtime.getRuntime().totalMemory() / MEGABYTES));
            setString((Runtime.getRuntime().totalMemory() / MEGABYTES) + "MB / " + (Runtime.getRuntime().maxMemory() / MEGABYTES) + "MB");
        });
        timer.setRepeats(true);
        timer.start();
    }
}
