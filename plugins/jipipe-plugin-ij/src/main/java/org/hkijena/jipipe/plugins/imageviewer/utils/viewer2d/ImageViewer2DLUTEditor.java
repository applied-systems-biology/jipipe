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

package org.hkijena.jipipe.plugins.imageviewer.utils.viewer2d;

import ij.ImagePlus;
import org.hkijena.jipipe.plugins.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.plugins.imageviewer.utils.ImageViewerLUTEditor;

public class ImageViewer2DLUTEditor extends ImageViewerLUTEditor {
    public ImageViewer2DLUTEditor(JIPipeImageViewer imageViewerPanel, int targetChannel) {
        super(imageViewerPanel, targetChannel);
    }

    @Override
    public void applyLUT() {
        ImagePlus image = getImageViewerPanel().getImagePlus();
        if (image != null && image.getType() != ImagePlus.COLOR_RGB) {
            if (getTargetChannel() < image.getNChannels()) {
//                if (image instanceof CompositeImage) {
//                    CompositeImage compositeImage = (CompositeImage) image;
//                    compositeImage.setChannelLut(getLUT(), targetChannel + 1);
//                }
//                int c = image.getC();
//                if (c == targetChannel + 1) {
//                    image.setLut(getLUT());
//                    imageViewerPanel.uploadSliceToCanvas();
//                }
                getImageViewerPanel().getViewerPanel2D().uploadSliceToCanvas();
            }
        }
    }
}
