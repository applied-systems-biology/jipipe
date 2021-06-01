package org.hkijena.jipipe.extensions.tools;

import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.extension.MenuExtension;
import org.hkijena.jipipe.ui.extension.MenuTarget;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Path;

@JIPipeOrganization(menuExtensionTarget = MenuTarget.ProjectMainMenu)
public class SaveAllCacheTool extends MenuExtension {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public SaveAllCacheTool(JIPipeWorkbench workbench) {
        super(workbench);
        setText("Save project and cache");
        setToolTipText("Saves all the currently cached data of all nodes. Only the newest cache is saved.");
        setIcon(UIUtils.getIconFromResources("actions/save.png"));
        addActionListener(e -> saveAllCaches());
    }

    private void saveAllCaches() {
        Path outputPath = FileChooserSettings.saveDirectory(SwingUtilities.getWindowAncestor(this), FileChooserSettings.KEY_DATA, "Save project and cache");
        if(outputPath != null) {
            SaveAllCachedDataRun run = new SaveAllCachedDataRun(getWorkbench(), ((JIPipeProjectWorkbench) getWorkbench()).getProject(), outputPath);
            JIPipeRunExecuterUI.runInDialog(getWorkbench().getWindow(), run);
        }
    }
}
