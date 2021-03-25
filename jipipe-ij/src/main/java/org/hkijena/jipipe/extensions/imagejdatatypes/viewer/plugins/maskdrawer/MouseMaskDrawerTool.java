package org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.maskdrawer;

import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * The standard mouse selection.
 * Allows left-click canvas dragging
 */
public class MouseMaskDrawerTool extends MaskDrawerTool {
    public MouseMaskDrawerTool(MaskDrawerPlugin plugin) {
        super(plugin, "Panning", "Allows to drag the canvas with the left mouse", UIUtils.getIconFromResources("actions/hand.png"));
    }

    @Override
    public void activate() {
        getViewerPanel().getCanvas().setStandardCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        getViewerPanel().getCanvas().setDragWithLeftMouse(true);
    }

    @Override
    public void deactivate() {

    }
}
