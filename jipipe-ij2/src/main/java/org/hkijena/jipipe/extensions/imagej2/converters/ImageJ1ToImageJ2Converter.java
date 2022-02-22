package org.hkijena.jipipe.extensions.imagej2.converters;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataConverter;
import org.hkijena.jipipe.extensions.imagej2.datatypes.ImageJ2DatasetData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

public class ImageJ1ToImageJ2Converter implements JIPipeDataConverter {
    @Override
    public Class<? extends JIPipeData> getInputType() {
        return ImagePlusData.class;
    }

    @Override
    public Class<? extends JIPipeData> getOutputType() {
        return ImageJ2DatasetData.class;
    }

    @Override
    public JIPipeData convert(JIPipeData input) {
        return new ImageJ2DatasetData(((ImagePlusData)input).getImage());
    }
}
