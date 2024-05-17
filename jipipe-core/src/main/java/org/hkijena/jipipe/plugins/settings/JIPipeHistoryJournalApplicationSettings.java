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
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

/**
 * All settings for {@link JIPipeDesktopGraphEditorUI}
 */
public class JIPipeHistoryJournalApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    public static String ID = "org.hkijena.jipipe:history-journal";
    private int maxEntries = 50;

    public static JIPipeHistoryJournalApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, JIPipeHistoryJournalApplicationSettings.class);
    }

    @SetJIPipeDocumentation(name = "Maximum number of entries", description = "Determines how many operations are logged in the " +
            "journal functionality. Set to a negative value to disable limitations. Set to zero to disable the journal functionality.")
    @JIPipeParameter("max-entries")
    public int getMaxEntries() {
        return maxEntries;
    }

    @JIPipeParameter("max-entries")
    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.General;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/edit-undo-history.png");
    }

    @Override
    public String getName() {
        return "Journal";
    }

    @Override
    public String getDescription() {
        return "Configure the undo/redo behavior (e.g., how many steps are saved)";
    }
}
