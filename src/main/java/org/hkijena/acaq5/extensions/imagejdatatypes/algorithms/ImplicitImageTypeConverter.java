package org.hkijena.acaq5.extensions.imagejdatatypes.algorithms;

import ij.ImagePlus;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataConverter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;

/**
 * An implicit converter between {@link org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData} types
 */
public class ImplicitImageTypeConverter implements ACAQDataConverter {

    private Class<? extends ACAQData> inputType;
    private Class<? extends ACAQData> outputType;

    /**
     * @param inputType  the input type
     * @param outputType the output type
     */
    public ImplicitImageTypeConverter(Class<? extends ACAQData> inputType, Class<? extends ACAQData> outputType) {
        this.inputType = inputType;
        this.outputType = outputType;
    }

    @Override
    public Class<? extends ACAQData> getInputType() {
        return inputType;
    }

    @Override
    public Class<? extends ACAQData> getOutputType() {
        return outputType;
    }

    @Override
    public ACAQData convert(ACAQData input) {
        ImagePlus img = ((ImagePlusData) input).getImage();
        return ACAQData.createInstance(outputType, img);
    }
}
