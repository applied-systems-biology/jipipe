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
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.hkijena.jipipe.api.compat.ImageJDatatypeAdapter;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter between {@link ImagePlus} and {@link ImagePlusData}
 */
public class ImgPlusDataImageJAdapter implements ImageJDatatypeAdapter {

    private Class<? extends ImagePlusData> jipipeDataClass;

    /**
     * @param jipipeDataClass the JIPipe data class
     */
    public ImgPlusDataImageJAdapter(Class<? extends ImagePlusData> jipipeDataClass) {
        this.jipipeDataClass = jipipeDataClass;
    }

    @Override
    public boolean canConvertImageJToJIPipe(Object imageJData) {
        return imageJData instanceof ImagePlus;
    }

    @Override
    public boolean canConvertJIPipeToImageJ(JIPipeData jipipeData) {
        return jipipeData.getClass() == jipipeDataClass;
    }

    @Override
    public Class<?> getImageJDatatype() {
        return ImagePlus.class;
    }

    @Override
    public Class<? extends JIPipeData> getJIPipeDatatype() {
        return jipipeDataClass;
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
        try {
            ImagePlus img = ImageJUtils.duplicate((ImagePlus) imageJData);
            return (JIPipeData) ConstructorUtils.invokeConstructor(jipipeDataClass, img);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object convertJIPipeToImageJ(JIPipeData jipipeData, boolean activate, boolean noWindow, String windowName) {
        ImagePlus img = ((ImagePlusData) jipipeData).getDuplicateImage();
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
