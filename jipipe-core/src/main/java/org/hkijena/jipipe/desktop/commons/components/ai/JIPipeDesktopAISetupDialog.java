package org.hkijena.jipipe.desktop.commons.components.ai;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class JIPipeDesktopAISetupDialog extends JDialog {
    private final JIPipeDesktopWorkbench workbench;

    public JIPipeDesktopAISetupDialog(JIPipeDesktopWorkbench workbench) {
        super(workbench.getWindow());
        this.workbench = workbench;
        initialize();
    }

    private void initialize() {
        setTitle("JIPipe - AI setup");
        setIconImage(UIUtils.getJIPipeIcon128());
    }

    public static void checkFirstTimeSetup(JIPipeDesktopWorkbench workbench) {
        if(JIPipe.getInstance().getAiService().hasConfiguredAndReadyEmbeddingModel()) {
            return;
        }
        // User is shown the dialog
        JIPipeDesktopAISetupDialog dialog = new JIPipeDesktopAISetupDialog(workbench);
        dialog.pack();
        dialog.setSize(1024,768);
        dialog.setLocationRelativeTo(workbench.getWindow());
        dialog.setModal(true);
        dialog.setVisible(true);
    }
}
