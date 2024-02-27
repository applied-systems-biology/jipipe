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

package org.hkijena.jipipe.extensions.imagej2.io.data.output;

import ij.ImagePlus;
import org.hkijena.jipipe.extensions.imagej2.datatypes.ImageJ2DatasetData;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.scijava.plugin.Plugin;

/**
 * Handling of {@link ImagePlus}
 * It is assumed that these refer to images (conversion to {@link ImageJ2DatasetData})
 */
@Plugin(type = ImageJ2ModuleIO.class)
public class ImagePlusImageJ2ModuleOutput extends DataSlotModuleOutput<ImagePlus, ImagePlusData> {
    @Override
    public ImagePlus convertJIPipeToModuleData(ImagePlusData obj) {
        return obj.getImage();
    }

    @Override
    public ImagePlusData convertModuleToJIPipeData(ImagePlus obj) {
        return new ImagePlusData(obj);
    }

    @Override
    public Class<ImagePlus> getModuleDataType() {
        return ImagePlus.class;
    }

    @Override
    public Class<ImagePlusData> getJIPipeDataType() {
        return ImagePlusData.class;
    }
}
