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

import com.google.common.eventbus.EventBus;

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

    public Set<JIPipeNotification> getNotifications() {
        return Collections.unmodifiableSet(notifications);
    }

    public void dismissAll() {
        notifications.clear();
        eventBus.post(new UpdatedEvent(this));
    }

    public void push(JIPipeNotification notification) {
        notifications.add(notification);
        eventBus.post(new UpdatedEvent(this));
    }

    public void dismiss(JIPipeNotification notification) {
        notifications.remove(notification);
        eventBus.post(new UpdatedEvent(this));
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public static JIPipeNotificationInbox getInstance() {
        return INSTANCE;
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
