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

package org.hkijena.jipipe.plugins.imageviewer;

import ij.ImagePlus;
import ij3d.Content;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.imageviewer.utils.viewer3d.universe.CustomImage3DUniverse;

import java.util.List;

public abstract class JIPipeLegacyImageViewerPlugin3D extends JIPipeImageViewerPlugin {

    public JIPipeLegacyImageViewerPlugin3D(JIPipeLegacyImageViewer viewerPanel) {
        super(viewerPanel);
    }

    public LegacyImageViewerPanel3D getViewerPanel3D() {
        return getViewerPanel().getImageViewerPanel3D();
    }

    public CustomImage3DUniverse getUniverse() {
        return getViewerPanel3D().getUniverse();
    }

    public ImagePlus preprocess(ImagePlus imagePlus, JIPipeProgressInfo progressInfo) {
        return imagePlus;
    }

    public void onImageContentReady(List<Content> content) {

    }

    public void onViewerUniverseReady() {

    }

    public void onViewerActivated() {

    }
}
