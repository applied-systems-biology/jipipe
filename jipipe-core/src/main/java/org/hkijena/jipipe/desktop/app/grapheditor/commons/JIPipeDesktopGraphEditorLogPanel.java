package org.hkijena.jipipe.desktop.app.grapheditor.commons;

import org.hkijena.jipipe.api.run.JIPipeQueuedRunnableExecutor;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.running.queue.JIPipeDesktopRunQueueLoggerPanel;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopGraphEditorLogPanel extends JIPipeDesktopWorkbenchPanel {

    private final JIPipeDesktopRunQueueLoggerPanel loggerPanel;

    public JIPipeDesktopGraphEditorLogPanel(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        this.loggerPanel = new JIPipeDesktopRunQueueLoggerPanel(workbench, JIPipeQueuedRunnableExecutor.getInstance());
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(loggerPanel, BorderLayout.CENTER);

        // The Show log / Auto show progress / Auto view results toggles are provided by the
        // progress tree panel's header; the editor only preserves its previous defaults here
        // (the run managers read them via the getters below)
        loggerPanel.setAutoShowProgressSelected(true);
        loggerPanel.setAutoViewResultsSelected(true);
        loggerPanel.addDefaultCancelButton();
    }

    public boolean isAutoShowProgress() {
        return loggerPanel.isAutoShowProgress();
    }

    public boolean isAutoShowResults() {
        return loggerPanel.isAutoViewResults();
    }
}
