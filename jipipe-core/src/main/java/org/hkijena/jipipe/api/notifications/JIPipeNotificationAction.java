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

import org.hkijena.jipipe.api.JIPipeWorkbench;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class JIPipeNotificationAction implements Runnable {

    private String label;
    private String tooltip;
    private Icon icon;
    private Consumer<JIPipeWorkbench> action;
    private boolean dismiss = true;

    private Style style = Style.Normal;

    public JIPipeNotificationAction() {
    }

    public JIPipeNotificationAction(String label, String tooltip, Icon icon, Consumer<JIPipeWorkbench> action) {
        this.label = label;
        this.tooltip = tooltip;
        this.icon = icon;
        this.action = action;
    }

    public JIPipeNotificationAction(String label, String tooltip, Icon icon, Style style, Consumer<JIPipeWorkbench> action) {
        this.label = label;
        this.tooltip = tooltip;
        this.icon = icon;
        this.action = action;
        this.style = style;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getTooltip() {
        return tooltip;
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    public Consumer<JIPipeWorkbench> getAction() {
        return action;
    }

    public void setAction(Consumer<JIPipeWorkbench> action) {
        this.action = action;
    }

    @Override
    public void run() {

    }

    public boolean isDismiss() {
        return dismiss;
    }

    public void setDismiss(boolean dismiss) {
        this.dismiss = dismiss;
    }

    public Style getStyle() {
        return style;
    }

    public void setStyle(Style style) {
        this.style = style;
    }

    public enum Style {
        Normal(null, null),
        Success(new Color(0x5CB85C), Color.WHITE);

        private final Color background;
        private final Color text;

        Style(Color background, Color text) {
            this.background = background;
            this.text = text;
        }

        public Color getBackground() {
            return background;
        }

        public Color getText() {
            return text;
        }
    }
}
