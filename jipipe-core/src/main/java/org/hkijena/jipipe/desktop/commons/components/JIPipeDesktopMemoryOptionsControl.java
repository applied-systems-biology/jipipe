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

import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

/**
 * A tool that automatically runs 'update cache' when any parameter or graph property is changed
 */
public class JIPipeDesktopMemoryOptionsControl extends JIPipeDesktopProjectWorkbenchPanel {

    /**
     * @param workbenchUI The workbench UI
     */
    public JIPipeDesktopMemoryOptionsControl(JIPipeDesktopProjectWorkbench workbenchUI) {
        super(workbenchUI);
    }

    public JButton createOptionsButton() {
        JButton button = new JButton("Memory", UIUtils.getIconFromResources("devices/media-memory.png"));
        JPopupMenu menu = new JPopupMenu();
        UIUtils.addReloadablePopupMenuToButton(button, menu, () -> {
            menu.removeAll();
            JMenuItem gcItem = new JMenuItem("Clean memory", UIUtils.getIconFromResources("actions/clear-brush.png"));
            gcItem.setToolTipText("Runs the garbage collector (GC) that attempts to clean unused memory. Please note that this will shortly freeze the application.");
            gcItem.addActionListener(e -> {
                System.gc();
                getDesktopWorkbench().sendStatusBarText("Unused memory was freed");
            });
            menu.add(gcItem);
        });
        return button;
    }
}
