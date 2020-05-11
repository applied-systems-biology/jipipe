package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.fft;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;

/**
 * 2D image in frequency space
 */
@ACAQDocumentation(name = "2D FFT Image")
@ACAQOrganization(menuPath = "Images\nFFT")
public class ImagePlusFFT2DData extends ImagePlusData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 2;

    /**
     * Creates a new instance
     *
     * @param image wrapped image
     */
    public ImagePlusFFT2DData(ImagePlus image) {
        super(image);

        if (image.getNDimensions() > 2) {
            throw new IllegalArgumentException("Trying to fit higher-dimensional data into 2D data!");
        }
    }

}
