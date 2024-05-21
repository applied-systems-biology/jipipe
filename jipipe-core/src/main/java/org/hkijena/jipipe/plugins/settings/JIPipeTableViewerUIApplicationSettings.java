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

package org.hkijena.jipipe.plugins.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class JIPipeTableViewerUIApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    public static String ID = "org.hkijena.jipipe:table-viewer-ui";
    private boolean showSideBar = true;

    public static JIPipeTableViewerUIApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, JIPipeTableViewerUIApplicationSettings.class);
    }

    @SetJIPipeDocumentation(name = "Show side bar", description = "If enabled, show a side bar with additional settings and tools")
    @JIPipeParameter("show-side-bar")
    public boolean isShowSideBar() {
        return showSideBar;
    }

    @JIPipeParameter("show-side-bar")
    public void setShowSideBar(boolean showSideBar) {
        this.showSideBar = showSideBar;
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.UI;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/table.png");
    }

    @Override
    public String getName() {
        return "Table viewer";
    }

    @Override
    public String getDescription() {
        return "Settings for the JIPipe table viewer";
    }
}
