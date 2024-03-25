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

package org.hkijena.jipipe.plugins.imageviewer.plugins2d.maskdrawer;

import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.plugins.imageviewer.utils.viewer2d.ImageViewerPanelCanvas2D;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * The standard mouse selection.
 * Allows left-click canvas dragging
 */
public class MouseMaskDrawer2DTool extends MaskDrawer2DTool {
    public MouseMaskDrawer2DTool(MaskDrawerPlugin2D plugin) {
        super(plugin,
                "No tool",
                "Allows to drag the canvas with the left mouse",
                UIUtils.getIcon32FromResources("actions/hand.png"));
    }

    @Override
    public Cursor getToolCursor() {
        return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    }

    @Override
    public void onToolActivate(ImageViewerPanelCanvas2D canvas) {

    }

    @Override
    public void onToolDeactivate(ImageViewerPanelCanvas2D canvas) {
    }

    @Override
    public boolean toolAllowLeftMouseDrag() {
        return true;
    }

    @Override
    public boolean toolIsActive(ImageViewerPanelCanvas2D canvas) {
        return canvas.getTool() == null || canvas.getTool() instanceof MouseMaskDrawer2DTool;
    }

    @Override
    public boolean showGuides() {
        return false;
    }

    private void addAlgorithmButton(JIPipeDesktopFormPanel formPanel, String name, String description, Icon icon, Runnable function) {
        JButton button = new JButton(name, icon);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setToolTipText(description);
        button.addActionListener(e -> function.run());
        formPanel.addToForm(button, new JLabel(), null);
    }

}
