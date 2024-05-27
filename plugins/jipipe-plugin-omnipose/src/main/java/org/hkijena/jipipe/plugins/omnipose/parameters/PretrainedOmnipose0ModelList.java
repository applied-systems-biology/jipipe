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

package org.hkijena.jipipe.plugins.omnipose.parameters;

import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.plugins.parameters.api.enums.EnumParameterSettings;

@EnumParameterSettings(itemInfo = PretrainedOmnipose0ModelEnumItemInfo.class)
public class PretrainedOmnipose0ModelList extends ListParameter<PretrainedOmnipose0Model> {
    public PretrainedOmnipose0ModelList() {
        super(PretrainedOmnipose0Model.class);
    }

    public PretrainedOmnipose0ModelList(PretrainedOmnipose0ModelList other) {
        super(PretrainedOmnipose0Model.class);
        addAll(other);
    }
}
