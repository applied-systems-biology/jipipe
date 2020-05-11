package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.fft;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * 5D image in frequency space
 */
@ACAQDocumentation(name = "5D FFT Image")
@ACAQOrganization(menuPath = "Images\nFFT")
public class ImagePlusFFT5DData extends ImagePlusFFT2DData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 5;

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

}
