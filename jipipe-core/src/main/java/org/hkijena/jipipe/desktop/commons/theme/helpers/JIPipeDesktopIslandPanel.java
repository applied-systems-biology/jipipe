package org.hkijena.jipipe.desktop.commons.theme.helpers;

import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * A special island panel used for the modern theme.
 * To be used within {@link org.hkijena.jipipe.utils.UIUtils}
 */
public class JIPipeDesktopIslandPanel extends JPanel {

    public JIPipeDesktopIslandPanel(JComponent content) {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBorder(UIUtils.createEmptyBorder(5));
        setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());
        add(content, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        Object oldValue = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int arc = 20;
        g2.setColor(ThemeUtils.getCurrentStyle().getPanelBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

//        g2.setColor(UIUtils.CURRENT_STYLE.getWindowBackground());
//        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldValue);
    }
}
