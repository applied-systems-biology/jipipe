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

package org.hkijena.jipipe.desktop.commons.components.layouts;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class JIPipeDesktopColumnLayout implements LayoutManager2 {

    private static final int COLUMNS = 12;

    private final int gutter;
    private final Map<Component, JIPipeDesktopColumnConstraints> constraintsMap = new LinkedHashMap<>();

    public JIPipeDesktopColumnLayout(int gutter) {
        this.gutter = gutter;
    }

    public int getGutter() {
        return gutter;
    }

    @Override
    public void addLayoutComponent(Component comp, Object constraints) {
        if (constraints instanceof JIPipeDesktopColumnConstraints c) {
            constraintsMap.put(comp, c);
        } else if (constraints == null) {
            constraintsMap.put(comp, new JIPipeDesktopColumnConstraints(COLUMNS));
        } else {
            throw new IllegalArgumentException("Invalid constraints: " + constraints);
        }
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        return layoutSize(target, false);
    }

    @Override
    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int width = target.getWidth();
            if (width == 0) {
                Container parent = target.getParent();
                while (parent != null && parent.getWidth() == 0) {
                    parent = parent.getParent();
                }
                if (parent != null) {
                    width = parent.getWidth();
                }
            }
            if (width == 0) {
                width = 1024;
            }

            Insets insets = target.getInsets();
            int innerWidth = width - insets.left - insets.right;
            JIPipeDesktopBreakpoint bp = JIPipeDesktopBreakpoint.fromWidth(innerWidth);
            int columnUnit = Math.max(0, (innerWidth - (COLUMNS - 1) * gutter) / COLUMNS);

            int totalHeight = 0;
            int currentCol = 0;
            int rowHeight = 0;

            for (Map.Entry<Component, JIPipeDesktopColumnConstraints> entry : constraintsMap.entrySet()) {
                Component comp = entry.getKey();
                if (!comp.isVisible()) continue;
                JIPipeDesktopColumnConstraints cons = entry.getValue();
                int span = Math.min(COLUMNS, cons.getEffectiveSpan(bp));
                int offset = cons.getEffectiveOffset(bp);

                if (currentCol + offset + span > COLUMNS) {
                    totalHeight += rowHeight + gutter;
                    currentCol = 0;
                    rowHeight = 0;
                }

                currentCol += offset;

                int compWidth = span * columnUnit + (span - 1) * gutter;
                Dimension d = preferred ? comp.getPreferredSize() : comp.getMinimumSize();
                if (d.width > 0 && d.height > 0) {
                    int compHeight = d.height;
                    if (compWidth > 0) {
                        compHeight = Math.max(compHeight, comp.getMinimumSize().height);
                    }
                    rowHeight = Math.max(rowHeight, compHeight);
                }

                currentCol += span;
            }
            totalHeight += rowHeight;

            return new Dimension(width, totalHeight + insets.top + insets.bottom);
        }
    }

    @Override
    public void layoutContainer(Container target) {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();
            int innerWidth = target.getWidth() - insets.left - insets.right;
            JIPipeDesktopBreakpoint bp = JIPipeDesktopBreakpoint.fromWidth(innerWidth);
            int columnUnit = Math.max(0, (innerWidth - (COLUMNS - 1) * gutter) / COLUMNS);

            int startX = insets.left;
            int startY = insets.top;
            int currentCol = 0;
            int rowHeight = 0;

            for (Component comp : target.getComponents()) {
                if (!comp.isVisible()) {
                    continue;
                }
                JIPipeDesktopColumnConstraints cons = constraintsMap.getOrDefault(comp,
                        new JIPipeDesktopColumnConstraints(COLUMNS));
                int span = Math.min(COLUMNS, cons.getEffectiveSpan(bp));
                int offset = cons.getEffectiveOffset(bp);

                if (currentCol + offset + span > COLUMNS) {
                    startY += rowHeight + gutter;
                    currentCol = 0;
                    rowHeight = 0;
                }

                currentCol += offset;

                int compWidth = span * columnUnit + (span - 1) * gutter;
                int compX = startX + currentCol * (columnUnit + gutter);
                Dimension pref = comp.getPreferredSize();
                int compHeight = pref.height;
                rowHeight = Math.max(rowHeight, compHeight);

                comp.setBounds(compX, startY, compWidth, compHeight);

                currentCol += span;
                if (currentCol >= COLUMNS) {
                    startY += rowHeight + gutter;
                    currentCol = 0;
                    rowHeight = 0;
                }
            }
        }
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
        constraintsMap.put(comp, new JIPipeDesktopColumnConstraints(COLUMNS));
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        constraintsMap.remove(comp);
    }

    @Override
    public float getLayoutAlignmentX(Container target) {
        return 0.5f;
    }

    @Override
    public float getLayoutAlignmentY(Container target) {
        return 0f;
    }

    @Override
    public void invalidateLayout(Container target) {
    }
}
