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

package org.hkijena.jipipe.extensions.imagejdatatypes.algorithms;

import ij.ImagePlus;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataConverter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

public class ImagePlusToOMEImageTypeConverter implements JIPipeDataConverter {
    @Override
    public Class<? extends JIPipeData> getInputType() {
        return ImagePlusData.class;
    }

    @Override
    public Class<? extends JIPipeData> getOutputType() {
        return OMEImageData.class;
    }

    @Override
    public JIPipeData convert(JIPipeData input) {
        ImagePlus img = ((ImagePlusData) input).getImage();
        ROIListData rois = new ROIListData();
        if (img.getRoi() != null) {
            rois.add(img.getRoi());
        }
        return new OMEImageData(img, rois, null);
    }
}
