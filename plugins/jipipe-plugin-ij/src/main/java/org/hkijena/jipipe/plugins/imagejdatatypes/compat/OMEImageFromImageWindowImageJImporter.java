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

package org.hkijena.jipipe.plugins.imagejdatatypes.compat;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.compat.ImageJDataImporter;
import org.hkijena.jipipe.api.compat.ImageJImportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.List;

@SetJIPipeDocumentation(name = "Import OME Image from ImageJ window", description = "Imports an image window into JIPipe")
public class OMEImageFromImageWindowImageJImporter implements ImageJDataImporter {

    @Override
    public JIPipeDataTable importData(List<Object> objects, ImageJImportParameters parameters, JIPipeProgressInfo progressInfo) {
        ImagePlus imagePlus;
        if (StringUtils.isNullOrEmpty(parameters.getName())) {
            imagePlus = IJ.getImage(); // The active image
        } else {
            imagePlus = WindowManager.getImage(parameters.getName());
        }
        if (parameters.isDuplicate()) {
            String title = imagePlus.getTitle();
            imagePlus = imagePlus.duplicate();
            imagePlus.setTitle(title);
        }
        JIPipeDataTable result = new JIPipeDataTable(OMEImageData.class);
        ROIListData rois = new ROIListData();
        for (Roi roi : RoiManager.getRoiManager().getRoisAsArray()) {
            if (roi.getImage() == imagePlus) {
                if (parameters.isDuplicate())
                    rois.add((Roi) roi.clone());
                else
                    rois.add(roi);
            }
        }
        OMEImageData omeImageData = new OMEImageData(imagePlus, rois, null);
        result.addData(omeImageData, new JIPipeProgressInfo());
        return result;
    }

    @Override
    public Class<? extends JIPipeData> getImportedJIPipeDataType() {
        return OMEImageData.class;
    }

    @Override
    public Class<?> getImportedImageJDataType() {
        return ImagePlus.class;
    }
}
