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

package org.hkijena.jipipe.plugins.cellpose.parameters;

import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.plugins.parameters.api.enums.EnumParameterSettings;

@EnumParameterSettings(itemInfo = PretrainedCellpose2ModelEnumItemInfo.class)
public class PretrainedCellpose2ModelList extends ListParameter<PretrainedCellpose2Model> {
    public PretrainedCellpose2ModelList() {
        super(PretrainedCellpose2Model.class);
    }

    public PretrainedCellpose2ModelList(PretrainedCellpose2ModelList other) {
        super(PretrainedCellpose2Model.class);
        addAll(other);
    }
}
