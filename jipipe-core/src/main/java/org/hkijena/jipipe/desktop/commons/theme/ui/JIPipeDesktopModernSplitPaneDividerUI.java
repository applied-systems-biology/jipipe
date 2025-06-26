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

package org.hkijena.jipipe.desktop.commons.theme.ui;

import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;

import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;

public class JIPipeDesktopModernSplitPaneDividerUI extends BasicSplitPaneDivider {
    private final Color dividerColor;

    public JIPipeDesktopModernSplitPaneDividerUI(BasicSplitPaneUI ui) {
        super(ui);
        this.dividerColor = JIPipeDesktopModernThemeStyle.getCurrent().getWindowBackground();
        setBackground(dividerColor);
    }

    @Override
    public void paint(Graphics g) {
        g.setColor(Color.RED);
        g.fillRect(0, 0, getWidth(), getHeight());
    }
}
