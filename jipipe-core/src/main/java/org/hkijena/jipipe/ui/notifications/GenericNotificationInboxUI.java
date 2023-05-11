package org.hkijena.jipipe.ui.notifications;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.extensions.settings.NotificationUISettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

public class GenericNotificationInboxUI extends JIPipeWorkbenchPanel implements JIPipeNotificationInbox.PushedEventListener, JIPipeNotificationInbox.DismissedEventListener {

    private final FormPanel notificationsPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);
    private final FormPanel dismissedNotificationsPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);
    private final FormPanel hiddenNotificationsPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);
    private final List<JIPipeNotification> dismissedNotifications = new ArrayList<>();
    private final JIPipeNotificationInbox inbox;

    public GenericNotificationInboxUI(JIPipeWorkbench workbench, JIPipeNotificationInbox inbox) {
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
        if (NotificationUISettings.getInstance().isEnableNotifications()) {
            notificationSet.addAll(inbox.getNotifications());
        }

        Set<String> blockedIds = new HashSet<>(NotificationUISettings.getInstance().getBlockedNotifications());

        boolean hasNotifications = false;
        for (JIPipeNotification notification : notificationSet) {
            if (blockedIds.contains(notification.getId())) {
                hiddenNotificationsPanel.addWideToForm(new GenericNotificationUI(this,
                                notification,
                                true,
                                false),
                        null);
            } else {
                notificationsPanel.addWideToForm(new GenericNotificationUI(this,
                                notification,
                                false,
                                false),
                        null);
                hasNotifications = true;
            }
        }
        for (JIPipeNotification notification : dismissedNotifications) {
            dismissedNotificationsPanel.addWideToForm(new GenericNotificationUI(this,
                            notification,
                            false,
                            true),
                    null);
        }

        if (!hasNotifications) {
            JPanel noNotificationPanel = new JPanel(new BorderLayout());
            noNotificationPanel.setLayout(new BorderLayout());
            noNotificationPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1, true));

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
    }

    private void initialize() {
        setLayout(new BorderLayout());
        DocumentTabPane documentTabPane = new DocumentTabPane(true);
        documentTabPane.getTabbedPane().setTabPlacement(SwingConstants.BOTTOM);

        documentTabPane.addTab("Current notifications",
                UIUtils.getIconFromResources("actions/bell.png"),
                notificationsPanel,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);
        documentTabPane.addTab("Dismissed notifications",
                UIUtils.getIconFromResources("actions/checkmark.png"),
                dismissedNotificationsPanel,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);
        documentTabPane.addTab("Hidden notifications",
                UIUtils.getIconFromResources("actions/eye-slash.png"),
                hiddenNotificationsPanel,
                DocumentTabPane.CloseMode.withoutCloseButton,
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
