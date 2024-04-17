/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.ilastik;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.notifications.JIPipeDesktopGenericNotificationInboxUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;

public class RunIlastikDesktopMenuExtension extends JIPipeDesktopMenuExtension implements ActionListener {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public RunIlastikDesktopMenuExtension(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        setText("Run Ilastik");
        setIcon(IlastikPlugin.RESOURCES.getIconFromResources("ilastik.png"));
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
        if (IlastikSettings.environmentSettingsAreValid()) {
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            progressInfo.setLogToStdOut(true);
            getDesktopWorkbench().sendStatusBarText("Launching Ilastik ...");
            IlastikPlugin.runIlastik(null, Collections.emptyList(), progressInfo, true);
        } else {
            JIPipeNotificationInbox inbox = new JIPipeNotificationInbox();
            IlastikPlugin.createMissingIlastikNotificationIfNeeded(inbox);
            JIPipeDesktopGenericNotificationInboxUI ui = new JIPipeDesktopGenericNotificationInboxUI(getDesktopWorkbench(), inbox);
            JFrame dialog = new JFrame();
            dialog.setTitle("Run Ilastik");
            dialog.setContentPane(ui);
            dialog.setIconImage(UIUtils.getJIPipeIcon128());
            dialog.pack();
            dialog.setSize(800, 600);
            dialog.setLocationRelativeTo(getDesktopWorkbench().getWindow());
            dialog.setVisible(true);
        }
    }
}