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

package org.hkijena.jipipe.desktop.commons.components.slider;

import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Locale;

/**
 * A theme-aware, custom-painted horizontal slider for number parameters
 * (#1339 pass 2, S6). Opt-in via
 * {@link org.hkijena.jipipe.plugins.parameters.library.primitives.NumberParameterSettings.Style#SLIDER}
 * on a bounded number parameter.
 * <p>
 * The value math lives in the static methods (headless-testable by
 * {@code NumberSliderMathTest}); this class only paints and routes input:
 * press/drag anywhere sets the value (step-snapped), Shift-drag refines by
 * {@code step/10}, arrow keys adjust by one step, Home/End jump to the range
 * ends, and double-click or F2 opens an inline overlaid text field for exact
 * entry (Enter commits, Esc / invalid / focus-lost reverts). Changes are
 * reported to {@link ChangeListener}s only when user-caused.
 */
public class JIPipeDesktopNumberSlider extends JComponent {

    /** Track height in px (visual; the component itself is the hit target). */
    private static final int TRACK_HEIGHT = 6;
    /** Thumb diameter in px. */
    private static final int THUMB_SIZE = 12;

    private double value;
    private double min;
    private double max;
    private double step;
    private int precision;

    private final ChangeListenerModel changeListeners = new ChangeListenerModel();

    private JTextField inlineEdit;   // non-null while the inline editor is open
    private double inlineEditOrigin;  // the value to revert to if the edit is cancelled

