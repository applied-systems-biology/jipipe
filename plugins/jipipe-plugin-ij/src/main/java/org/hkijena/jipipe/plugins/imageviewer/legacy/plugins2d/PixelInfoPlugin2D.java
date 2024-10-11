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

package org.hkijena.jipipe.plugins.imageviewer.legacy.plugins2d;

import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.plugins.imageviewer.legacy.JIPipeDesktopLegacyImageViewer;
import org.hkijena.jipipe.plugins.imageviewer.utils.viewer2d.ImageViewerPanelCanvas2D;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.swing.*;
import java.awt.*;

public class PixelInfoPlugin2D extends GeneralImageViewerPanelPlugin2D implements ImageViewerPanelCanvas2D.PixelHoverEventListener {

    private final JLabel pixelInfoLabel = new JLabel(UIUtils.getIconFromResources("actions/tool-pointer.png"), JLabel.LEFT);

    public PixelInfoPlugin2D(JIPipeDesktopLegacyImageViewer viewerPanel) {
        super(viewerPanel);
        getViewerPanel2D().getCanvas().getPixelHoverEventEmitter().subscribe(this);
        updatePixelInfo(null);
    }

    @Override
    public void buildRibbon(JIPipeDesktopRibbon ribbon) {

    }

    @Override
    public void buildDock(JIPipeDesktopDockPanel dockPanel) {

    }

    @Override
    public void buildStatusBar(JPanel statusBar) {
        statusBar.add(pixelInfoLabel);
    }

    private void updatePixelInfo(Point coordinate) {
        if (getCurrentImagePlus() != null) {
            if (coordinate != null) {
                if (coordinate.x < 0 || coordinate.y < 0 || coordinate.x >= getCurrentImagePlus().getWidth() || coordinate.y >= getCurrentImagePlus().getHeight()) {
                    pixelInfoLabel.setText("x: " + coordinate.x + " y: " + coordinate.y + " (Outside image)");
                } else {
                    String value = "";
                    try {
                        ImageProcessor slice = getViewerPanel2D().getCurrentSlice();
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

    @Override
    public void onImageViewerCanvasPixelHover(ImageViewerPanelCanvas2D.PixelHoverEvent event) {
        updatePixelInfo(event.getPixelCoordinate());
    }
}
