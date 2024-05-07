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

package org.hkijena.jipipe.api.settings;

import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

/**
 * Application settings sheet of type {@link AbstractJIPipeParameterCollection}, pre-configured to be
 */
public abstract class JIPipeDefaultApplicationsSettingsSheet extends AbstractJIPipeParameterCollection implements JIPipeApplicationSettingsSheet {

    /**
     * Returns one of the predefined categories
     * @return the category
     */
    public abstract JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory();

    @Override
    public Icon getCategoryIcon() {
        return getDefaultCategory().getIcon();
    }

    @Override
    public String getCategory() {
        return getDefaultCategory().getCategory();
    }
}
