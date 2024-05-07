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

package org.hkijena.jipipe.desktop.commons.components;

import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopDarkModernMetalTheme;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernMetalTheme;
import org.hkijena.jipipe.plugins.settings.JIPipeGeneralUIApplicationSettings;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopVerticalToolBar extends JPanel {
    public JIPipeDesktopVerticalToolBar() {
        initialize();
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        switch (JIPipeGeneralUIApplicationSettings.getInstance().getTheme()) {
            case ModernLight:
                setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, JIPipeDesktopModernMetalTheme.GRAY));
                break;
            case ModernDark:
                setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, JIPipeDesktopDarkModernMetalTheme.GRAY));
                break;
        }
    }

    public void addSeparator() {
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setMaximumSize(new Dimension(Short.MAX_VALUE, 8));
        add(separator);
    }

    @Override
    public Component add(Component comp) {
        if (comp instanceof JComponent) {
            ((JComponent) comp).setAlignmentY(JComponent.TOP_ALIGNMENT);
        }
        return super.add(comp);
    }
}
