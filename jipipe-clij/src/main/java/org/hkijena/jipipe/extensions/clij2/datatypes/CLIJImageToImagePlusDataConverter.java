package org.hkijena.jipipe.extensions.clij2.datatypes;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataConverter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

/**
 * Converter from {@link CLIJImageData} to {@link ImagePlusData}
 */
public class CLIJImageToImagePlusDataConverter implements JIPipeDataConverter {
    @Override
    public Class<? extends JIPipeData> getInputType() {
        return CLIJImageData.class;
    }

    @Override
    public Class<? extends JIPipeData> getOutputType() {
        return ImagePlusData.class;
    }

    @Override
    public JIPipeData convert(JIPipeData input) {
        return ((CLIJImageData)input).pull();
    }
}
