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

package org.hkijena.jipipe.ui.notifications;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.extensions.settings.NotificationUISettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.icons.AnimatedIcon;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * UI that monitors the queue
 */
public class GenericNotificationButton extends JButton implements JIPipeNotificationInbox.UpdatedEventListener {

    private final JIPipeWorkbench workbench;
    private final JIPipeNotificationInbox inbox;
    private AnimatedIcon warningIcon;

    /**
     * @param workbench the workbench
     */
    public GenericNotificationButton(JIPipeWorkbench workbench, JIPipeNotificationInbox inbox) {
        this.workbench = workbench;
        this.inbox = inbox;
        initialize();
        updateStatus();

        inbox.getUpdatedEventEmitter().subscribeWeak(this);
        addActionListener(e -> openNotifications());
    }

    private void openNotifications() {
        JFrame frame = new JFrame();
        frame.setTitle("Notifications");
        frame.setIconImage(UIUtils.getJIPipeIcon128());
        frame.setContentPane(new GenericNotificationInboxUI(workbench, inbox));
        frame.pack();
        frame.setSize(800,600);
        frame.setLocationRelativeTo(workbench.getWindow());
        frame.setVisible(true);
    }

    private void updateStatus() {
        warningIcon.stop();

        if (inbox.isEmpty()) {
            setText("");
            setIcon(UIUtils.getIconFromResources("actions/circle-check.png"));
        } else {
            setText("Notifications");
            setIcon(warningIcon);
            warningIcon.start();
        }
    }

    private void initialize() {
        UIUtils.setStandardButtonBorder(this);

        warningIcon = new AnimatedIcon(this, UIUtils.getIconFromResources("emblems/emblem-important.png"),
                UIUtils.getIconFromResources("emblems/warning.png"),
                100, 0.05);
        setIcon(warningIcon);
        setHorizontalAlignment(LEFT);
    }

    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    @Override
    public void onNotificationInboxUpdated(JIPipeNotificationInbox.UpdatedEvent event) {
        updateStatus();
    }
}
