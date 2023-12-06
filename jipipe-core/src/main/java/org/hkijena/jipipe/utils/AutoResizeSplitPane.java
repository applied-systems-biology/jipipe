package org.hkijena.jipipe.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.function.Function;

/**
 * A split pane that automatically resizes itself to a ratio
 */
public class AutoResizeSplitPane extends JSplitPane {

    public static final int LEFT_RIGHT = JSplitPane.HORIZONTAL_SPLIT;

    public static final int TOP_BOTTOM = JSplitPane.VERTICAL_SPLIT;

    public static final double RATIO_3_TO_1 = 0.66;
    public static final double RATIO_1_TO_3 = 0.33;
    public static final double RATIO_1_TO_1 = 0.5;

    private Ratio ratio = new FixedRatio();

    public AutoResizeSplitPane(int newOrientation, double ratio) {
        super(newOrientation);
        setRatio(new FixedRatio(ratio));
        initialize();
    }

    public AutoResizeSplitPane(int newOrientation, Component newLeftComponent, Component newRightComponent, double ratio) {
        super(newOrientation, newLeftComponent, newRightComponent);
        setRatio(new FixedRatio(ratio));
        initialize();
    }

    public AutoResizeSplitPane(int newOrientation, Ratio ratio) {
        super(newOrientation);
        setRatio(ratio);
        initialize();
    }

    public AutoResizeSplitPane(int newOrientation, Component newLeftComponent, Component newRightComponent, Ratio ratio) {
        super(newOrientation, newLeftComponent, newRightComponent);
        setRatio(ratio);
        initialize();
    }

    private void initialize() {
        setDividerSize(3);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                applyRatio();
            }
        });

        // Remove interfering hotkeys
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), "none");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), "none");
    }

    public Ratio getRatio() {
        return ratio;
    }

    public void setRatio(Ratio ratio) {
        this.ratio = ratio;
        setResizeWeight(ratio.apply(getWidth()));
        applyRatio();
    }

    public void applyRatio() {
        setDividerLocation(ratio.apply(getWidth()));
    }

    /**
     * Interface that takes the component width as input and returns the ratio
     */
    public interface Ratio extends Function<Integer, Double> {

    }

    /**
     * A fixed ratio
     */
    public static class FixedRatio implements Ratio {
        private double ratio = 0.5;

        public FixedRatio(double ratio) {
            this.ratio = ratio;
        }

        public FixedRatio() {
        }

        @Override
        public Double apply(Integer integer) {
            return ratio;
        }

        public double getRatio() {
            return ratio;
        }

        public void setRatio(double ratio) {
            this.ratio = ratio;
        }
    }

    /**
     * A dynamic ratio designed for UIs with a sidebar (defaults to the right) that adapts to the available space
     * Targets a preferred sidebar width
     */
    public static class DynamicSidebarRatio implements Ratio {

        private boolean invert = false;
        private int preferredSidebarWidth = 700;

        public DynamicSidebarRatio() {
        }

        public DynamicSidebarRatio(int preferredSidebarWidth, boolean invert) {
            this.preferredSidebarWidth = preferredSidebarWidth;
            this.invert = invert;
        }

        @Override
        public Double apply(Integer availableWidth) {
            if(availableWidth <= preferredSidebarWidth * 2) {
                // Very small width -> 1:1 ratio
                return RATIO_1_TO_1;
            }
            else {
                double ratio = 1.0 * (availableWidth - preferredSidebarWidth) / availableWidth;
                return invert ? 1.0 - ratio : ratio;
            }
        }

        public boolean isInvert() {
            return invert;
        }

        public void setInvert(boolean invert) {
            this.invert = invert;
        }
    }

}
