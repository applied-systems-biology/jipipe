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

package org.hkijena.jipipe.desktop.commons.components.ai.monitor;

import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;

import java.awt.*;

/**
 * Abstract base class for pages within the {@link JIPipeDesktopAIMonitorWindow}.
 * Follows the same page-based design pattern as the cache monitor.
 */
public abstract class JIPipeDesktopAIMonitorPage extends JIPipeDesktopWorkbenchPanel {
    private final JIPipeDesktopAIMonitorWindow window;

    public JIPipeDesktopAIMonitorPage(JIPipeDesktopAIMonitorWindow window) {
        super(window.getDesktopWorkbench());
        this.window = window;
        setLayout(new BorderLayout(8, 8));
    }

    public JIPipeDesktopAIMonitorWindow getWindow() {
        return window;
    }

    /**
     * Refreshes the page content.
     */
    public abstract void refresh();
}
