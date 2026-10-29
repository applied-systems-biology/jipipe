package org.hkijena.jipipe.desktop.commons.components.color.chooser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.IntFunction;

/**
 * A horizontal gradient rail with a draggable handle. Custom-painted so
 * the gradient renders identically in every look & feel (JSlider's track
 * paints over its background). Keyboard accessible (arrows adjust).
 */
public class ColorRail extends JComponent {
    private final double min;
    private final double max;
    private final boolean checkerboard;
    private IntFunction<Color> gradient;
    private double value;
    private List<DoubleConsumer> listeners = new ArrayList<>();
    private BufferedImage image;
    private boolean gradientDirty = true;
    private double[] gradientKey;

    public ColorRail(double min, double max, IntFunction<Color> gradient) {
        this(min, max, gradient, false);
    }

    public ColorRail(double min, double max, IntFunction<Color> gradient, boolean checkerboard) {
        this.min = min;
        this.max = max;
        this.gradient = gradient;
        this.checkerboard = checkerboard;
        this.value = min;
        setFocusable(true);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                updateFromMouse(e);
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                updateFromMouse(e);
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                double step = (max - min) / 255.0;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT, KeyEvent.VK_DOWN -> setValue(value - step);
                    case KeyEvent.VK_RIGHT, KeyEvent.VK_UP -> setValue(value + step);
                    default -> {
                    }
                }
            }
        });
    }

    private void updateFromMouse(MouseEvent e) {
        double frac = Math.max(0, Math.min(1, e.getX() / (double) Math.max(1, getWidth())));
        setValue(min + frac * (max - min));
    }

    public double getValue() {
        return value;
    }

    public void setValue(double v) {
        v = Math.max(min, Math.min(max, v));
        if (v == value) {
            return;
        }
        value = v;
        repaint();
        for (DoubleConsumer l : listeners) {
            l.accept(v);
        }
    }

    public void addChangeListener(DoubleConsumer l) {
        listeners.add(l);
    }

    /**
     * Replaces the (color-dependent) gradient function and repaints.
     */
    void repaintGradient(IntFunction<Color> newGradient) {
        if (newGradient != null) {
            this.gradient = newGradient;
        }
        this.gradientDirty = true;
        repaint();
    }

    /**
     * Like {@link #repaintGradient} but skips the work when the controlling
     * inputs are unchanged (#1339). During an a/b drag on the LAB well only
     * one channel changes, so the other rails keep their cached gradient
     * image instead of rebuilding a {@link BufferedImage} per frame — the
     * main cause of the drag lag.
     */
    public void updateGradient(double[] key, IntFunction<Color> newGradient) {
        if (key != null && java.util.Arrays.equals(key, gradientKey)) {
            return;
        }
        gradientKey = key != null ? key.clone() : null;
        repaintGradient(newGradient);
    }

    private void ensureImage() {
        if (!gradientDirty) {
            return;
        }
        int w = Math.max(2, getWidth());
        int h = Math.max(2, getHeight());
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        if (checkerboard) {
            int sq = 4;
            for (int y = 0; y < h; y += sq) {
                for (int x = 0; x < w; x += sq) {
                    boolean light = ((x / sq + y / sq) & 1) == 0;
                    g.setColor(light ? new Color(0xdd, 0xdd, 0xdd) : new Color(0xbb, 0xbb, 0xbb));
                    g.fillRect(x, y, sq, sq);
                }
            }
        }
        for (int x = 0; x < w; x++) {
            int frac = (int) (x / (double) (w - 1) * (max - min) + min);
            Color c = gradient != null ? gradient.apply(frac) : new Color(120, 120, 120);
            g.setColor(c);
            g.drawLine(x, 0, x, h - 1);
        }
        g.dispose();
        this.image = img;
        this.gradientDirty = false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        ensureImage();
        int w = getWidth(), h = getHeight();
        g.drawImage(image, 0, 0, null);
        int x = (int) ((value - min) / (max - min) * w);
        x = Math.max(0, Math.min(w - 1, x));
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(Color.WHITE);
        g2.drawLine(x, 0, x, h);
        g2.setColor(Color.BLACK);
        g2.drawLine(x + 1, 0, x + 1, h);
        g2.dispose();
        g.setColor(new Color(110, 110, 110));
        g.drawRect(0, 0, w - 1, h - 1);
        if (isFocusOwner()) {
            g.setColor(new Color(60, 130, 220));
            g.drawRect(1, 1, w - 3, h - 3);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(160, 14);
    }
}
