package org.hkijena.jipipe.desktop.app.customizer;

import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopCustomizerDialog extends JDialog implements JIPipeDesktopWorkbenchAccess {

    private final JIPipeDesktopWorkbench workbench;

    public JIPipeDesktopCustomizerDialog(JIPipeDesktopWorkbench workbench) {
        super(workbench.getWindow());
        this.workbench = workbench;
        initialize();
    }

    private void initialize() {
        setTitle("Customize JIPipe");
        setIconImage(UIUtils.getJIPipeIcon128());
        setModal(true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        pack();
        setSize(1024,768);
        setLocationRelativeTo(workbench.getWindow());
    }

    @Override
    public JIPipeDesktopWorkbench getWorkbench() {
        return workbench;
    }

    @Override
    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return workbench;
    }
}
