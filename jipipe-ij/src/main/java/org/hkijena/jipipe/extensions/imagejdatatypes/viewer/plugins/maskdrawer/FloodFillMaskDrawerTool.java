package org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.maskdrawer;

import com.google.common.eventbus.Subscribe;
import ij.process.FloodFiller;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.utils.BusyCursor;
import org.hkijena.jipipe.utils.MouseClickedEvent;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * The standard mouse selection.
 * Allows left-click canvas dragging
 */
public class FloodFillMaskDrawerTool extends MaskDrawerTool {
    public FloodFillMaskDrawerTool(MaskDrawerPlugin plugin) {
        super(plugin,
                "Flood fill",
                "Fills the selected area with the selected color",
                UIUtils.getIconFromResources("actions/color-fill.png"));
        getViewerPanel().getCanvas().getEventBus().register(this);
    }

    @Override
    public void activate() {
        getViewerPanel().getCanvas().setStandardCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        getViewerPanel().getCanvas().setDragWithLeftMouse(false);
    }

    @Override
    public void deactivate() {

    }

    @Subscribe
    public void onMouseClick(MouseClickedEvent event) {
        if (!isActive())
            return;
        if (SwingUtilities.isLeftMouseButton(event)) {
            Point point = getViewerPanel().getCanvas().getMouseModelPixelCoordinate(true);
            if (point == null) {
                return;
            }

            ImageProcessor processor = getMaskDrawerPlugin().getCurrentMaskSlice();
            if (getMaskDrawerPlugin().getCurrentColor() == MaskDrawerPlugin.MaskColor.Foreground) {
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
        }
    }
}
