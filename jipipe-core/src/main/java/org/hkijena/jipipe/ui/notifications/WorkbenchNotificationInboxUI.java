package org.hkijena.jipipe.ui.notifications;

import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.extensions.settings.NotificationUISettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

public class WorkbenchNotificationInboxUI extends JIPipeWorkbenchPanel implements JIPipeNotificationInbox.PushedEventListener, JIPipeNotificationInbox.DismissedEventListener {

    private final FormPanel notificationsPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);
    private final FormPanel dismissedNotificationsPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);
    private final FormPanel hiddenNotificationsPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);
    private final List<JIPipeNotification> dismissedNotifications = new ArrayList<>();
    private boolean hasNotifications = false;

    /**
     * @param workbench the workbench
     */
    public WorkbenchNotificationInboxUI(JIPipeWorkbench workbench) {
        super(workbench);
        initialize();
        updateNotifications();

        JIPipeNotificationInbox.getInstance().getPushedEventEmitter().subscribeWeak(this);
        JIPipeNotificationInbox.getInstance().getDismissedEventEmitter().subscribeWeak(this);
        getWorkbench().getNotificationInbox().getPushedEventEmitter().subscribeWeak(this);
        getWorkbench().getNotificationInbox().getDismissedEventEmitter().subscribeWeak(this);
    }

    public boolean isHasNotifications() {
        return hasNotifications;
    }

    public void updateNotifications() {
        notificationsPanel.clear();
        dismissedNotificationsPanel.clear();
        hiddenNotificationsPanel.clear();
        hasNotifications = false;

        Set<JIPipeNotification> notificationSet = new TreeSet<>();
        if (NotificationUISettings.getInstance().isEnableNotifications()) {
            notificationSet.addAll(JIPipeNotificationInbox.getInstance().getNotifications());
            notificationSet.addAll(getWorkbench().getNotificationInbox().getNotifications());
        }

        Set<String> blockedIds = new HashSet<>(NotificationUISettings.getInstance().getBlockedNotifications());

        for (JIPipeNotification notification : notificationSet) {
            if (blockedIds.contains(notification.getId())) {
                hiddenNotificationsPanel.addWideToForm(new WorkbenchNotificationUI(this,
                                notification,
                                true,
                                false),
                        null);
            } else {
                notificationsPanel.addWideToForm(new WorkbenchNotificationUI(this,
                                notification,
                                false,
                                false),
                        null);
                hasNotifications = true;
            }
        }
        for (JIPipeNotification notification : dismissedNotifications) {
            dismissedNotificationsPanel.addWideToForm(new WorkbenchNotificationUI(this,
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

            JLabel infoLabel = new JLabel("There are no notifications. You can close this tab.");
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

        JSplitPane splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT, documentTabPane,
                new MarkdownReader(false, MarkdownDocument.fromPluginResource("documentation/notifications.md", Collections.emptyMap())), AutoResizeSplitPane.RATIO_3_TO_1);
        add(splitPane, BorderLayout.CENTER);
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
