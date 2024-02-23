package org.hkijena.jipipe.extensions.imp.datatypes;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataConverter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

public class ImageJImageToImpImageDataTypeConverter implements JIPipeDataConverter {
    @Override
    public Class<? extends JIPipeData> getInputType() {
        return ImagePlusData.class;
    }

    @Override
    public Class<? extends JIPipeData> getOutputType() {
        return ImpImageData.class;
    }

    @Override
    public JIPipeData convert(JIPipeData input, JIPipeProgressInfo progressInfo) {
        ImagePlus img = ((ImagePlusData)input).getImage();
        return new ImpImageData(ImageJUtils.renderToRGBWithLUTIfNeeded(img, progressInfo).getBufferedImage());
    }
}
