package org.hkijena.jipipe.plugins.statistics;

import org.hkijena.jipipe.JIPipe;

import javax.swing.*;

public enum JIPipeStatisticsItemCategory {
    Machine("Machine information"),
    Usage("Usage statistics"),
    Fun("Fun statistics");

    private final String displayName;

    JIPipeStatisticsItemCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getCategory() {
        return displayName;
    }

    public Icon getIcon() {
        return switch (this) {
            case Machine -> JIPipe.RESOURCES.getIcon16("actions/computer.png");
            case Usage -> JIPipe.RESOURCES.getIcon16("actions/chart-bar.png");
            case Fun -> JIPipe.RESOURCES.getIcon16("actions/gamepad.png");
        };
    }
}
