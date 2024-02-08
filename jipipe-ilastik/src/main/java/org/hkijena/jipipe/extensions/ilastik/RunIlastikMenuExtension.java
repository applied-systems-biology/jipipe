package org.hkijena.jipipe.extensions.ilastik;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtension;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.ui.notifications.GenericNotificationInboxUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;

public class RunIlastikMenuExtension extends JIPipeMenuExtension implements ActionListener {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public RunIlastikMenuExtension(JIPipeWorkbench workbench) {
        super(workbench);
        setText("Run Ilastik");
        setIcon(IlastikExtension.RESOURCES.getIconFromResources("ilastik.png"));
        setToolTipText("Starts a new instance of Ilastik.");
        addActionListener(this);
    }

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectToolsMenu;
    }

    @Override
    public String getMenuPath() {
        return "";
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(IlastikSettings.environmentSettingsAreValid()) {
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            progressInfo.setLogToStdOut(true);
            getWorkbench().sendStatusBarText("Launching Ilastik ...");
            IlastikExtension.runIlastik(null, Collections.emptyList(), progressInfo, true);
        }
        else {
            JIPipeNotificationInbox inbox = new JIPipeNotificationInbox();
            IlastikExtension.createMissingIlastikNotificationIfNeeded(inbox);
            GenericNotificationInboxUI ui = new GenericNotificationInboxUI(getWorkbench(), inbox);
            JFrame dialog = new JFrame();
            dialog.setTitle("Run Ilastik");
            dialog.setContentPane(ui);
            dialog.setIconImage(UIUtils.getJIPipeIcon128());
            dialog.pack();
            dialog.setSize(800,600);
            dialog.setLocationRelativeTo(getWorkbench().getWindow());
            dialog.setVisible(true);
        }
    }
}
