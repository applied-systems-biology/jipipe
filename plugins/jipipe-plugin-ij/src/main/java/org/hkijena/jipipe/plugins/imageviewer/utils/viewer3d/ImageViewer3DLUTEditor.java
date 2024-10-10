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

package org.hkijena.jipipe.plugins.imageviewer.utils.viewer3d;

import ij.ImagePlus;
import org.hkijena.jipipe.plugins.imageviewer.legacy.JIPipeDesktopLegacyImageViewer;
import org.hkijena.jipipe.plugins.imageviewer.utils.ImageViewerLUTEditor;

public class ImageViewer3DLUTEditor extends ImageViewerLUTEditor {
    public ImageViewer3DLUTEditor(JIPipeDesktopLegacyImageViewer imageViewerPanel, int targetChannel) {
        super(imageViewerPanel, targetChannel);
    }

    @Override
    public void applyLUT() {
        ImagePlus image = getImageViewerPanel().getImagePlus();
        if (image != null && image.getType() != ImagePlus.COLOR_RGB) {
            if (getTargetChannel() < image.getNChannels()) {
                getImageViewerPanel().getImageViewerPanel3D().scheduleUpdateLutAndCalibration();
            }
        }
    }
}
