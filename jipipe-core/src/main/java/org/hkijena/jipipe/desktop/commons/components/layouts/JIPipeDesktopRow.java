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

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopRow extends JPanel {
    private int maxWidth = 1140;

    public JIPipeDesktopRow() {
        this(16);
    }

    public JIPipeDesktopRow(int gutter) {
        setLayout(new JIPipeDesktopColumnLayout(gutter));
        setOpaque(false);
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(maxWidth, Integer.MAX_VALUE);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension pref = super.getPreferredSize();
        if (pref.width > maxWidth) {
            pref.width = maxWidth;
        }
        return pref;
    }
}
