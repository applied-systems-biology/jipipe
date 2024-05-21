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

package org.hkijena.jipipe.api.notifications;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.plugins.settings.JIPipeNotificationUIApplicationSettings;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.StampedLock;

/**
 * A list of notifications that has not yet been reviewed
 */
public class JIPipeNotificationInbox {
    private static final JIPipeNotificationInbox INSTANCE = new JIPipeNotificationInbox();
    private final Set<JIPipeNotification> notifications = new TreeSet<>();
    private final DismissedEventEmitter dismissedEventEmitter = new DismissedEventEmitter();
    private final PushedEventEmitter pushedEventEmitter = new PushedEventEmitter();
    private final UpdatedEventEmitter updatedEventEmitter = new UpdatedEventEmitter();
    private final StampedLock stampedLock = new StampedLock();

    public JIPipeNotificationInbox() {

    }

    public JIPipeNotificationInbox(JIPipeNotificationInbox other) {
        for (JIPipeNotification notification : other.notifications) {
            notifications.add(new JIPipeNotification(notification));
        }
    }

    public static JIPipeNotificationInbox getInstance() {
        return INSTANCE;
    }

    public Set<JIPipeNotification> getNotifications() {
        return Collections.unmodifiableSet(notifications);
    }

    public DismissedEventEmitter getDismissedEventEmitter() {
        return dismissedEventEmitter;
    }

    public PushedEventEmitter getPushedEventEmitter() {
        return pushedEventEmitter;
    }

    public UpdatedEventEmitter getUpdatedEventEmitter() {
        return updatedEventEmitter;
    }

    public void dismissAll() {
        ImmutableList<JIPipeNotification> copy = ImmutableList.copyOf(notifications);
        long stamp = stampedLock.writeLock();
        try {
            notifications.clear();
        } finally {
            stampedLock.unlock(stamp);
        }
        for (JIPipeNotification notification : copy) {
            dismissedEventEmitter.emit(new DismissedEvent(this, notification));
        }
        updatedEventEmitter.emit(new UpdatedEvent(this));
    }

    public void push(JIPipeNotification notification) {
        long stamp = stampedLock.writeLock();
        try {
            notification.setInbox(this);
            notifications.add(notification);
        } finally {
            stampedLock.unlock(stamp);
        }
        pushedEventEmitter.emit(new PushedEvent(this, notification));
        updatedEventEmitter.emit(new UpdatedEvent(this));
    }

    public void dismiss(JIPipeNotification notification) {
        long stamp = stampedLock.writeLock();
        try {
            notifications.remove(notification);
        } finally {
            stampedLock.unlock(stamp);
        }
        dismissedEventEmitter.emit(new DismissedEvent(this, notification));
        updatedEventEmitter.emit(new UpdatedEvent(this));
    }

    public void dismiss(String id) {
        ImmutableList.copyOf(notifications).stream().filter(notification -> notification.getId().equals(id)).forEach(this::dismiss);
    }

    public boolean isEmpty() {
        long stamp = stampedLock.readLock();
        try {
            return notifications.isEmpty();
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Connects all dismisses in this notification inbox to another notification inbox
     *
     * @param inbox the target
     */
    public void connectDismissTo(JIPipeNotificationInbox inbox) {
        if (inbox == this)
            return;
        inbox.dismissedEventEmitter.subscribeLambda((emitter, event) -> inbox.dismiss(event.notification.getId()));
    }

    public boolean hasNotifications() {
        Set<String> blocked = new HashSet<>();
        if (JIPipe.isInstantiated()) {
            JIPipeNotificationUIApplicationSettings settings = JIPipeNotificationUIApplicationSettings.getInstance();
            if (settings != null) {
                blocked.addAll(settings.getBlockedNotifications());
            }
        }
        long stamp = stampedLock.readLock();
        try {
            for (JIPipeNotification notification : notifications) {
                if (!blocked.contains(notification.getId())) {
                    return true;
                }
            }
        } finally {
            stampedLock.unlock(stamp);
        }
        return false;
    }

    public interface PushedEventListener {
        void onNotificationPushed(PushedEvent event);
    }

    public interface DismissedEventListener {
        void onNotificationDismissed(DismissedEvent event);
    }

    public interface UpdatedEventListener {
        void onNotificationInboxUpdated(UpdatedEvent event);
    }

    public static class PushedEvent extends AbstractJIPipeEvent {
        private final JIPipeNotificationInbox inbox;
        private final JIPipeNotification notification;

        public PushedEvent(JIPipeNotificationInbox inbox, JIPipeNotification notification) {
            super(inbox);
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

    public static class PushedEventEmitter extends JIPipeEventEmitter<PushedEvent, PushedEventListener> {

        @Override
        protected void call(PushedEventListener pushedEventListener, PushedEvent event) {
            pushedEventListener.onNotificationPushed(event);
        }
    }

    public static class DismissedEvent extends AbstractJIPipeEvent {
        private final JIPipeNotificationInbox inbox;
        private final JIPipeNotification notification;

        public DismissedEvent(JIPipeNotificationInbox inbox, JIPipeNotification notification) {
            super(inbox);
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

    public static class DismissedEventEmitter extends JIPipeEventEmitter<DismissedEvent, DismissedEventListener> {

        @Override
        protected void call(DismissedEventListener dismissedEventListener, DismissedEvent event) {
            dismissedEventListener.onNotificationDismissed(event);
        }
    }

    /**
     * Event triggered when an inbox is updated
     */
    public static class UpdatedEvent extends AbstractJIPipeEvent {
        private final JIPipeNotificationInbox inbox;

        public UpdatedEvent(JIPipeNotificationInbox inbox) {
            super(inbox);
            this.inbox = inbox;
        }

        public JIPipeNotificationInbox getInbox() {
            return inbox;
        }
    }

    public static class UpdatedEventEmitter extends JIPipeEventEmitter<UpdatedEvent, UpdatedEventListener> {

        @Override
        protected void call(UpdatedEventListener updatedEventListener, UpdatedEvent event) {
            updatedEventListener.onNotificationInboxUpdated(event);
        }
    }
}
