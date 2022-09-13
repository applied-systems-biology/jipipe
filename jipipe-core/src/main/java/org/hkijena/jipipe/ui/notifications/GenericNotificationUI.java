package org.hkijena.jipipe.ui.notifications;

import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class GenericNotificationUI extends JIPipeWorkbenchPanel {

    private final GenericNotificationInboxUI inboxUI;
    private final JIPipeNotification notification;
    private final boolean dismissed;

    private final boolean blocked;

    /**
     * @param inboxUI      the workbench
     * @param notification the notification instance
     * @param dismissed    if dismissed
     * @param blocked if blocked
     */
    public GenericNotificationUI(GenericNotificationInboxUI inboxUI, JIPipeNotification notification, boolean dismissed, boolean blocked) {
        super(inboxUI.getWorkbench());
        this.inboxUI = inboxUI;
        this.notification = notification;
        this.dismissed = dismissed;
        this.blocked = blocked;
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

        if (!dismissed && !blocked) {
            JButton dismissButton = new JButton(UIUtils.getIconFromResources("actions/close-tab.png"));
            UIUtils.makeFlat25x25(dismissButton);
            dismissButton.setToolTipText("Dismisses this notification");
            dismissButton.addActionListener(e -> notification.dismiss());
            headerPanel.add(dismissButton);
        }

        // Add content
        JTextPane textPane = UIUtils.makeBorderlessReadonlyTextPane(new MarkdownDocument(notification.getDescription()).getRenderedHTML(), true);
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
                    notification.dismiss();
            });
            actionPanel.add(actionButton);
        }

        add(actionPanel, BorderLayout.SOUTH);
    }
}