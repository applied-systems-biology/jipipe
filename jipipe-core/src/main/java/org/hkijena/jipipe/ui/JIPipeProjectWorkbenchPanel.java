package org.hkijena.jipipe.ui;

import javax.swing.*;

/**
 * A {@link JPanel} that implements {@link JIPipeProjectWorkbenchAccess}
 */
public class JIPipeProjectWorkbenchPanel extends JPanel implements JIPipeProjectWorkbenchAccess {
    private final JIPipeWorkbench workbench;

    public JIPipeProjectWorkbenchPanel(JIPipeProjectWorkbench workbench) {
        this.workbench = workbench;
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }
}
