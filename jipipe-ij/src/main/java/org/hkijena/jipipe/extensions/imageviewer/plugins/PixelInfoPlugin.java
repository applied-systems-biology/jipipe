package org.hkijena.jipipe.extensions.imageviewer.plugins;

import com.google.common.eventbus.Subscribe;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanel;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanelCanvas;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class PixelInfoPlugin extends GeneralImageViewerPanelPlugin {

    private final JLabel pixelInfoLabel = new JLabel(UIUtils.getIconFromResources("actions/tool-pointer.png"), JLabel.LEFT);

    public PixelInfoPlugin(ImageViewerPanel viewerPanel) {
        super(viewerPanel);
        getViewerPanel().getCanvas().getEventBus().register(this);
        updatePixelInfo(null);
    }

    @Override
    public ImageProcessor draw(int c, int z, int t, ImageProcessor processor) {
        return processor;
    }

    @Override
    public void createPalettePanel(FormPanel formPanel) {
        formPanel.addWideToForm(pixelInfoLabel, null);
    }

    @Subscribe
    public void onPixelHover(ImageViewerPanelCanvas.PixelHoverEvent event) {
        updatePixelInfo(event.getPixelCoordinate());
    }

    private void updatePixelInfo(Point coordinate) {
        if (getCurrentImage() != null) {
            if (coordinate != null) {
                if (coordinate.x < 0 || coordinate.y < 0 || coordinate.x >= getCurrentImage().getWidth() || coordinate.y >= getCurrentImage().getHeight()) {
                    pixelInfoLabel.setText("x: " + coordinate.x + " y: " + coordinate.y + " (Outside image)");
                } else {
                    String value = "";
                    try {
                        ImageProcessor slice = getViewerPanel().getCurrentSlice();
                        if (slice != null) {
                            if (slice instanceof ColorProcessor) {
                                Color color = ((ColorProcessor) slice).getColor(coordinate.x, coordinate.y);
                                value = String.format("RGB(%d, %d, %d)", color.getRed(), color.getGreen(), color.getBlue());
                            } else {
                                value = "Intensity: " + slice.getf(coordinate.x, coordinate.y);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    pixelInfoLabel.setText("x: " + coordinate.x + " y: " + coordinate.y + " " + value);
                }

            } else {
                pixelInfoLabel.setText("No info available");
            }
        } else {
            pixelInfoLabel.setText("No info available");
        }
    }
}
