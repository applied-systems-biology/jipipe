package org.hkijena.jipipe.ui.components;

import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchAccess;

/**
 * A content panel with access to a {@link JIPipeWorkbench}
 */
public class FlexContentWorkbenchPanel extends FlexContentPanel implements JIPipeWorkbenchAccess {

    private final JIPipeWorkbench workbench;

    public FlexContentWorkbenchPanel(JIPipeWorkbench workbench) {
        this(workbench, NONE);
    }

    public FlexContentWorkbenchPanel(JIPipeWorkbench workbench, int flags) {
        super(flags);
        this.workbench = workbench;
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }
}
