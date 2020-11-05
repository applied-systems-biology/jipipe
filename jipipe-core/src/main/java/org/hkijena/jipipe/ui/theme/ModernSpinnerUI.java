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

import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSpinnerUI;
import java.awt.Component;
import java.awt.Dimension;

public class ModernSpinnerUI extends BasicSpinnerUI {

    public ModernSpinnerUI() {
    }

    @Override
    protected Component createPreviousButton() {
        JButton button = new JButton(UIUtils.getIconFromResources("actions/arrow-down.png"));
        button.setBackground(UIManager.getColor( "Spinner.background"));
        button.setPreferredSize(new Dimension(21, 14));
        button.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        installPreviousButtonListeners(button);
        return button;
    }

    @Override
    protected Component createNextButton() {
        JButton button = new JButton(UIUtils.getIconFromResources("actions/arrow-up.png"));
        button.setBackground(UIManager.getColor( "Spinner.background"));
        button.setPreferredSize(new Dimension(21, 14));
        button.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        installNextButtonListeners(button);
        return button;
    }

    public static ComponentUI createUI(JComponent c)    {
        return new ModernSpinnerUI();
    }
}
