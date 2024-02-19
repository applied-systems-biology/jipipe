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

package org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.fft;

import ij.ImagePlus;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.LabelAsJIPipeHeavyData;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImageTypeInfo;

/**
 * 5D image in frequency space
 */
@SetJIPipeDocumentation(name = "5D FFT Image")
@DefineJIPipeNode(menuPath = "Images\nFFT")
@LabelAsJIPipeHeavyData
@ImageTypeInfo(numDimensions = 5)
public class ImagePlusFFT5DData extends ImagePlusFFT2DData {

    /**
     * Creates a new instance
     *
     * @param image wrapped image
     */
    public ImagePlusFFT5DData(ImagePlus image) {
        super(image);

        if (image.getNDimensions() > 5) {
            throw new IllegalArgumentException("Trying to fit higher-dimensional data into 5D data!");
        }
    }

    public static ImagePlusFFT5DData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new ImagePlusFFT5DData(ImagePlusFFTData.importData(storage, progressInfo).getImage());
    }

    /**
     * Converts the incoming image data into the current format.
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        return new ImagePlusFFT5DData(data.getImage());
    }
}
