/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 *
 */

package org.hkijena.jipipe.extensions.imagejdatatypes.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.RoiDrawer;
import org.hkijena.jipipe.extensions.settings.ImageViewerUISettings;

public class ImageViewerUIRoiDisplaySettings extends RoiDrawer {
    public static String ID = "image-viewer-ui-roi-display";

    public ImageViewerUIRoiDisplaySettings() {
    }

    public ImageViewerUIRoiDisplaySettings(RoiDrawer other) {
        super(other);
    }

    public static ImageViewerUIRoiDisplaySettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, ImageViewerUIRoiDisplaySettings.class);
    }
}
