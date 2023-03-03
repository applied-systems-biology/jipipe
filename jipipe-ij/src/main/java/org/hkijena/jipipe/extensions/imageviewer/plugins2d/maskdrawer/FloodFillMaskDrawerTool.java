package org.hkijena.jipipe.extensions.imageviewer.plugins2d.maskdrawer;

import com.google.common.eventbus.Subscribe;
import ij.process.FloodFiller;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imageviewer.utils.ImageViewerPanelCanvas;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.BusyCursor;
import org.hkijena.jipipe.utils.ui.MouseClickedEvent;

import javax.swing.*;
import java.awt.*;

/**
 * The standard mouse selection.
 * Allows left-click canvas dragging
 */
public class FloodFillMaskDrawerTool extends MaskDrawerTool {
    public FloodFillMaskDrawerTool(MaskDrawerPlugin2D plugin) {
        super(plugin,
                "Flood fill",
                "Fills the selected area with the selected color",
                UIUtils.getIconFromResources("actions/color-fill.png"));
        getViewerPanel2D().getCanvas().getEventBus().register(this);
    }

    @Override
    public Cursor getToolCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    @Override
    public void onToolActivate(ImageViewerPanelCanvas canvas) {

    }

    @Override
    public void onToolDeactivate(ImageViewerPanelCanvas canvas) {

    }

    @Override
    public boolean showGuides() {
        return false;
    }

    @Subscribe
    public void onMouseClick(MouseClickedEvent event) {
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
