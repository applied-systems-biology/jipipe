/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.pipelinej.extensions.filesystem.algorithms;

import org.hkijena.pipelinej.api.data.ACAQData;
import org.hkijena.pipelinej.api.data.ACAQDataConverter;
import org.hkijena.pipelinej.extensions.filesystem.dataypes.PathData;

/**
 * Converts between the {@link org.hkijena.pipelinej.extensions.filesystem.dataypes.PathData} types (non-trivial conversions)
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
