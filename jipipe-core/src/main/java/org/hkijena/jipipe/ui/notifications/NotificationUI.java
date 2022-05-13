package org.hkijena.jipipe.ui.notifications;

import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import org.hkijena.jipipe.extensions.settings.NotificationUISettings;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class NotificationUI extends JIPipeWorkbenchPanel {

    private final WorkbenchNotificationInboxUI inboxUI;
    private final JIPipeNotification notification;
    private final boolean blocked;
    private final boolean dismissed;

    /**
     * @param inboxUI      the workbench
     * @param notification the notification instance
     * @param blocked if blocked
     * @param dismissed if dismissed
     */
    public NotificationUI(WorkbenchNotificationInboxUI inboxUI, JIPipeNotification notification, boolean blocked, boolean dismissed) {
        super(inboxUI.getWorkbench());
        this.inboxUI = inboxUI;
        this.notification = notification;
        this.blocked = blocked;
        this.dismissed = dismissed;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.GRAY, 1, true));

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.X_AXIS));

        // Add heading
        JLabel headingLabel = new JLabel(notification.getHeading());
        headingLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        headingLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
        headerPanel.add(headingLabel);
        headerPanel.add(Box.createHorizontalGlue());
        add(headerPanel, BorderLayout.NORTH);

        if (this.blocked) {
            JButton unblockButton = new JButton("Unblock", UIUtils.getIconFromResources("actions/eye.png"));
            UIUtils.makeBorderlessWithoutMargin(unblockButton);
            unblockButton.setToolTipText("Unblocks this type of notification");
            unblockButton.addActionListener(e -> unblock());
            headerPanel.add(unblockButton);
        } else {
            JButton blockButton = new JButton(UIUtils.getIconFromResources("actions/eye-slash.png"));
            UIUtils.makeFlat25x25(blockButton);
            blockButton.setToolTipText("Blocks this type of notification");
            blockButton.addActionListener(e -> block());
            headerPanel.add(blockButton);

            if (!dismissed) {
                JButton dismissButton = new JButton(UIUtils.getIconFromResources("actions/close-tab.png"));
                UIUtils.makeFlat25x25(dismissButton);
                dismissButton.setToolTipText("Dismisses this notification");
                dismissButton.addActionListener(e -> dismiss());
                headerPanel.add(dismissButton);
            }
        }

        // Add content
        JTextPane textPane = UIUtils.makeBorderlessReadonlyTextPane(new MarkdownDocument(notification.getDescription()).getRenderedHTML());
        textPane.setOpaque(false);
        textPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(textPane, BorderLayout.CENTER);

        // Action panel
        JPanel actionPanel = new JPanel();
        actionPanel.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.X_AXIS));

        actionPanel.add(Box.createHorizontalGlue());
        for (JIPipeNotificationAction action : notification.getActions()) {
            JButton actionButton = new JButton(action.getLabel(), action.getIcon());
            actionButton.setToolTipText(action.getTooltip());
            actionButton.addActionListener(e -> {
                action.getAction().accept(getWorkbench());
                if (action.isDismiss())
                    dismiss();
            });
            actionPanel.add(actionButton);
        }

        add(actionPanel, BorderLayout.SOUTH);
    }

    private void block() {
        if (JOptionPane.showConfirmDialog(getWorkbench().getWindow(), "Do you really want to block all future notifications of " +
                "the type '" + notification.getHeading() + "'?", "Block notification", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            NotificationUISettings.getInstance().getBlockedNotifications().add(notification.getId());
            NotificationUISettings.getInstance().triggerParameterChange("blocked-action-notifications");
            dismiss();
        }
    }

    private void dismiss() {
        inboxUI.getDismissedNotifications().add(notification);
        notification.dismiss();
    }

    private void unblock() {
        NotificationUISettings.getInstance().getBlockedNotifications().remove(notification.getId());
        NotificationUISettings.getInstance().triggerParameterChange("blocked-action-notifications");
    }
}
