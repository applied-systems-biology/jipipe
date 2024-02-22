/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorUI;

/**
 * All settings for {@link JIPipeGraphEditorUI}
 */
public class HistoryJournalSettings extends AbstractJIPipeParameterCollection {

    public static String ID = "history-journal";
    private int maxEntries = 50;

    public static HistoryJournalSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, HistoryJournalSettings.class);
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
}
