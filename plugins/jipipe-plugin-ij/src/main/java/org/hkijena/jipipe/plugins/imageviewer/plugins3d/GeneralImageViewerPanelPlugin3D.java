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

package org.hkijena.jipipe.plugins.imageviewer.plugins3d;

import org.hkijena.jipipe.plugins.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.plugins.imageviewer.JIPipeImageViewerPlugin3D;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public abstract class GeneralImageViewerPanelPlugin3D extends JIPipeImageViewerPlugin3D {
    public GeneralImageViewerPanelPlugin3D(JIPipeImageViewer viewerPanel) {
        super(viewerPanel);
    }

    @Override
    public String getCategory() {
        return "General";
    }

    @Override
    public Icon getCategoryIcon() {
        return UIUtils.getIconFromResources("actions/configure.png");
    }
}
