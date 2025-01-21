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

package org.hkijena.jipipe.plugins.imagejdatatypes.display;

import ij.ImagePlus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.desktop.api.data.JIPipeDesktopDataDisplayOperation;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class OpenInImageJDataDisplayOperation implements JIPipeDesktopDataDisplayOperation {
    @Override
    public void display(JIPipeData data, String displayName, JIPipeDesktopWorkbench desktopWorkbench, JIPipeDataSource source) {
        ImagePlus image;
        if (data instanceof ImagePlusData) {
            image = ((ImagePlusData) data).getDuplicateImage();
        } else if (data instanceof OMEImageData) {
            image = ((OMEImageData) data).getDuplicateImage();
        } else if (JIPipe.getDataTypes().isConvertible(data.getClass(), ImagePlusData.class)) {
            image = JIPipe.getDataTypes().convert(data, ImagePlusData.class, JIPipeProgressInfo.SILENT).getDuplicateImage();
        } else {
            throw new UnsupportedOperationException();
        }
        image.setTitle(displayName);
        image.show();
    }

    @Override
    public String getId() {
        return "jipipe:open-image-in-imagej";
    }

    @Override
    public String getName() {
        return "Open in ImageJ";
    }

    @Override
    public String getDescription() {
        return "Opens the image in ImageJ";
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("apps/imagej.png");
    }
}
