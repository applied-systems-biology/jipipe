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

package org.hkijena.jipipe.desktop.app.customizer;

import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopThemeEditor extends JFrame {

    private final JIPipeDesktopWorkbench workbench;

    public JIPipeDesktopThemeEditor(JIPipeDesktopWorkbench workbench) {
        this.workbench = workbench;
        initialize();
    }

    private void initialize() {
        setTitle("JIPipe - Theme editor");
        setIconImage(UIUtils.getJIPipeIcon128());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout(8,8));
        getContentPane().setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());

        // Final preparation
        pack();
        setSize(1024,768);
        setLocationRelativeTo(workbench.getWindow());
    }
}
