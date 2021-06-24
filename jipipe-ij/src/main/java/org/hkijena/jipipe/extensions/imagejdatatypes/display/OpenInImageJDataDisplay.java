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
 */

package org.hkijena.jipipe.extensions.imagejdatatypes.display;

import ij.ImagePlus;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataDisplayOperation;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class OpenInImageJDataDisplay implements JIPipeDataDisplayOperation {
    @Override
    public void display(JIPipeData data, String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        ImagePlus image;
        if (data instanceof ImagePlusData) {
            image = ((ImagePlusData) data).getDuplicateImage();
        } else if (data instanceof OMEImageData) {
            image = ((OMEImageData) data).getDuplicateImage();
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
