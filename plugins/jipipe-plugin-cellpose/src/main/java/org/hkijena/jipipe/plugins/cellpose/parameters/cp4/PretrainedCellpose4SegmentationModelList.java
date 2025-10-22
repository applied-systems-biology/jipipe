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

package org.hkijena.jipipe.plugins.cellpose.parameters.cp4;

import org.hkijena.jipipe.plugins.cellpose.parameters.PretrainedCellposeModelEnumItemInfo;
import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;
import org.hkijena.jipipe.plugins.parameters.api.enums.EnumParameterSettings;

@EnumParameterSettings(itemInfo = PretrainedCellposeModelEnumItemInfo.class)
public class PretrainedCellpose4SegmentationModelList extends JIPipeListParameter<PretrainedCellpose4SegmentationModel> {
    public PretrainedCellpose4SegmentationModelList() {
        super(PretrainedCellpose4SegmentationModel.class);
    }

    public PretrainedCellpose4SegmentationModelList(PretrainedCellpose4SegmentationModelList other) {
        super(PretrainedCellpose4SegmentationModel.class);
        addAll(other);
    }
}
