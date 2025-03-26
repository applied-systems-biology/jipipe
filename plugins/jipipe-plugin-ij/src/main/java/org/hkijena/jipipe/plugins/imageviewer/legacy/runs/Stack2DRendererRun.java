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

package org.hkijena.jipipe.plugins.imageviewer.legacy.runs;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.imageviewer.legacy.JIPipeDesktopLegacyImageViewer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Stack2DRendererRun extends AbstractJIPipeRunnable {
    private final String name;
    private final JIPipeDesktopLegacyImageViewer viewerPanel;
    private final double magnification;
    private final Consumer<ImagePlus> nextAction;

    public Stack2DRendererRun(JIPipeDesktopLegacyImageViewer viewerPanel, String name, Consumer<ImagePlus> nextAction) {
        this.viewerPanel = viewerPanel;
        this.name = name;
        this.nextAction = nextAction;
        this.magnification = viewerPanel.getViewerPanel2D().getExportedMagnification();
    }

    public Stack2DRendererRun(JIPipeDesktopLegacyImageViewer viewerPanel, String name, double magnification, Consumer<ImagePlus> nextAction) {
        this.viewerPanel = viewerPanel;
        this.name = name;
        this.nextAction = nextAction;
        this.magnification = magnification;
    }

    @Override
    public String getTaskLabel() {
        return name;
    }

    @Override
    public void run() {
        JIPipeProgressInfo progressInfo = getProgressInfo();
        ImagePlus image = viewerPanel.getImagePlus();
        progressInfo.setMaxProgress(image.getStackSize());
        Map<ImageSliceIndex, ImageProcessor> generatedSlices = new HashMap<>();
        for (int c = 0; c < image.getNChannels(); c++) {
            for (int t = 0; t < image.getNFrames(); t++) {
                for (int z = 0; z < image.getNSlices(); z++) {
                    if (progressInfo.isCancelled())
                        return;
                    ImageSliceIndex sliceIndex = new ImageSliceIndex(c, z, t);
                    progressInfo.log("Rendering " + sliceIndex);
                    progressInfo.incrementProgress();
                    ImageProcessor slice = viewerPanel.getViewerPanel2D().generateSlice(c, z, t, magnification, true);
                    generatedSlices.put(sliceIndex, slice);
                }
            }
        }

        ImagePlus outputImage = ImageJUtils.mergeMappedSlices(generatedSlices);
        outputImage.copyScale(image);
        outputImage.setTitle(image.getTitle() + " (Rendered)");

        progressInfo.log("Rendering finished. Continuing.");
        nextAction.accept(outputImage);
    }

    public JIPipeDesktopLegacyImageViewer getViewerPanel() {
        return viewerPanel;
    }
}
