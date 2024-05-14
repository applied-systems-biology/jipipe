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

import javax.swing.*;

/**
 * A settings sheet
 */
public interface JIPipeSettingsSheet {

    /**
     * The unique ID of the settings sheet
     * @return the ID
     */
    String getId();

    /**
     * The icon of the settings sheet
     * @return the icon
     */
    Icon getIcon();

    /**
     * The name of the settings sheet as displayed in the UI
     * @return the name
     */
    String getName();

    /**
     * The category of the settings sheet as displayed in the UI
     * @return the category
     */
    String getCategory();

    /**
     * The icon for the category as displayed in the UI
     * @return the category icon
     */
    Icon getCategoryIcon();

    /**
     * Description of this settings sheet
     * @return the description
     */
    String getDescription();
}
