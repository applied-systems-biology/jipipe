package org.hkijena.jipipe.api.notifications;

import javax.swing.*;

public class JIPipeNotificationAction implements Runnable {

    private String label;
    private String tooltip;
    private Icon icon;
    private Runnable action;

    public JIPipeNotificationAction() {
    }

    public JIPipeNotificationAction(String label, String tooltip, Icon icon, Runnable action) {
        this.label = label;
        this.tooltip = tooltip;
        this.icon = icon;
        this.action = action;
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

    public Runnable getAction() {
        return action;
    }

    public void setAction(Runnable action) {
        this.action = action;
    }

    @Override
    public void run() {

    }
}
