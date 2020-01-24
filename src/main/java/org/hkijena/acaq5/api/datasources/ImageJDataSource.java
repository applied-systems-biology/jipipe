package org.hkijena.acaq5.api.datasources;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDataSource;
import org.hkijena.acaq5.api.ACAQOutputDataSlot;

/**
 * Data source that provides a predetermined {@link ij.ImagePlus} instance
 */
public class ImageJDataSource extends ACAQDataSource {

    private ImagePlus image;

    private ImageJDataSource(ImagePlus image, ACAQOutputDataSlot<?> outputDataSlot) {
        super(outputDataSlot);
        this.image = image;
    }

    @Override
    public void run() {
    }
}
