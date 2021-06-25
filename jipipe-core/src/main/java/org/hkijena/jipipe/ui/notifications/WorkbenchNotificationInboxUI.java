package org.hkijena.jipipe.ui.notifications;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.extensions.settings.NotificationUISettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.MarkdownReader;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.*;
import java.util.List;

public class WorkbenchNotificationInboxUI extends JIPipeWorkbenchPanel {

    private FormPanel notificationsPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);
    private FormPanel dismissedNotificationsPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);
    private FormPanel hiddenNotificationsPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);
    private List<JIPipeNotification> dismissedNotifications = new ArrayList<>();

    /**
     * @param workbench the workbench
     */
    public WorkbenchNotificationInboxUI(JIPipeWorkbench workbench) {
        super(workbench);
        initialize();
        updateNotifications();

        JIPipeNotificationInbox.getInstance().getEventBus().register(this);
        getWorkbench().getNotificationInbox().getEventBus().register(this);
    }

    public void updateNotifications() {
        notificationsPanel.clear();
        dismissedNotificationsPanel.clear();
        hiddenNotificationsPanel.clear();

        Set<JIPipeNotification> notificationSet = new TreeSet<>();
        if(NotificationUISettings.getInstance().isEnableNotifications()) {
            notificationSet.addAll(JIPipeNotificationInbox.getInstance().getNotifications());
            notificationSet.addAll(getWorkbench().getNotificationInbox().getNotifications());
        }

        Set<String> blockedIds = new HashSet<>(NotificationUISettings.getInstance().getBlockedNotifications());

        for (JIPipeNotification notification : notificationSet) {
            if(blockedIds.contains(notification.getId())) {
                hiddenNotificationsPanel.addWideToForm(new NotificationUI(this,
                                notification,
                                true,
                                false),
                        null);
            }
            else {
                notificationsPanel.addWideToForm(new NotificationUI(this,
                                notification,
                                false,
                                false),
                        null);
            }
        }
        for (JIPipeNotification notification : dismissedNotifications) {
            dismissedNotificationsPanel.addWideToForm(new NotificationUI(this,
                            notification,
                            false,
                            true),
                    null);
        }

        notificationsPanel.addVerticalGlue();
        dismissedNotificationsPanel.addVerticalGlue();
        hiddenNotificationsPanel.addVerticalGlue();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        DocumentTabPane documentTabPane = new DocumentTabPane();
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

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, documentTabPane,
                new MarkdownReader(false, MarkdownDocument.fromPluginResource("documentation/notifications.md", Collections.emptyMap())));
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.66);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.66);
            }
        });
        add(splitPane, BorderLayout.CENTER);
    }

    @Subscribe
    public void onNotificationsChanged(JIPipeNotificationInbox.UpdatedEvent event) {
        updateNotifications();
    }

    public List<JIPipeNotification> getDismissedNotifications() {
        return dismissedNotifications;
    }
}
