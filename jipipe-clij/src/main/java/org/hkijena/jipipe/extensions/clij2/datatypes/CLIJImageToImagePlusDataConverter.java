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

package org.hkijena.jipipe.extensions.clij2.datatypes;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
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
    public JIPipeData convert(JIPipeData input, JIPipeProgressInfo progressInfo) {
        return (JIPipeData) ReflectionUtils.newInstance(outputType, ((CLIJImageData) input).pull().getImage());
    }
}
