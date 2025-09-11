package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.overlays;

import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasResources;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.managers.JIPipeDesktopGraphCanvasNotificationsManager;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Overlay that draws notifications on the graph canvas
 */
public class JIPipeDesktopGraphCanvasNotificationsOverlay implements JIPipeDesktopGraphCanvasOverlay {

    // UI constants
    public static final int NOTIFICATION_HEIGHT = 42;
    public static final int NOTIFICATION_SPACING = 8;
    public static final int NOTIFICATION_ROUNDING = 8;
    public static final int NOTIFICATION_PADDING = 12;
    public static final int NOTIFICATION_ICON_TEXT_SPACING = 8;
    private final JIPipeDesktopGraphCanvasUI canvasUI;

    public JIPipeDesktopGraphCanvasNotificationsOverlay(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
    }

    @Override
    public void paint(Graphics2D g) {
        List<JIPipeDesktopGraphCanvasNotificationsManager.CanvasNotification> notifications =
                canvasUI.getNotificationsManager().getDisplayNotifications();
        Rectangle visibleRect = canvasUI.getVisibleRect();


        if (notifications.isEmpty() || visibleRect == null) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create();

        try {
            // Enable antialiasing for smooth rendering
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Calculate notification stack position (bottom-right corner)
            int startX = visibleRect.x + visibleRect.width - 16;
            int startY = visibleRect.y + visibleRect.height - 16;

            // Draw notifications from bottom to top
            for (int i = notifications.size() - 1; i >= 0; i--) {
                JIPipeDesktopGraphCanvasNotificationsManager.CanvasNotification notification = notifications.get(i);
                drawNotification(g2d, notification, startX, startY);

                // Move position up for next notification
                startY -= NOTIFICATION_HEIGHT + NOTIFICATION_SPACING;
            }

        } finally {
            g2d.dispose();
        }
    }

    @Override
    public void paintComponent(Graphics2D g) {
        // No component-level painting needed for notifications
    }

    /**
     * Draws a single notification with rounded rectangle background
     */
    private void drawNotification(Graphics2D g,
                                  JIPipeDesktopGraphCanvasNotificationsManager.CanvasNotification notification,
                                  int x, int y) {
        g.setStroke(JIPipeDesktopGraphCanvasResources.STROKE_UNIT);

        int width = notification.getPreferredWidth();
        int height = NOTIFICATION_HEIGHT;

        // Calculate actual position
        int actualX = x - width - NOTIFICATION_PADDING;
        int actualY = y - height - NOTIFICATION_PADDING;

        // Get notification type colors
        Color baseColor = getNotificationBackgroundColor(notification.getType());

        // Create background shape
        RoundRectangle2D background = new RoundRectangle2D.Float(
                actualX, actualY, width, height,
                NOTIFICATION_ROUNDING, NOTIFICATION_ROUNDING);

        // Draw background with gradient effect
        g.setPaint(ColorUtils.interpolate(baseColor, ThemeUtils.getCurrentStyle().getWindowBackground(), 0.5));
        g.fill(background);

        // Draw border
        g.setColor(baseColor);
        g.draw(background);

        // Draw content
        drawNotificationContent(g, notification, actualX, actualY, width, height);
    }

    /**
     * Draws the content (icon and text) of a notification
     */
    private void drawNotificationContent(Graphics2D g,
                                         JIPipeDesktopGraphCanvasNotificationsManager.CanvasNotification notification,
                                         int x, int y, int width, int height) {
        // Set font
        Font font = new Font(Font.DIALOG, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeSmall());
        g.setFont(font);

        // Font metrics for text positioning
        FontMetrics fontMetrics = g.getFontMetrics();

        // Icon position
        int iconX = x + NOTIFICATION_PADDING;
        int iconY = y + (height - 16) / 2; // Center vertically (assuming 16px icon)

        // Draw icon
        Icon icon = notification.getIcon();
        if (icon != null) {
            icon.paintIcon(canvasUI, g, iconX, iconY);
        }

        // Text position
        int textX = icon != null ? iconX + icon.getIconWidth() + NOTIFICATION_ICON_TEXT_SPACING : x + NOTIFICATION_PADDING;
        int textY = y + (height - fontMetrics.getHeight()) / 2 + fontMetrics.getAscent();

        // Draw text
        g.setColor(ThemeUtils.getCurrentStyle().getTextForeground());
        g.drawString(notification.getDisplayText(), textX, textY);
    }

    /**
     * Creates a paint for notification background with 50% interpolated effect
     */
    private Paint createBackgroundPaint(Color baseColor, int x, int y, int width, int height) {
        // Create a lighter version of the base color for the 50% interpolation
        Color lighterColor = new Color(
                (baseColor.getRed() + 255) / 2,
                (baseColor.getGreen() + 255) / 2,
                (baseColor.getBlue() + 255) / 2,
                (int) (baseColor.getAlpha() * 0.5) // 50% opacity
        );

        return new GradientPaint(x, y, baseColor, x, y + height, lighterColor);
    }

    /**
     * Gets background color for notification type
     */
    private Color getNotificationBackgroundColor(JIPipeDesktopGraphCanvasNotificationsManager.NotificationType type) {
        return switch (type) {
            case Info -> ThemeUtils.getCurrentStyle().getPrimaryColor();
            case InfoMuted -> ThemeUtils.getCurrentStyle().getSecondaryColor();
            case Success -> ThemeUtils.getCurrentStyle().getSuccessColor();
            case Error -> ThemeUtils.getCurrentStyle().getDangerColor();
            case Warning -> ThemeUtils.getCurrentStyle().getWarningColor();
            default -> ThemeUtils.getCurrentStyle().getPrimaryColor();
        };
    }
}
