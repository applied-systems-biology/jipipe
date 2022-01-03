/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.notifications;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.extensions.settings.NotificationUISettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.icons.AnimatedIcon;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * UI that monitors the queue
 */
public class NotificationButton extends JButton {

    private final JIPipeWorkbench workbench;
    private final Timer timer;
    private AnimatedIcon warningIcon;
    private Set<JIPipeNotification> notificationSet = new TreeSet<>();
    private List<String> headings = new ArrayList<>();
    private int currentHeading;

    /**
     * @param workbench the workbench
     */
    public NotificationButton(JIPipeWorkbench workbench) {
        this.workbench = workbench;
        this.timer = new Timer(5000, e -> showNextNotification());
        initialize();
        updateStatus();

        JIPipeNotificationInbox.getInstance().getEventBus().register(this);
        getWorkbench().getNotificationInbox().getEventBus().register(this);
        addActionListener(e -> workbench.getDocumentTabPane().selectSingletonTab(JIPipeProjectWorkbench.TAB_NOTIFICATIONS));
    }

    private void showNextNotification() {
        currentHeading = (currentHeading + 1) % headings.size();
        setText(headings.get(currentHeading));
    }

    private void updateStatus() {
        notificationSet.clear();
        warningIcon.stop();
        timer.stop();

        // Add global and local (JIPipeProjectWorkbench) notifications
        if (NotificationUISettings.getInstance().isEnableNotifications()) {
            notificationSet.addAll(JIPipeNotificationInbox.getInstance().getNotifications());
            notificationSet.addAll(getWorkbench().getNotificationInbox().getNotifications());
        }
        if (!NotificationUISettings.getInstance().getBlockedNotifications().isEmpty()) {
            Set<String> ids = new HashSet<>(NotificationUISettings.getInstance().getBlockedNotifications());
            for (JIPipeNotification notification : ImmutableList.copyOf(notificationSet)) {
                if (ids.contains(notification.getId())) {
                    notificationSet.remove(notification);
                }
            }
        }

        headings.clear();
        headings.add(notificationSet.size() == 1 ? "You have one notification" : "You have " + notificationSet.size() + " notifications");
        for (JIPipeNotification notification : notificationSet) {
            headings.add(notification.getHeading());
        }
        currentHeading = 0;

        if (notificationSet.isEmpty()) {
            setVisible(false);
        } else {
            // Set width
            FontMetrics fontMetrics = getFontMetrics(getFont());
            int width = notificationSet.stream().map(notification -> fontMetrics.stringWidth(notification.getHeading())).max(Comparator.naturalOrder()).orElse(0);
            setPreferredSize(new Dimension(width + 100, 32));

            setVisible(true);
            setText("Your action is required");
            warningIcon.start();
            timer.start();
        }
    }

    private void initialize() {
        UIUtils.makeFlat(this);

        warningIcon = new AnimatedIcon(this, UIUtils.getIconFromResources("emblems/emblem-important.png"),
                UIUtils.getIconFromResources("emblems/warning.png"),
                100, 0.05);
        setIcon(warningIcon);
        setHorizontalAlignment(LEFT);
    }

    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    @Subscribe
    public void onNotificationsChanged(JIPipeNotificationInbox.UpdatedEvent event) {
        updateStatus();
    }
}
