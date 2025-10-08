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

package org.hkijena.jipipe.desktop.commons.components.support;

import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopSupportAssistantWindow extends JFrame {
    public JIPipeDesktopSupportAssistantWindow() {
        initialize();
    }

    private void initialize() {
        setTitle("JIPipe - Support");
        setIconImage(UIUtils.getJIPipeIcon128());
    }

    public static void show(Component parent) {
        JIPipeDesktopSupportAssistantWindow window = new JIPipeDesktopSupportAssistantWindow();
        window.pack();
        window.setSize(1027,768);
        window.setLocationRelativeTo(parent);
        window.setVisible(true);
    }
}
