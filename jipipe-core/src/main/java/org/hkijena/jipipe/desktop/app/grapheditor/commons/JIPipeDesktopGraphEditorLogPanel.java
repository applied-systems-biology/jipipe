package org.hkijena.jipipe.desktop.app.grapheditor.commons;

import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.running.queue.JIPipeDesktopRunnableQueuePanelUI;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopGraphEditorLogPanel extends JIPipeDesktopWorkbenchPanel {

    private final JCheckBox autoShowProgress = new JCheckBox("Auto show progress");
    private final JCheckBox autoShowResults = new JCheckBox("Auto show results");

    public JIPipeDesktopGraphEditorLogPanel(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        initialize();
    }

    private void initialize() {
        autoShowProgress.setSelected(true);
        autoShowResults.setSelected(true);

        setLayout(new BorderLayout());
        add(new JIPipeDesktopRunnableQueuePanelUI(), BorderLayout.CENTER);

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        add(toolbar, BorderLayout.NORTH);

        toolbar.add(autoShowProgress);
        toolbar.add(autoShowResults);
    }

    public boolean isAutoShowProgress() {
        return autoShowProgress.isSelected();
    }

    public boolean isAutoShowResults() {
        return autoShowResults.isSelected();
    }
}