    public JIPipeDesktopNumberSlider() {
        setFocusable(true);
        setPreferredSize(new Dimension(160, 22));
        setToolTipText("slider");
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (isInlineEditing()) {
                    return; // the text field owns the events now
                }
                setValueFromMouse(e, e.isShiftDown());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isInlineEditing()) {
                    return;
                }
                setValueFromMouse(e, e.isShiftDown());
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !isInlineEditing()) {
                    openInlineEdit();
                }
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isInlineEditing()) {
                    return;
                }
                setValueFromMouse(e, e.isShiftDown());
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (isInlineEditing()) {
                    return;
                }
                double s = e.isShiftDown() ? fineStep(step) : step;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT, KeyEvent.VK_DOWN -> commit(value - s);
                    case KeyEvent.VK_RIGHT, KeyEvent.VK_UP -> commit(value + s);
                    case KeyEvent.VK_HOME -> commit(min);
                    case KeyEvent.VK_END -> commit(max);
                    case KeyEvent.VK_F2 -> openInlineEdit();
                    default -> {
                    }
                }
            }
        });
    }

    // ------------------------------------------------------------------
    // Static math (the source of truth; pinned by NumberSliderMathTest)
    // ------------------------------------------------------------------

    /**
     * Rounds to the nearest multiple of {@code step} (relative to
     * {@code min}) and clamps into {@code [min, max]}.
     */
    public static double snap(double value, double min, double max, double step) {
        double snapped;
        if (step > 0) {
            double steps = Math.round((value - min) / step);
            snapped = min + steps * step;
        } else {
            snapped = value;
        }
        return Math.max(min, Math.min(max, snapped));
    }

    /**
     * Maps a fraction of the track (clamped to [0, 1]) to a value, snapped to
     * the step.
     */
    public static double valueFromFraction(double frac, double min, double max, double step) {
        double f = Math.max(0.0, Math.min(1.0, frac));
        return snap(min + f * (max - min), min, max, step);
    }

    /**
     * Maps a value to its fraction on the track ([0, 1]).
     */
    public static double fractionFromValue(double value, double min, double max) {
        if (max <= min) {
            return 0;
        }
        return (value - min) / (max - min);
    }

    /**
     * The fine-adjust step: one tenth of the normal step (Shift-drag /
     * Shift+arrows).
     */
    public static double fineStep(double step) {
        return step / 10.0;
    }

    /**
     * Parses and commits a typed value: locale-insensitive (comma accepted as
     * a decimal separator), snapped to the step and clamped to the range.
     *
     * @return the committed value, or {@code null} if the text is empty or
     * not a number
     */
    public static Double parseCommit(String text, double min, double max, double step) {
        if (text == null) {
            return null;
        }
        String t = text.trim().replace(',', '.');
        if (t.isEmpty()) {
            return null;
        }
        double v;
        try {
            v = Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return null;
        }
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return null;
        }
        return snap(v, min, max, step);
    }

    /**
     * Formats a value with up to {@code precision} decimals, trimming
     * trailing zeros (and a trailing decimal point).
     */
    public static String format(double value, int precision) {
        if (precision <= 0) {
            return String.valueOf((long) value);
        }
        String s = String.format(Locale.ROOT, "%." + precision + "f", value);
        s = s.replaceAll("0+$", "");
        if (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    public double getValue() {
        return value;
    }

    /**
     * Sets the value (clamped + snapped) without firing change listeners —
     * used by the owning editor to sync programmatic updates.
     */
    public void setValue(double v) {
        value = snap(v, min, max, step);
        updateTooltip();
        repaint();
    }

    /**
     * Sets the range and step; the current value is re-snapped/clamped into
     * the new range.
     */
    public void setRange(double min, double max, double step) {
        this.min = min;
        this.max = max;
        this.step = step > 0 ? step : 1;
        value = snap(value, min, max, this.step);
        updateTooltip();
        repaint();
    }

    /**
     * Sets the number of decimals used by the inline text editor and tooltip.
     */
    public void setPrecision(int decimals) {
        this.precision = Math.max(0, decimals);
        updateTooltip();
        repaint();
    }

    public void addChangeListener(ChangeListener l) {
        changeListeners.add(l);
    }

    public void removeChangeListener(ChangeListener l) {
        changeListeners.remove(l);
    }

    // ------------------------------------------------------------------
    // Input plumbing
    // ------------------------------------------------------------------

    private boolean isInlineEditing() {
        return inlineEdit != null;
    }

    private void setValueFromMouse(MouseEvent e, boolean fine) {
        int w = Math.max(1, getWidth() - 2); // leave 1px border margin each side
        double frac = (e.getX() - 1) / (double) w;
        double s = fine ? fineStep(step) : step;
        double v = valueFromFraction(frac, min, max, s);
        commit(v);
    }

    /**
     * Applies a user-caused value change: snaps, fires listeners only when the
     * value actually moved, repaints.
     */
    private void commit(double v) {
        double snapped = snap(v, min, max, step);
        if (snapped == value) {
            return;
        }
        value = snapped;
        updateTooltip();
        repaint();
        changeListeners.fire(this);
    }

    private void updateTooltip() {
        setToolTipText(String.format(Locale.ROOT, "%s (%s – %s, %s)",
                format(value, precision), format(min, precision), format(max, precision), format(step, precision)));
    }

    // ------------------------------------------------------------------
    // Inline text editor (double-click / F2)
    // ------------------------------------------------------------------

    private void openInlineEdit() {
        if (isInlineEditing()) {
            return;
        }
        inlineEditOrigin = value;
        inlineEdit = new JTextField(format(value, precision));
        inlineEdit.setFont(new Font(Font.MONOSPACED, Font.PLAIN, getFont().getSize()));
        inlineEdit.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        inlineEdit.setBounds(0, 0, getWidth(), getHeight());
        inlineEdit.addActionListener(e -> commitInline());
        inlineEdit.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    e.consume();
                    revertInline();
                }
            }
        });
        inlineEdit.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (isInlineEditing()) {
                    // Lost focus without committing (Tab away, click elsewhere): revert.
                    revertInline();
                }
            }
        });
        add(inlineEdit);
        inlineEdit.revalidate();
        inlineEdit.repaint();
        inlineEdit.requestFocusInWindow();
        inlineEdit.selectAll();
        repaint();
    }

    private void commitInline() {
        if (!isInlineEditing()) {
            return;
        }
        Double v = parseCommit(inlineEdit.getText(), min, max, step);
        remove(inlineEdit);
        inlineEdit = null;
        if (v != null) {
            commit(v);
        } else {
            // invalid/empty: revert to the value at edit start
            setValue(inlineEditOrigin);
        }
        revalidate();
        repaint();
    }

    private void revertInline() {
        if (!isInlineEditing()) {
            return;
        }
        remove(inlineEdit);
        inlineEdit = null;
        setValue(inlineEditOrigin);
        revalidate();
        repaint();
        requestFocusInWindow();
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int trackY = (h - TRACK_HEIGHT) / 2;
        int thumbCenter = thumbX(w);

        // Unfilled track
        Color trackColor = UIManager.getColor("Panel.background");
        if (trackColor == null) {
            trackColor = new Color(220, 220, 220);
        }
        g2.setColor(trackColor);
        roundedFill(g2, 1, trackY, w - 2, TRACK_HEIGHT, TRACK_HEIGHT / 2);

        // Filled portion (primary)
        Color primary = ThemeUtils.getCurrentStyle().getPrimaryColor();
        g2.setColor(primary);
        int filledW = Math.max(0, thumbCenter - 1);
        if (filledW > 0) {
            roundedFill(g2, 1, trackY, filledW, TRACK_HEIGHT, TRACK_HEIGHT / 2);
        }

        // Thumb (hidden while the inline editor is up)
        if (!isInlineEditing()) {
            g2.setColor(primary);
            g2.fillOval(thumbCenter - THUMB_SIZE / 2, (h - THUMB_SIZE) / 2, THUMB_SIZE, THUMB_SIZE);
            g2.setColor(Color.WHITE);
            g2.drawOval(thumbCenter - THUMB_SIZE / 2, (h - THUMB_SIZE) / 2, THUMB_SIZE, THUMB_SIZE);
        }

        // Focus ring
        if (isFocusOwner() && isEnabled()) {
            g2.setColor(new Color(primary.getRed(), primary.getGreen(), primary.getBlue(), 110));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
        }

        // Disabled look
        if (!isEnabled()) {
            Color bg = UIManager.getColor("Panel.background");
            if (bg != null) {
                g2.setColor(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 160));
                g2.fillRect(0, 0, w, h);
            }
        }

        g2.dispose();
    }

    private int thumbX(int w) {
        double frac = fractionFromValue(value, min, max);
        return (int) Math.round(1 + frac * (w - 2));
    }

    private static void roundedFill(Graphics2D g2, int x, int y, int w, int h, int arc) {
        g2.fillRoundRect(x, y, w, h, arc, arc);
    }

    /** Minimal ChangeListener container. */
    private static class ChangeListenerModel {
        private final List<ChangeListener> listeners = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        void add(ChangeListener l) {
            listeners.add(l);
        }

        void remove(ChangeListener l) {
            listeners.remove(l);
        }

        void fire(Object source) {
            for (ChangeListener l : listeners) {
                l.stateChanged(new javax.swing.event.ChangeEvent(source));
            }
        }
    }
}
