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

package org.hkijena.jipipe.desktop.commons.components.tools.memorymonitor;

import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;

import java.awt.*;

public abstract class JIPipeDesktopMemoryMonitorPage extends JIPipeDesktopWorkbenchPanel {
    private final JIPipeDesktopMemoryMonitorWindow window;

    public JIPipeDesktopMemoryMonitorPage(JIPipeDesktopMemoryMonitorWindow window) {
        super(window.getDesktopWorkbench());
        this.window = window;
        setLayout(new BorderLayout(8, 8));
    }

    public JIPipeDesktopMemoryMonitorWindow getWindow() {
        return window;
    }

    public abstract void refresh();

    /**
     * Adds this page's content to a report.
     *
     * @param reportBuilder the StringBuilder to append the report content to
     */
    public abstract void addToReport(StringBuilder reportBuilder);
}
