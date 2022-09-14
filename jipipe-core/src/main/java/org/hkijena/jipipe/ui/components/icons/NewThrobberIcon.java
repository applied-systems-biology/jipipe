package org.hkijena.jipipe.ui.components.icons;

import org.hkijena.jipipe.ui.theme.ModernMetalTheme;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.util.Arrays;

public class NewThrobberIcon implements Icon {

    public static final int ANIMATION_DELAY = 80;
    private final Timer timer;

    private Component parent;
    private final int numLines = 14;
    private final double[] x1Locations = new double[numLines];
    private final double[] y1Locations = new double[numLines];
    private final double[] x2Locations = new double[numLines];
    private final double[] y2Locations = new double[numLines];

    private final Color[] colors;

    private int colorShift = 0;

    public NewThrobberIcon(Component parent) {
        this.parent = parent;
        this.timer = new Timer(ANIMATION_DELAY, e -> updateIcon());
        this.timer.setRepeats(true);
        this.timer.setCoalesce(false);

        final double rInner = getIconWidth() / 2.0 * 0.6;
        final double rOuter = getIconWidth() / 2.0 - 1.0;
        for (int i = 0; i < numLines; i++) {
            double angle = i * (2.0 * Math.PI / numLines);
            x1Locations[i] = (Math.cos(angle) * rInner) + getIconWidth() / 2.0;
            y1Locations[i] = (Math.sin(angle) * rInner) + getIconHeight() / 2.0;
            x2Locations[i] = (Math.cos(angle) * rOuter) + getIconWidth() / 2.0;
            y2Locations[i] = (Math.sin(angle) * rOuter) + getIconHeight() / 2.0;
        }
        Color baseColor = UIUtils.DARK_THEME ? new Color(0xdfdfdf) : new Color(0x444444);
        colors = ColorUtils.renderGradient(Arrays.asList(
                new ColorUtils.GradientStop(0.0f, baseColor),
                new ColorUtils.GradientStop(0.75f, ModernMetalTheme.PRIMARY5),
                new ColorUtils.GradientStop(1.0f, ModernMetalTheme.PRIMARY6)), numLines);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D graphics2D = (Graphics2D) g;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        for (int i = 0; i < numLines; i++) {
            graphics2D.setColor(colors[(numLines - i - 1 + colorShift) % numLines]);
            graphics2D.draw(new Line2D.Double(x1Locations[i], y1Locations[i], x2Locations[i], y2Locations[i]));
        }
    }

    private void updateIcon() {
        colorShift = (colorShift + 1) % numLines;
        if (parent != null && parent.isDisplayable()) {
            parent.repaint();
            parent.getToolkit().sync();
        } else {
            timer.stop();
        }
    }

    public void start() {
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    public Timer getTimer() {
        return timer;
    }

    public Component getParent() {
        return parent;
    }

    public void setParent(Component parent) {
        this.parent = parent;
    }

    @Override
    public int getIconWidth() {
        return 16;
    }

    @Override
    public int getIconHeight() {
        return 16;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        JPanel panel = new JPanel(new BorderLayout());
        frame.setContentPane(panel);
        NewThrobberIcon icon = new NewThrobberIcon(panel);
        icon.start();
        JLabel label = new JLabel(icon);
        panel.add(label, BorderLayout.CENTER);
        frame.setSize(400,400);
        frame.revalidate();
        frame.repaint();
        frame.setVisible(true);
    }


}
