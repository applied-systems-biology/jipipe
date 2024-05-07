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

package org.hkijena.jipipe.desktop.commons.notifications;

import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.settings.JIPipeNotificationUIApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

public class JIPipeDesktopGenericNotificationInboxUI extends JIPipeDesktopWorkbenchPanel implements JIPipeNotificationInbox.PushedEventListener, JIPipeNotificationInbox.DismissedEventListener {

    private final JIPipeDesktopFormPanel notificationsPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.WITH_SCROLLING);
    private final JIPipeDesktopFormPanel dismissedNotificationsPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.WITH_SCROLLING);
    private final JIPipeDesktopFormPanel hiddenNotificationsPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.WITH_SCROLLING);
    private final List<JIPipeNotification> dismissedNotifications = new ArrayList<>();
    private final JIPipeNotificationInbox inbox;

    public JIPipeDesktopGenericNotificationInboxUI(JIPipeDesktopWorkbench workbench, JIPipeNotificationInbox inbox) {
        super(workbench);
        this.inbox = inbox;
        initialize();
        updateNotifications();
        inbox.getPushedEventEmitter().subscribeWeak(this);
        inbox.getDismissedEventEmitter().subscribeWeak(this);
    }

    public void updateNotifications() {
        notificationsPanel.clear();
        dismissedNotificationsPanel.clear();
        hiddenNotificationsPanel.clear();

        Set<JIPipeNotification> notificationSet = new TreeSet<>();
        if (JIPipeNotificationUIApplicationSettings.getInstance().isEnableNotifications()) {
            notificationSet.addAll(inbox.getNotifications());
        }

        Set<String> blockedIds = new HashSet<>(JIPipeNotificationUIApplicationSettings.getInstance().getBlockedNotifications());

        boolean hasNotifications = false;
        for (JIPipeNotification notification : notificationSet) {
            if (blockedIds.contains(notification.getId())) {
                hiddenNotificationsPanel.addWideToForm(new JIPipeDesktopGenericNotificationUI(this,
                                notification,
                                true,
                                false),
                        null);
            } else {
                notificationsPanel.addWideToForm(new JIPipeDesktopGenericNotificationUI(this,
                                notification,
                                false,
                                false),
                        null);
                hasNotifications = true;
            }
        }
        for (JIPipeNotification notification : dismissedNotifications) {
            dismissedNotificationsPanel.addWideToForm(new JIPipeDesktopGenericNotificationUI(this,
                            notification,
                            false,
                            true),
                    null);
        }

        if (!hasNotifications) {
            JPanel noNotificationPanel = new JPanel(new BorderLayout());
            noNotificationPanel.setLayout(new BorderLayout());
            noNotificationPanel.setBorder(UIUtils.createControlBorder());

            JLabel label = new JLabel("No notifications", UIUtils.getIcon64FromResources("check-circle-green.png"), JLabel.LEFT);
            label.setFont(label.getFont().deriveFont(26.0f));
            label.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            noNotificationPanel.add(label, BorderLayout.CENTER);

            JLabel infoLabel = new JLabel("There are no notifications.");
            infoLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            noNotificationPanel.add(infoLabel, BorderLayout.SOUTH);

            notificationsPanel.addWideToForm(noNotificationPanel, null);
        }

        notificationsPanel.addVerticalGlue();
        dismissedNotificationsPanel.addVerticalGlue();
        hiddenNotificationsPanel.addVerticalGlue();

        revalidate();
        repaint();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JIPipeDesktopTabPane documentTabPane = new JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.TabPlacement.Left);

        documentTabPane.addTab("Current",
                UIUtils.getIcon32FromResources("actions/bell.png"),
                notificationsPanel,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
                false);
        documentTabPane.addTab("Dismissed",
                UIUtils.getIcon32FromResources("actions/checkmark.png"),
                dismissedNotificationsPanel,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
                false);
        documentTabPane.addTab("Hidden",
                UIUtils.getIcon32FromResources("actions/eye-slash.png"),
                hiddenNotificationsPanel,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
                false);

        add(documentTabPane, BorderLayout.CENTER);
    }

    @Override
    public void onNotificationPushed(JIPipeNotificationInbox.PushedEvent event) {
        updateNotifications();
    }

    @Override
    public void onNotificationDismissed(JIPipeNotificationInbox.DismissedEvent event) {
        dismissedNotifications.add(event.getNotification());
        updateNotifications();
    }
}
