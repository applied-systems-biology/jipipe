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

import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

/**
 * Pre-defined application setting sheet categories to be used with {@link JIPipeDefaultProjectSettingsSheet}
 */
public enum JIPipeDefaultProjectSettingsSheetCategory {
    General("General", UIUtils.getIconFromResources("actions/wrench.png")),
    Data("Data", UIUtils.getIconFromResources("actions/update-cache.png")),
    Plugins("Plugins", UIUtils.getIconFromResources("actions/puzzle-piece.png")),
    Samples("Samples", UIUtils.getIconFromResources("actions/template.png")),
    ImageViewer("Image viewer", UIUtils.getIconFromResources("actions/image.png")),
    UI("User interface", UIUtils.getIconFromResources("actions/arrow-pointer.png"));

    private final String category;
    private final Icon icon;

    JIPipeDefaultProjectSettingsSheetCategory(String category, Icon icon) {

        this.category = category;
        this.icon = icon;
    }

    public String getCategory() {
        return category;
    }

    public Icon getIcon() {
        return icon;
    }
}
