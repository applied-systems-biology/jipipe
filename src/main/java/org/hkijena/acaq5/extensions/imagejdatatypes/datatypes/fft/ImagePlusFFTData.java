package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.fft;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;

/**
 * Image in frequency space
 */
@ACAQDocumentation(name = "FFT Image")
@ACAQOrganization(menuPath = "Images\nFFT")
public class ImagePlusFFTData extends ImagePlusData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = -1;

    /**
     * Creates a new instance
     *
     * @param image wrapped image
     */
    public ImagePlusFFTData(ImagePlus image) {
        super(image);
    }
}
