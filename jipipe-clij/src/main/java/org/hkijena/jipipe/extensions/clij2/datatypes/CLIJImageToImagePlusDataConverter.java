package org.hkijena.jipipe.extensions.clij2.datatypes;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataConverter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.utils.ReflectionUtils;

/**
 * Converter from {@link CLIJImageData} to {@link ImagePlusData}
 */
public class CLIJImageToImagePlusDataConverter implements JIPipeDataConverter {
    private final Class<? extends JIPipeData> outputType;

    /**
     * @param outputType the output type
     */
    public CLIJImageToImagePlusDataConverter(Class<? extends JIPipeData> outputType) {
        this.outputType = outputType;
    }

    @Override
    public Class<? extends JIPipeData> getInputType() {
        return CLIJImageData.class;
    }

    @Override
    public Class<? extends JIPipeData> getOutputType() {
        return outputType;
    }

    @Override
    public JIPipeData convert(JIPipeData input) {
        return (JIPipeData) ReflectionUtils.newInstance(outputType, ((CLIJImageData) input).pull().getImage());
    }
}
