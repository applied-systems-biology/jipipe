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

package org.hkijena.jipipe.extensions.imageviewer.plugins2d;

import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewerPlugin2D;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public abstract class GeneralImageViewerPanelPlugin2D extends JIPipeImageViewerPlugin2D {
    public GeneralImageViewerPanelPlugin2D(JIPipeImageViewer viewerPanel) {
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
