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

package org.hkijena.jipipe.plugins.imageviewer.legacy.plugins2d.maskdrawer;

import ij.process.FloodFiller;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.plugins.imageviewer.utils.viewer2d.ImageViewerPanelCanvas2D;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.BusyCursor;
import org.hkijena.jipipe.utils.ui.events.MouseClickedEvent;
import org.hkijena.jipipe.utils.ui.events.MouseClickedEventListener;

import javax.swing.*;
import java.awt.*;

/**
 * The standard mouse selection.
 * Allows left-click canvas dragging
 */
public class FloodFillMaskDrawer2DTool extends MaskDrawer2DTool implements MouseClickedEventListener {
    public FloodFillMaskDrawer2DTool(MaskDrawerPlugin2D plugin) {
        super(plugin,
                "Flood fill",
                "Fills the selected area with the selected color",
                UIUtils.getIconFromResources("actions/color-fill.png"));
        ImageViewerPanelCanvas2D canvas = getViewerPanel2D().getCanvas();
        canvas.getMouseClickedEventEmitter().subscribe(this);
    }

    @Override
    public Cursor getToolCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    @Override
    public void onToolActivate(ImageViewerPanelCanvas2D canvas) {

    }

    @Override
    public void onToolDeactivate(ImageViewerPanelCanvas2D canvas) {

    }

    @Override
    public boolean showGuides() {
        return false;
    }

    @Override
    public void onComponentMouseClicked(MouseClickedEvent event) {
        if (!toolIsActive())
            return;
        if (SwingUtilities.isLeftMouseButton(event)) {
            Point point = getViewerPanel2D().getCanvas().getMouseModelPixelCoordinate(null, true);
            if (point == null) {
                return;
            }

            ImageProcessor processor = getMaskDrawerPlugin().getCurrentMaskSlice();
            if (getMaskDrawerPlugin().getCurrentColor() == MaskDrawerPlugin2D.MaskColor.Foreground) {
                if (processor.get(point.x, point.y) > 0)
                    return;
                FloodFiller filler = new FloodFiller(processor);
                try (BusyCursor cursor = new BusyCursor(getViewerPanel())) {
                    processor.setValue(255);
                    filler.fill(point.x, point.y);
                }
            } else {
                if (processor.get(point.x, point.y) == 0)
                    return;
                processor.invert();
                FloodFiller filler = new FloodFiller(processor);
                try (BusyCursor cursor = new BusyCursor(getViewerPanel())) {
                    processor.setValue(255);
                    filler.fill(point.x, point.y);
                }
                processor.invert();
            }
            getMaskDrawerPlugin().recalculateMaskPreview();
            postMaskChangedEvent();
        }
    }
}
