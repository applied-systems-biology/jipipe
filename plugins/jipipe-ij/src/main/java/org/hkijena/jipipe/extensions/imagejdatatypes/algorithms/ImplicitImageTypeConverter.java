/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejdatatypes.algorithms;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataConverter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * An implicit converter between {@link org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData} types
 */
public class ImplicitImageTypeConverter implements JIPipeDataConverter {

    private final Method convertFunction;
    private Class<? extends JIPipeData> inputType;
    private Class<? extends JIPipeData> outputType;

    /**
     * @param inputType  the input type
     * @param outputType the output type
     */
    public ImplicitImageTypeConverter(Class<? extends JIPipeData> inputType, Class<? extends JIPipeData> outputType) {
        this.inputType = inputType;
        this.outputType = outputType;
        try {
            this.convertFunction = outputType.getDeclaredMethod("convertFrom", ImagePlusData.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Class<? extends JIPipeData> getInputType() {
        return inputType;
    }

    @Override
    public Class<? extends JIPipeData> getOutputType() {
        return outputType;
    }

    @Override
    public JIPipeData convert(JIPipeData input, JIPipeProgressInfo progressInfo) {
        try {
            return (JIPipeData) convertFunction.invoke(null, (ImagePlusData) input);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
