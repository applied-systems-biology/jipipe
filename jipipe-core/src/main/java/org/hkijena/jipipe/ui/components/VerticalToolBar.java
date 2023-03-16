package org.hkijena.jipipe.ui.components;

import org.hkijena.jipipe.extensions.settings.GeneralUISettings;
import org.hkijena.jipipe.ui.theme.DarkModernMetalTheme;
import org.hkijena.jipipe.ui.theme.ModernMetalTheme;

import javax.swing.*;
import java.awt.*;

public class VerticalToolBar extends JPanel {
    public VerticalToolBar() {
        initialize();
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        switch (GeneralUISettings.getInstance().getTheme()) {
            case ModernLight:
                setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, ModernMetalTheme.GRAY));
                break;
            case ModernDark:
                setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, DarkModernMetalTheme.GRAY));
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
