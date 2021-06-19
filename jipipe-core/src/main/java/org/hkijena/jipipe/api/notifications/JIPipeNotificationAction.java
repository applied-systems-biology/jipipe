package org.hkijena.jipipe.api.notifications;

import org.hkijena.jipipe.ui.JIPipeWorkbench;

import javax.swing.*;
import java.util.function.Consumer;

public class JIPipeNotificationAction implements Runnable {

    private String label;
    private String tooltip;
    private Icon icon;
    private Consumer<JIPipeWorkbench> action;
    private boolean dismiss = true;

    public JIPipeNotificationAction() {
    }

    public JIPipeNotificationAction(String label, String tooltip, Icon icon, Consumer<JIPipeWorkbench> action) {
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
}
