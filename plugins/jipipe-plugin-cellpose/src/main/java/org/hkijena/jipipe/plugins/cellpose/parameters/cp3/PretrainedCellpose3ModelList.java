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

package org.hkijena.jipipe.plugins.cellpose.parameters.cp3;

import org.hkijena.jipipe.plugins.cellpose.parameters.cp2.PretrainedCellpose2Model;
import org.hkijena.jipipe.plugins.cellpose.parameters.cp2.PretrainedCellpose2ModelEnumItemInfo;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.plugins.parameters.api.enums.EnumParameterSettings;

@EnumParameterSettings(itemInfo = PretrainedCellpose2ModelEnumItemInfo.class)
public class PretrainedCellpose3ModelList extends ListParameter<PretrainedCellpose3Model> {
    public PretrainedCellpose3ModelList() {
        super(PretrainedCellpose3Model.class);
    }

    public PretrainedCellpose3ModelList(PretrainedCellpose3ModelList other) {
        super(PretrainedCellpose3Model.class);
        addAll(other);
    }
}
