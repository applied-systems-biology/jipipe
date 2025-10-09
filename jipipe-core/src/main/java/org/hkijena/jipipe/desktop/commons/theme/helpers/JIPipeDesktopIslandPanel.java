package org.hkijena.jipipe.desktop.commons.theme.helpers;

import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * A special island panel used for the modern theme.
 * To be used within {@link org.hkijena.jipipe.utils.UIUtils}
 */
public class JIPipeDesktopIslandPanel extends JPanel {

    private final int cornerRadius;
    private final JIPipeDesktopModernThemeStyle style;

    public JIPipeDesktopIslandPanel(JComponent content) {
        this.style = ThemeUtils.getCurrentStyle();
        this.cornerRadius = style.getIslandsCornerRadius();
        setLayout(new BorderLayout());
        setOpaque(true);
        setBorder(UIUtils.createEmptyBorder(5));
        setBackground(style.getWindowBackground());
        add(content, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        Object oldValue = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int arc = cornerRadius;
        g2.setColor(style.getPanelBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

        if(style.isIslandsDrawBorder()) {
            g2.setColor(style.getIslandsBorderColor());
            g2.drawRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        }

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldValue);
    }
}
