package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.managers;

import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.overlays.JIPipeDesktopGraphCanvasNotificationsOverlay;
import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * Manages notifications displayed on the graph canvas
 */
public class JIPipeDesktopGraphCanvasNotificationsManager {
    // Configuration constants
    private static final long MIN_DURATION_MS = 3000; // 3 seconds minimum
    private static final long PER_CHAR_DURATION_MS = 100; // 100ms per character
    private final JIPipeDesktopGraphCanvasUI canvasUI;
    // Data structures for storing active notifications with deduplication
    private final Map<String, CanvasNotification> activeNotifications = new ConcurrentHashMap<>();
    private final List<CanvasNotification> displayNotifications = new CopyOnWriteArrayList<>();
    // Timer for expiration system
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public JIPipeDesktopGraphCanvasNotificationsManager(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;

        // Start expiration checker
        scheduler.scheduleAtFixedRate(this::checkExpiredNotifications, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Adds a new notification or updates an existing one with the same text
     *
     * @param text The notification text (used as unique identifier)
     * @param icon The notification icon (can be null)
     * @param type The notification type
     */
    public void addNotification(String text, Icon icon, NotificationType type) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        long duration = calculateDuration(text);
        long expirationTime = System.currentTimeMillis() + duration;

        CanvasNotification canvasNotification = activeNotifications.get(text);
        if (canvasNotification != null) {
            // Update existing notification - increment duplicate count and reset expiration
            canvasNotification.setDuplicateCount(canvasNotification.getDuplicateCount() + 1);
            canvasNotification.setExpirationTime(expirationTime);
            canvasNotification.setIcon(icon);
            canvasNotification.setType(type);
        } else {
            // Create new notification
            canvasNotification = new CanvasNotification(text, icon, type, expirationTime);
            activeNotifications.put(text, canvasNotification);
        }

        updateDisplayNotifications();
        canvasUI.repaintLowLag();
    }

    /**
     * Calculates display duration based on message length
     */
    private long calculateDuration(String message) {
        if (message == null || message.isEmpty()) {
            return MIN_DURATION_MS;
        }
        return Math.min(MIN_DURATION_MS * 3, Math.max(MIN_DURATION_MS, (message.length() * PER_CHAR_DURATION_MS)));
    }

    /**
     * Updates the list of notifications to be displayed
     */
    private void updateDisplayNotifications() {
        // Show at most 5 newest notifications
        displayNotifications.clear();
        displayNotifications.addAll(activeNotifications.values().stream()
                .sorted(Comparator.comparing(CanvasNotification::getCreationTime).reversed())
                .limit(5)
                .toList());

        // Calculate maximum width for consistent display
        int maxTextWidth = calculateMaxTextWidth();
        for (CanvasNotification notification : displayNotifications) {
            notification.setPreferredWidth(maxTextWidth);
        }
    }

    /**
     * Calculates the maximum text width among all notifications
     */
    private int calculateMaxTextWidth() {
        FontMetrics fontMetrics = canvasUI.getFontMetrics(new Font(Font.DIALOG, Font.PLAIN,
                ThemeUtils.getCurrentStyle().getFontSizeSmall()));
        int maxWidth = 0;

        for (CanvasNotification notification : displayNotifications) {
            int width = fontMetrics.stringWidth(notification.getDisplayText());

            // Add padding
            width += JIPipeDesktopGraphCanvasNotificationsOverlay.NOTIFICATION_PADDING * 2;

            // Icon-based padding
            if (notification.getIcon() != null) {
                width += JIPipeDesktopGraphCanvasNotificationsOverlay.NOTIFICATION_ICON_TEXT_SPACING;
                width += notification.getIcon().getIconWidth();
            }

            if (width > maxWidth) {
                maxWidth = width;
            }
        }

        return maxWidth;
    }

    /**
     * Checks and removes expired notifications
     */
    private void checkExpiredNotifications() {
        long currentTime = System.currentTimeMillis();
        List<String> expiredIds = new ArrayList<>();

        for (Map.Entry<String, CanvasNotification> entry : activeNotifications.entrySet()) {
            if (entry.getValue().getExpirationTime() <= currentTime) {
                expiredIds.add(entry.getKey());
            }
        }

        if (!expiredIds.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                for (String id : expiredIds) {
                    activeNotifications.remove(id);
                }
                updateDisplayNotifications();
                canvasUI.repaintLowLag();
            });
        }
    }

    /**
     * Gets the list of notifications to be displayed
     */
    public List<CanvasNotification> getDisplayNotifications() {
        return Collections.unmodifiableList(displayNotifications);
    }

    /**
     * Dismisses a specific notification by text
     */
    public void dismissNotification(String notificationText) {
        CanvasNotification notification = activeNotifications.remove(notificationText);
        if (notification != null) {
            updateDisplayNotifications();
            canvasUI.repaintLowLag();
        }
    }

    /**
     * Dismisses all notifications
     */
    public void dismissAllNotifications() {
        activeNotifications.clear();
        updateDisplayNotifications();
        canvasUI.repaintLowLag();
    }

    /**
     * Gets the number of active notifications
     */
    public int getActiveNotificationCount() {
        return activeNotifications.size();
    }

    /**
     * Gets the number of notifications being displayed
     */
    public int getDisplayNotificationCount() {
        return displayNotifications.size();
    }

    /**
     * Enumeration of notification types
     */
    public enum NotificationType {
        Info,
        InfoMuted,
        Success,
        Error,
        Warning
    }

    /**
     * Internal notification data structure
     */
    public static class CanvasNotification {
        private final long creationTime;
        private String message;
        private Icon icon;
        private NotificationType type;
        private long expirationTime;
        private int duplicateCount;
        private int preferredWidth;

        public CanvasNotification(String message, Icon icon, NotificationType type, long expirationTime) {
            this.message = message;
            this.icon = icon;
            this.type = type;
            this.creationTime = System.currentTimeMillis();
            this.expirationTime = expirationTime;
            this.duplicateCount = 0;
            this.preferredWidth = 300;
        }

        public String getDisplayText() {
            if (duplicateCount > 0) {
                return message + " (" + (duplicateCount + 1) + ")";
            }
            return message;
        }

        public Icon getIcon() {
            return icon;
        }

        public void setIcon(Icon icon) {
            this.icon = icon;
        }

        public NotificationType getType() {
            return type;
        }

        public void setType(NotificationType type) {
            this.type = type;
        }

        // Getters and setters
        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public long getCreationTime() {
            return creationTime;
        }

        public long getExpirationTime() {
            return expirationTime;
        }

        public void setExpirationTime(long expirationTime) {
            this.expirationTime = expirationTime;
        }

        public int getDuplicateCount() {
            return duplicateCount;
        }

        public void setDuplicateCount(int duplicateCount) {
            this.duplicateCount = duplicateCount;
        }

        public int getPreferredWidth() {
            return preferredWidth;
        }

        public void setPreferredWidth(int preferredWidth) {
            this.preferredWidth = preferredWidth;
        }
    }
}
