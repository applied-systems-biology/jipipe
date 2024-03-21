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

package org.hkijena.jipipe.extensions.imagej2.converters;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataConverter;
import org.hkijena.jipipe.extensions.imagej2.datatypes.ImageJ2DatasetData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

public class ImageJ2ToImageJ1Converter implements JIPipeDataConverter {
    @Override
    public Class<? extends JIPipeData> getInputType() {
        return ImageJ2DatasetData.class;
    }

    @Override
    public Class<? extends JIPipeData> getOutputType() {
        return ImagePlusData.class;
    }

    @Override
    public JIPipeData convert(JIPipeData input, JIPipeProgressInfo progressInfo) {
        return ((ImageJ2DatasetData) input).wrap();
    }
}
