package org.hkijena.acaq5.extensions.filesystem.algorithms;

import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataConverter;
import org.hkijena.acaq5.extensions.filesystem.dataypes.PathData;

/**
 * Converts between the {@link org.hkijena.acaq5.extensions.filesystem.dataypes.PathData} types (non-trivial conversions)
 */
public class ImplicitPathTypeConverter implements ACAQDataConverter {
    private Class<? extends ACAQData> inputType;
    private Class<? extends ACAQData> outputType;

    /**
     * @param inputType  the input type
     * @param outputType the output type
     */
    public ImplicitPathTypeConverter(Class<? extends ACAQData> inputType, Class<? extends ACAQData> outputType) {
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
        return ACAQData.createInstance(outputType, ((PathData) input).getPath());
    }
}
