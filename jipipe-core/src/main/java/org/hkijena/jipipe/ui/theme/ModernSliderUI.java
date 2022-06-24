package org.hkijena.jipipe.ui.theme;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.event.MouseEvent;

public class ModernSliderUI extends BasicSliderUI {
    public ModernSliderUI(JSlider b) {
        super(b);
    }

    public static ComponentUI createUI(JComponent c) {
        return new ModernSliderUI((JSlider) c);
    }

    @Override
    public void paintTrack(Graphics g) {
        if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
            Rectangle trackBounds = trackRect;

            g.setColor(Color.LIGHT_GRAY);
            // Draw the ticks
            if (slider.getMinorTickSpacing() > 0) {
                int value = slider.getMinimum();
                int yBase = trackBounds.y + trackBounds.height / 2;
                int h = trackBounds.height / 4;

                while (value <= slider.getMaximum()) {
                    int xPos = xPositionForValue(value);
                    g.drawLine(xPos, yBase - h, xPos, yBase + h);

                    // Overflow checking
                    if (Integer.MAX_VALUE - slider.getMinorTickSpacing() < value) {
                        break;
                    }

                    value += slider.getMinorTickSpacing();
                }
            }

            g.setColor(Color.GRAY);

            // Draw the median line
            {
                int x = trackBounds.x;
                int y = trackBounds.y + trackBounds.height / 2;
                g.fillRect(x, y, trackBounds.width, 1);
            }
        } else {
            super.paintTrack(g);
        }
    }

    @Override
    protected TrackListener createTrackListener(JSlider slider) {
        return new TrackListener() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (UIManager.getBoolean("Slider.onlyLeftMouseButtonDrag")
                        && SwingUtilities.isLeftMouseButton(e)) {
                    JSlider slider = (JSlider) e.getComponent();
                    switch (slider.getOrientation()) {
                        case SwingConstants.VERTICAL:
                            slider.setValue(valueForYPosition(e.getY()));
                            break;
                        case SwingConstants.HORIZONTAL:
                            slider.setValue(valueForXPosition(e.getX()));
                            break;
                        default:
                            throw new IllegalArgumentException("orientation must be one of: VERTICAL, HORIZONTAL");
                    }
                    super.mousePressed(e); //isDragging = true;
                    super.mouseDragged(e);
                } else {
                    super.mousePressed(e);
                }
            }

            @Override
            public boolean shouldScroll(int direction) {
                return false;
            }
        };
    }

    @Override
    public void paintThumb(Graphics g) {
//        super.paintThumb(g);
        Rectangle knobBounds = thumbRect;
        int w = knobBounds.width;
        int h = knobBounds.height;

        g.setColor(UIManager.getColor("Button.background"));
//        g.setColor(Color.RED);
        g.fillRoundRect(knobBounds.x, knobBounds.y, knobBounds.width - 1, knobBounds.height - 1, 3, 3);
        g.setColor(UIManager.getColor("Button.borderColor"));
        g.drawRoundRect(knobBounds.x, knobBounds.y, knobBounds.width - 1, knobBounds.height - 1, 3, 3);
//        g.setColor(slider.getBackground());
//
    }
}
