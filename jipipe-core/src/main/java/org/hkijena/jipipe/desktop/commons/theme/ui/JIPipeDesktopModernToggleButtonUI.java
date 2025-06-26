package org.hkijena.jipipe.desktop.commons.theme.ui;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToggleButtonUI;
import java.awt.*;

public class JIPipeDesktopModernToggleButtonUI extends BasicToggleButtonUI {

    private static final int ARC = 10;
    private static final int SPACING = 2;

    @Override
    protected void paintButtonPressed(Graphics g, AbstractButton b) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(UIManager.getColor("ToggleButton.select"));
        g2.fillRoundRect(SPACING, SPACING, b.getWidth() - SPACING * 2, b.getHeight() - SPACING * 2, ARC, ARC);
        g2.dispose();
    }

    @Override
    public void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setFocusPainted(false);
        b.setBorderPainted(true);
    }

    public static ComponentUI createUI(JComponent c) {
        return new JIPipeDesktopModernToggleButtonUI();
    }
}
