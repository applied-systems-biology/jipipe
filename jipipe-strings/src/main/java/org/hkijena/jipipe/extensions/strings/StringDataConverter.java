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

package org.hkijena.jipipe.extensions.strings;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataConverter;
import org.hkijena.jipipe.utils.ReflectionUtils;

public class StringDataConverter implements JIPipeDataConverter {
    private final Class<? extends JIPipeData> outputType;

    public StringDataConverter(Class<? extends JIPipeData> outputType) {
        this.outputType = outputType;
    }

    @Override
    public Class<? extends JIPipeData> getInputType() {
        return StringData.class;
    }

    @Override
    public Class<? extends JIPipeData> getOutputType() {
        return outputType;
    }

    @Override
    public JIPipeData convert(JIPipeData input) {
        return (JIPipeData) ReflectionUtils.newInstance(outputType, ((StringData) input).getData());
    }
}