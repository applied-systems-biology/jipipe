package org.hkijena.jipipe.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;

/**
 * A split pane that automatically resizes itself to a ratio
 */
public class AutoResizeSplitPane extends JSplitPane {

    public static final int LEFT_RIGHT = JSplitPane.HORIZONTAL_SPLIT;

    public static final int TOP_BOTTOM = JSplitPane.VERTICAL_SPLIT;

    public static final double RATIO_3_TO_1 = 0.66;
    public static final double RATIO_1_TO_3 = 0.33;
    public static final double RATIO_1_TO_1 = 0.5;

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

        // Remove interfering hotkeys
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), "none");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), "none");
    }
}
