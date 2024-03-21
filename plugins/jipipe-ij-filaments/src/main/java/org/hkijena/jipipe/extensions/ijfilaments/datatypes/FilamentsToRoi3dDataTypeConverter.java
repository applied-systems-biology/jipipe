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

package org.hkijena.jipipe.extensions.ijfilaments.datatypes;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataConverter;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;

public class FilamentsToRoi3dDataTypeConverter implements JIPipeDataConverter {
    @Override
    public Class<? extends JIPipeData> getInputType() {
        return Filaments3DData.class;
    }

    @Override
    public Class<? extends JIPipeData> getOutputType() {
        return ROI3DListData.class;
    }

    @Override
    public JIPipeData convert(JIPipeData input, JIPipeProgressInfo progressInfo) {
        return ((Filaments3DData) input).toRoi3D(false, true, -1, -1, progressInfo);
    }
}
