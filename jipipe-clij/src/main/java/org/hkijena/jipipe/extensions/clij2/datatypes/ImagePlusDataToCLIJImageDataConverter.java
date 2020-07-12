package org.hkijena.jipipe.extensions.clij2.datatypes;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataConverter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

/**
 * Converter from {@link org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData} to {@link CLIJImageData}
 */
public class ImagePlusDataToCLIJImageDataConverter implements JIPipeDataConverter {
    @Override
    public Class<? extends JIPipeData> getInputType() {
        return ImagePlusData.class;
    }

    @Override
    public Class<? extends JIPipeData> getOutputType() {
        return CLIJImageData.class;
    }

    @Override
    public JIPipeData convert(JIPipeData input) {
        return new CLIJImageData((ImagePlusData)input);
    }
}
