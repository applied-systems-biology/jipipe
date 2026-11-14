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

package org.hkijena.jipipe.desktop.app.shortcuts;

import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutRegistry;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutSettings;

import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;

/**
 * Builds context menu items that display the effective key binding of a
 * shortcut action (issue #1346, in-app verification wave 1).
 * <p>
 * The binding is rendered as right-aligned text inside the item, uniformly
 * for single-stroke AND chord bindings (a chord cannot be expressed as a
 * Swing accelerator, and mixing accelerators for plain bindings with a
 * second display style for chords would render the two inconsistently).
 * Unassigned actions render no binding text.
 * <p>
 * The binding text is painted by the item itself instead of being added as
 * a child component: a child JLabel would swallow the mouse events of the
 * menu item and break the clicks. The text is measured with the item's own
 * font metrics (the gap to the right edge is a pure gap); the preferred
 * width includes the measured text so a long binding widens the item
 * instead of being clipped.
 */
public final class JIPipeDesktopShortcutMenuItems {

    /**
     * The gap between the binding text and the item's right edge. A pure
     * gap, not a text-derived size.
     */
    private static final int BINDING_RIGHT_GAP = 8;

    private JIPipeDesktopShortcutMenuItems() {
    }

    /**
     * Creates a menu item whose right edge shows the effective binding of
     * the given shortcut action (empty when the action has no id or is
     * unassigned).
     *
     * @param name             the item text
     * @param icon             the item icon, may be {@code null}
     * @param description      the item tooltip
     * @param shortcutActionId the shortcut action id, may be {@code null}
     * @return the menu item
     */
    public static JMenuItem createItem(String name, Icon icon, String description, String shortcutActionId) {
        String bindingText = "";
        if (shortcutActionId != null) {
            bindingText = JIPipeShortcutRegistry.formatVariants(JIPipeShortcutRegistry.getInstance()
                    .bindingFor(shortcutActionId, JIPipeShortcutSettings.current()).variants());
        }
        JMenuItem item = new BindingMenuItem(name, icon, bindingText);
        item.setToolTipText(description);
        return item;
    }

    /**
     * The binding text color: the LaF's menu accelerator foreground when it
     * provides one (the conventional slot for right-aligned key text), the
     * disabled label color otherwise; disabled items always use a disabled
     * color.
     */
    private static Color bindingColor(JMenuItem item) {
        if (!item.isEnabled()) {
            Color disabled = UIManager.getColor("MenuItem.disabledForeground");
            if (disabled != null) {
                return disabled;
            }
        } else {
            Color accelerator = UIManager.getColor("MenuItem.acceleratorForeground");
            if (accelerator != null) {
                return accelerator;
            }
        }
        Color fallback = UIManager.getColor("Label.disabledForeground");
        return fallback != null ? fallback : item.getForeground();
    }

    /**
     * The menu item that paints the binding text right-aligned after the
     * regular item rendering.
     */
    private static class BindingMenuItem extends JMenuItem {
        private final String bindingText;

        private BindingMenuItem(String text, Icon icon, String bindingText) {
            super(text, icon);
            this.bindingText = bindingText == null ? "" : bindingText;
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            if (!bindingText.isEmpty()) {
                FontMetrics metrics = getFontMetrics(getFont());
                size.width += metrics.stringWidth(bindingText) + BINDING_RIGHT_GAP;
            }
            return size;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (bindingText.isEmpty()) {
                return;
            }
            FontMetrics metrics = g.getFontMetrics();
            int x = getWidth() - metrics.stringWidth(bindingText) - BINDING_RIGHT_GAP;
            int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            g.setColor(bindingColor(this));
            g.drawString(bindingText, x, y);
        }
    }
}
