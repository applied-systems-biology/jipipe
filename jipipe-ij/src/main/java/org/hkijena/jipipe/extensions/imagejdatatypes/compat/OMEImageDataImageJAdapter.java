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

package org.hkijena.jipipe.extensions.imagejdatatypes.compat;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter between {@link ImagePlus} and {@link org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData}
 */
public class OMEImageDataImageJAdapter implements ImageJDatatypeAdapter {

    public OMEImageDataImageJAdapter() {
    }

    @Override
    public boolean canConvertImageJToJIPipe(Object imageJData) {
        return imageJData instanceof ImagePlus;
    }

    @Override
    public boolean canConvertJIPipeToImageJ(JIPipeData jipipeData) {
        return jipipeData instanceof OMEImageData;
    }

    @Override
    public Class<?> getImageJDatatype() {
        return ImagePlus.class;
    }

    @Override
    public Class<? extends JIPipeData> getJIPipeDatatype() {
        return OMEImageData.class;
    }

    /**
     * Converts {@link ImagePlus} to an {@link ImagePlusData} instance.
     * If the imageJData is a {@link String}, the image is taken from the window with this name
     * If the imageJData is null, it is taken from the current WindowManager image
     *
     * @param imageJData the ImageJ data
     * @return the converted data
     */
    @Override
    public JIPipeData convertImageJToJIPipe(Object imageJData) {
        // If we provide a window name, convert it to ImagePlus first
        if (imageJData instanceof String) {
            imageJData = WindowManager.getImage((String) imageJData);
        }
        if (imageJData == null) {
            imageJData = IJ.getImage();
        }
        ImagePlus img = ((ImagePlus) imageJData).duplicate();
        ROIListData rois = new ROIListData();
        if (img.getRoi() != null) {
            rois.add(img.getRoi());
        }
        return new OMEImageData(img, rois, null);
    }

    @Override
    public Object convertJIPipeToImageJ(JIPipeData jipipeData, boolean activate, boolean noWindow, String windowName) {
        ImagePlus img = ((OMEImageData) jipipeData).getDuplicateImage();
        if (activate) {
            if (!noWindow) {
                img.show();
                if (!StringUtils.isNullOrEmpty(windowName)) {
                    img.setTitle(windowName);
                }
            }
            WindowManager.setTempCurrentImage(img);
        }
        return img;
    }

    @Override
    public List<Object> convertMultipleJIPipeToImageJ(List<JIPipeData> jipipeData, boolean activate, boolean noWindow, String windowName) {
        List<Object> result = new ArrayList<>();
        for (JIPipeData data : jipipeData) {
            result.add(convertJIPipeToImageJ(data, activate, noWindow, windowName));
        }
        return result;
    }

    @Override
    public JIPipeData importDataImageJ(String parameters) {
        if (StringUtils.isNullOrEmpty(parameters))
            return convertImageJToJIPipe(IJ.getImage());
        ImagePlus image = WindowManager.getImage(parameters);
        return convertImageJToJIPipe(image);
    }
}
