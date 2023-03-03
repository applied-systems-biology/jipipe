package org.hkijena.jipipe.extensions.imageviewer.plugins2d.maskdrawer;

import org.hkijena.jipipe.extensions.imageviewer.utils.ImageViewerPanelCanvas;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * The standard mouse selection.
 * Allows left-click canvas dragging
 */
public class MouseMaskDrawerTool extends MaskDrawerTool {
    public MouseMaskDrawerTool(MaskDrawerPlugin2D plugin) {
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
    public void onToolActivate(ImageViewerPanelCanvas canvas) {

    }

    @Override
    public void onToolDeactivate(ImageViewerPanelCanvas canvas) {
    }

    @Override
    public boolean toolAllowLeftMouseDrag() {
        return true;
    }

    @Override
    public boolean toolIsActive(ImageViewerPanelCanvas canvas) {
        return canvas.getTool() == null || canvas.getTool() instanceof MouseMaskDrawerTool;
    }

    @Override
    public boolean showGuides() {
        return false;
    }

    private void addAlgorithmButton(FormPanel formPanel, String name, String description, Icon icon, Runnable function) {
        JButton button = new JButton(name, icon);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setToolTipText(description);
        button.addActionListener(e -> function.run());
        formPanel.addToForm(button, new JLabel(), null);
    }

}
