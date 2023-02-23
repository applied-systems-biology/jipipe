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

package org.hkijena.jipipe.api.notifications;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * A list of notifications that has not yet been reviewed
 */
public class JIPipeNotificationInbox {
    private static final JIPipeNotificationInbox INSTANCE = new JIPipeNotificationInbox();

    private final EventBus eventBus = new EventBus();
    private final Set<JIPipeNotification> notifications = new TreeSet<>();

    public JIPipeNotificationInbox() {

    }

    public static JIPipeNotificationInbox getInstance() {
        return INSTANCE;
    }

    public Set<JIPipeNotification> getNotifications() {
        return Collections.unmodifiableSet(notifications);
    }

    public void dismissAll() {
        ImmutableList<JIPipeNotification> copy = ImmutableList.copyOf(notifications);
        notifications.clear();
        for (JIPipeNotification notification : copy) {
            eventBus.post(new DismissedEvent(this, notification));
        }
        eventBus.post(new UpdatedEvent(this));
    }

    public void push(JIPipeNotification notification) {
        notification.setInbox(this);
        notifications.add(notification);
        eventBus.post(new PushedEvent(this, notification));
        eventBus.post(new UpdatedEvent(this));
    }

    public void dismiss(JIPipeNotification notification) {
        notifications.remove(notification);
        eventBus.post(new DismissedEvent(this, notification));
        eventBus.post(new UpdatedEvent(this));
    }

    public void dismiss(String id) {
        ImmutableList.copyOf(notifications).stream().filter(notification -> notification.getId().equals(id)).forEach(this::dismiss);
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public boolean isEmpty() {
        return notifications.isEmpty();
    }

    /**
     * Connects all dismisses in this notification inbox to another notification inbox
     *
     * @param inbox the target
     */
    public void connectDismissTo(JIPipeNotificationInbox inbox) {
        if (inbox == this)
            return;
        eventBus.register(new Object() {
            @Subscribe
            public void onDismissed(DismissedEvent event) {
                inbox.dismiss(event.notification.getId());
            }
        });
    }

    public static class PushedEvent {
        private final JIPipeNotificationInbox inbox;
        private final JIPipeNotification notification;

        public PushedEvent(JIPipeNotificationInbox inbox, JIPipeNotification notification) {
            this.inbox = inbox;
            this.notification = notification;
        }

        public JIPipeNotificationInbox getInbox() {
            return inbox;
        }

        public JIPipeNotification getNotification() {
            return notification;
        }
    }

    public static class DismissedEvent {
        private final JIPipeNotificationInbox inbox;
        private final JIPipeNotification notification;

        public DismissedEvent(JIPipeNotificationInbox inbox, JIPipeNotification notification) {
            this.inbox = inbox;
            this.notification = notification;
        }

        public JIPipeNotificationInbox getInbox() {
            return inbox;
        }

        public JIPipeNotification getNotification() {
            return notification;
        }
    }

    /**
     * Event triggered when an inbox is updated
     */
    public static class UpdatedEvent {
        private final JIPipeNotificationInbox inbox;

        public UpdatedEvent(JIPipeNotificationInbox inbox) {
            this.inbox = inbox;
        }

        public JIPipeNotificationInbox getInbox() {
            return inbox;
        }
    }
}
