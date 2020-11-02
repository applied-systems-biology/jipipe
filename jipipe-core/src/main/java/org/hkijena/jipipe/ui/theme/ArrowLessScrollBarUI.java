/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.theme;

import org.hkijena.jipipe.extensions.settings.GeneralUISettings;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Dimension;

public class ArrowLessScrollBarUI extends BasicScrollBarUI {
    @Override
    protected JButton createDecreaseButton(int orientation) {
        if (GeneralUISettings.getInstance().getTheme().isModern())
            return createZeroButton();
        else
            return super.createDecreaseButton(orientation);
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        if (GeneralUISettings.getInstance().getTheme().isModern())
            return createZeroButton();
        else
            return super.createIncreaseButton(orientation);
    }

    private JButton createZeroButton() {
        JButton jbutton = new JButton();
        jbutton.setPreferredSize(new Dimension(0, 0));
        jbutton.setMinimumSize(new Dimension(0, 0));
        jbutton.setMaximumSize(new Dimension(0, 0));
        return jbutton;
    }

    public static ComponentUI createUI(JComponent c)    {
        return new ArrowLessScrollBarUI();
    }

}
