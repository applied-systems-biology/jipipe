package org.hkijena.jipipe.ui;

import javax.swing.*;

/**
 * A {@link JPanel} that implements {@link JIPipeWorkbenchAccess}
 */
public class JIPipeWorkbenchPanel extends JPanel implements JIPipeWorkbenchAccess {
    private final JIPipeWorkbench workbench;

    public JIPipeWorkbenchPanel(JIPipeWorkbench workbench) {
        this.workbench = workbench;
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }
}
