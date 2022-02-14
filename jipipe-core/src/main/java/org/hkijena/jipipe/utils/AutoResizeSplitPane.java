package org.hkijena.jipipe.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * A split pane that automatically resizes itself to a ratio
 */
public class AutoResizeSplitPane extends JSplitPane {

    public static final double RATIO_3_TO_1 = 0.66;
    public static final double RATIO_1_TO_3 = 0.33;

    private final double ratio;

    public AutoResizeSplitPane(int newOrientation, double ratio) {
        super(newOrientation);
        this.ratio = ratio;
        initialize();
    }

    public AutoResizeSplitPane(int newOrientation, Component newLeftComponent, Component newRightComponent, double ratio) {
        super(newOrientation, newLeftComponent, newRightComponent);
        this.ratio = ratio;
        initialize();
    }

    private void initialize() {
        setDividerSize(3);
        setResizeWeight(ratio);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                setDividerLocation(ratio);
            }
        });
    }
}
