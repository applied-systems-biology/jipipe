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

/**
 * A modified version of FlowLayout that allows containers using this
 * Layout to behave in a reasonable manner when placed inside a
 * JScrollPane
 *
 * @author Babu Kalakrishnan
 * Modifications by greearb and jzd
 */
public class JIPipeDesktopFlowLayout extends FlowLayout {

    public JIPipeDesktopFlowLayout() {
        super();
    }

    public JIPipeDesktopFlowLayout(int align) {
        super(align);
    }

    public JIPipeDesktopFlowLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    public Dimension minimumLayoutSize(Container target) {
        // Size of largest component, so we can resize it in
        // either direction with something like a split-pane.
        return computeMinSize(target);
    }

    public Dimension preferredLayoutSize(Container target) {
        return computeSize(target);
    }

    private Dimension computeSize(Container target) {
        synchronized (target.getTreeLock()) {
            int hgap = getHgap();
            int vgap = getVgap();
            int w = target.getWidth();

            // Let this behave like a regular FlowLayout (single row)
            // if the container hasn't been assigned any size yet
            if (w == 0) {
                w = Integer.MAX_VALUE;
            }

            Insets insets = target.getInsets();
            if (insets == null) {
                insets = new Insets(0, 0, 0, 0);
            }
            int reqdWidth = 0;

            int maxwidth = w - (insets.left + insets.right + hgap * 2);
            int n = target.getComponentCount();
            int x = 0;
            int y = insets.top + vgap; // FlowLayout starts by adding vgap, so do that here too.
            int rowHeight = 0;

            for (int i = 0; i < n; i++) {
                Component c = target.getComponent(i);
                if (c.isVisible()) {
                    Dimension d = c.getPreferredSize();
                    if ((x == 0) || ((x + d.width) <= maxwidth)) {
                        // fits in current row.
                        if (x > 0) {
                            x += hgap;
                        }
                        x += d.width;
                        rowHeight = Math.max(rowHeight, d.height);
                    } else {
                        // Start of new row
                        x = d.width;
                        y += vgap + rowHeight;
                        rowHeight = d.height;
                    }
                    reqdWidth = Math.max(reqdWidth, x);
                }
            }
            y += rowHeight;
            y += insets.bottom;
            return new Dimension(reqdWidth + insets.left + insets.right, y);
        }
    }

    private Dimension computeMinSize(Container target) {
        synchronized (target.getTreeLock()) {
            int minx = Integer.MAX_VALUE;
            int miny = Integer.MIN_VALUE;
            boolean found_one = false;
            int n = target.getComponentCount();

            for (int i = 0; i < n; i++) {
                Component c = target.getComponent(i);
                if (c.isVisible()) {
                    found_one = true;
                    Dimension d = c.getPreferredSize();
                    minx = Math.min(minx, d.width);
                    miny = Math.min(miny, d.height);
                }
            }
            if (found_one) {
                return new Dimension(minx, miny);
            }
            return new Dimension(0, 0);
        }
    }

}
