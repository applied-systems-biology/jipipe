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

package org.hkijena.jipipe.desktop.commons.theme;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.event.MouseEvent;

public class JIPipeDesktopModernSliderUI extends BasicSliderUI {
    public JIPipeDesktopModernSliderUI(JSlider b) {
        super(b);
    }

    public static ComponentUI createUI(JComponent c) {
        return new JIPipeDesktopModernSliderUI((JSlider) c);
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
