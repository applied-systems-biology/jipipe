package org.hkijena.jipipe.ui.quickrun;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class QuickRunSetupWindow extends JDialog {

    private final JIPipeWorkbench workbench;
    private boolean cancelled = true;

    public QuickRunSetupWindow(JIPipeWorkbench workbench) {
        super(workbench.getWindow());
        this.workbench = workbench;
        initialize();
    }

    private void initialize() {
        getContentPane().setLayout(new BorderLayout());
        setTitle("Quick run / Update cache");
        setSize(800, 600);
        setModal(true);
        setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());

        // Initialize form panel
        FormPanel formPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);

        {
            JLabel label = new JLabel("<html><div style=\"font-size: 26px;\">Before you start ...</div>" +
                    "<div>Please review these settings before you run the pipeline. Here you can increase the number of processing threads to increase the speed and " +
                    "change where JIPipe stores files that are generated during the analysis.</div></html>", UIUtils.getIcon64FromResources("check-circle-green.png"), JLabel.LEFT);
            formPanel.addWideToForm(label, null);
        }

        FormPanel.GroupHeaderPanel numThreadsHeader = formPanel.addGroupHeader("Number of threads", UIUtils.getIconFromResources("devices/cpu.png"));
        numThreadsHeader.setDescription("<html>Certain nodes can distribute their workload across multiple processing cores, significantly decreasing the runtime of your pipeline. " +
                "Use the following setting to increase the number of cores JIPipe will use.<br/>Please note that operations running in parallel will require </i>more memory</i>. " +
                "If you are unsure, set this number to <code>1</code>.</html>");

        formPanel.addToForm(JIPipe.getParameterTypes().createEditorFor(workbench, RuntimeSettings.getInstance().getParameterAccess("default-test-bench-threads")), new JLabel("Number of threads"), null);

        FormPanel.GroupHeaderPanel tempLocationHeader = formPanel.addGroupHeader("Temporary storage location", UIUtils.getIconFromResources("places/folder.png"));
        tempLocationHeader.setDescription("<html>JIPipe will store intermediate files into a directory that by default is determined by your system. If the system hard drive partition is too small, " +
                "your pipeline will run out of storage and crash. If you are working with large data sets, we recommend to override the temporary directory to a drive with enough space.</html>");

        formPanel.addToForm(JIPipe.getParameterTypes().createEditorFor(workbench, RuntimeSettings.getInstance().getParameterAccess("temp-directory")), new JLabel("Override temporary directory"), null);

        formPanel.addVerticalGlue();

        getContentPane().add(formPanel, BorderLayout.CENTER);

        // Initialize button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        buttonPanel.add(new JLabel("<html>All settings can be changed afterwards by visiting <i>Project &gt; Application settings &gt; General &gt; Runtime</i></html>"));

        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            cancelled = true;
            setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("Save settings and run", UIUtils.getIconFromResources("actions/run-build.png"));
        confirmButton.addActionListener(e -> {
            cancelled = false;
            RuntimeSettings.getInstance().setShowQuickRunSetupWindow(false);
            RuntimeSettings.getInstance().triggerParameterChange("show-quick-run-setup-window");
            setVisible(false);
        });
        buttonPanel.add(confirmButton);

        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }
}
