package org.hkijena.jipipe.extensions.imp.datatypes;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataConverter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

public class ImpImageToImageJImageDataTypeConverter implements JIPipeDataConverter {
    @Override
    public Class<? extends JIPipeData> getInputType() {
        return ImpImageData.class;
    }

    @Override
    public Class<? extends JIPipeData> getOutputType() {
        return ImagePlusData.class;
    }

    @Override
    public JIPipeData convert(JIPipeData input, JIPipeProgressInfo progressInfo) {
        return new ImagePlusData(new ImagePlus("Image", ((ImpImageData)input).getImageWithoutAlpha()));
    }
}
