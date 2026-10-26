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

package org.hkijena.jipipe.plugins.parameters.ui.library;

import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.color.JIPipeColorHistory;
import org.hkijena.jipipe.desktop.commons.components.color.NamedColor;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.selection.JIPipeDesktopRichSelectionBox;
import org.hkijena.jipipe.plugins.parameters.library.primitives.JIPipeFontParameter;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Editor for the dedicated {@link JIPipeFontParameter} (#1339): a compact
 * inline preview row (sample text rendered in the current font) that opens a
 * popout with family (searchable box), size, style and an <em>embedded</em>
 * color section (swatch, pure colors, recents, Custom…) — no popup-over-popup.
 * Custom color selection hides the popout first, then shows the modal color
 * dialog; the value is applied live and the inline preview updates on return.
 */
public class JIPipeDesktopFontParameterEditorUI extends JIPipeDesktopParameterEditorUI<JIPipeFontParameter> {

    /** The sample text rendered in the preview. */
    private static final String SAMPLE_TEXT = "Aa Label text 12";

    private static final Pattern MONO_PATTERN =
            Pattern.compile("mono|courier|consolas|menlo|dejavu sans mono|liberation mono|source code|fira code|jetbrains|inconsolata|ubunto? mono", Pattern.CASE_INSENSITIVE);
    private static final Pattern SERIF_PATTERN =
            Pattern.compile("serif|times|georgia|garamond|baskerville|nimbus roman|palatino|libre", Pattern.CASE_INSENSITIVE);

    private FontPreviewControl preview;
    private JPopupMenu popup;

    public JIPipeDesktopFontParameterEditorUI(InitializationParameters parameters) {
        super(JIPipeFontParameter.class, parameters);
        initialize();
    }

    @Override
    public void reload() {
        JIPipeFontParameter parameter = getParameter();
        preview.updateFrom(parameter);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        preview = new FontPreviewControl();
        // A non-zero minimum AND preferred size keeps the GridBag row from
        // collapsing the control (the same failure mode as the boolean switch
        // — #1339; in packed panels the row gets only its preferred height).
        preview.setMinimumSize(new Dimension(120, 22));
        preview.setPreferredSize(new Dimension(120, 22));
        preview.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    togglePopup();
                }
            }
        });
        add(preview, BorderLayout.CENTER);
    }

    // ------------------------------------------------------------------
    // Popout
    // ------------------------------------------------------------------

    private void togglePopup() {
        if (popup != null && popup.isVisible()) {
            popup.setVisible(false);
            return;
        }
        popup = new JPopupMenu();
        popup.add(getPopoutContent());
        // The popup auto-sizes to the content's preferred size; the preview
        // band's fixed width (see buildPreviewBand) drives the column width.
        popup.show(preview, 0, preview.getHeight() + 2);
        preview.repaint();
    }

    /**
     * Builds the popout's content panel. Extracted as a public seam so the
     * screenshot harness (the #1339 VLM loop) can render the popout's content
     * directly — a shown {@link JPopupMenu} can't be captured, but its content
     * panel can. The form panel's GridBagLayout sizes the label column to the
     * widest label under the active theme font, which keeps all controls
     * left-aligned to one shared inset (spec §5).
     */
    public JComponent getPopoutContent() {
        JIPipeDesktopFormPanel form = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.NONE);
        form.addToForm(buildPreviewBand(), new JLabel(""));
        form.addToForm(buildFamilyBox(), new JLabel("Family"));
        form.addToForm(buildSizeSpinner(), new JLabel("Size"));
        form.addToForm(buildStyleToggles(), new JLabel("Style"));
        form.addToForm(buildColorSection(), new JLabel("Color"));
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(form, BorderLayout.NORTH);
        return wrap;
    }

    private JComponent buildPreviewBand() {
        FontPreviewBand band = new FontPreviewBand();
        // The band's preferred width drives the popout's column width (the
        // JPopupMenu auto-sizes to its content's preferred size).
        band.setPreferredSize(new Dimension(300, 56));
        band.setMinimumSize(new Dimension(0, 56));
        return band;
    }

    private JComponent buildFamilyBox() {
        JIPipeFontParameter parameter = getParameter();
        List<String> baseFamilies = fontFamilies();
        // Include a family that is not in the system list (e.g. from an old
        // project) so the current value is selectable.
        final List<String> families;
        if (baseFamilies.contains(parameter.getFamily())) {
            families = baseFamilies;
        } else {
            families = new ArrayList<>(baseFamilies);
            families.add(0, parameter.getFamily());
        }
        JIPipeDesktopRichSelectionBox box = new JIPipeDesktopRichSelectionBox("Family", false,
                () -> familyOptions(families),
                selected -> {
                    if (selected.isEmpty()) {
                        return;
                    }
                    parameter.setFamily((String) selected.get(0).getValue());
                    setParameter(parameter, false);
                });
        box.setSelectedValues(List.of(parameter.getFamily()));
        box.refreshDisplay();
        return box;
    }

    private List<String> fontFamilies() {
        try {
            List<String> families = new ArrayList<>(Arrays.asList(
                    java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
            families.sort(String.CASE_INSENSITIVE_ORDER);
            return families;
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private List<JIPipeDesktopRichSelectionBox.Option<?>> familyOptions(List<String> families) {
        List<JIPipeDesktopRichSelectionBox.Option<?>> result = new ArrayList<>();
        for (String family : families) {
            result.add(new JIPipeDesktopRichSelectionBox.Option<>(family, family, null, fontGroup(family), null));
        }
        return result;
    }

    /** Rough family classification (name-based; sans-serif is the catch-all). */
    private static String fontGroup(String family) {
        if (MONO_PATTERN.matcher(family).find()) {
            return "Monospace";
        }
        if (SERIF_PATTERN.matcher(family).find()) {
            return "Serif";
        }
        return "Sans-serif";
    }

    private JComponent buildSizeSpinner() {
        JIPipeFontParameter parameter = getParameter();
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(parameter.getSize(), 1, 999, 1));
        spinner.addChangeListener(e -> {
            parameter.setSize((Integer) spinner.getValue());
            setParameter(parameter, false);
        });
        // Pin the width for a 3-digit value + arrows; take the height from the
        // spinner's natural default (never a flat 22px that undercuts the
        // theme font's line height — spec §5).
        Dimension d = spinner.getPreferredSize();
        spinner.setPreferredSize(new Dimension(80, Math.max(d.height, 24)));
        return spinner;
    }

    private JComponent buildStyleToggles() {
        JIPipeFontParameter parameter = getParameter();
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);
        JButton bold = new JButton("B");
        bold.setOpaque(false);
        bold.setFont(bold.getFont().deriveFont(Font.BOLD));
        bold.setFocusable(false);
        Dimension boldDim = bold.getPreferredSize();
        bold.setPreferredSize(new Dimension(28, Math.max(boldDim.height, 24)));
        JButton italic = new JButton("I");
        italic.setOpaque(false);
        italic.setFont(italic.getFont().deriveFont(Font.ITALIC));
        italic.setFocusable(false);
        Dimension italicDim = italic.getPreferredSize();
        italic.setPreferredSize(new Dimension(28, Math.max(italicDim.height, 24)));
        bold.setSelected((parameter.getStyle() & Font.BOLD) != 0);
        italic.setSelected((parameter.getStyle() & Font.ITALIC) != 0);
        bold.addActionListener(e -> {
            int style = bold.isSelected() ? parameter.getStyle() | Font.BOLD : parameter.getStyle() & ~Font.BOLD;
            parameter.setStyle(style);
            setParameter(parameter, false);
        });
        italic.addActionListener(e -> {
            int style = italic.isSelected() ? parameter.getStyle() | Font.ITALIC : parameter.getStyle() & ~Font.ITALIC;
            parameter.setStyle(style);
            setParameter(parameter, false);
        });
        p.add(bold);
        p.add(italic);
        return p;
    }

    private JComponent buildColorSection() {
        JIPipeFontParameter parameter = getParameter();
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(new JLabel("Color"), BorderLayout.WEST);
        JButton custom = new JButton("Custom…");
        custom.setFocusable(false);
        custom.addActionListener(e -> pickCustomColor(parameter));
        header.add(custom, BorderLayout.EAST);
        p.add(header);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.setOpaque(false);
        // Current-color swatch (click → custom dialog, popout hides first)
        JButton swatch = new JButton();
        swatch.setPreferredSize(new Dimension(18, 18));
        swatch.setMinimumSize(new Dimension(18, 18));
        swatch.setMaximumSize(new Dimension(18, 18));
        swatch.setBackground(parameter.getColor());
        swatch.setContentAreaFilled(true);
        swatch.setBorder(BorderFactory.createLineBorder(new Color(110, 110, 110), 1, true));
        swatch.setFocusable(false);
        swatch.setToolTipText("Current color " + ColorUtils.colorToHexString(parameter.getColor()) + " — click to open the full picker");
        swatch.addActionListener(e -> pickCustomColor(parameter));
        row.add(swatch);
        // Pure colors
        String[][] pure = {
                {"#000000", "Black"}, {"#FFFFFF", "White"}, {"#FF0000", "Red"}, {"#FF8800", "Orange"},
                {"#EEEE00", "Yellow"}, {"#00CC00", "Green"}, {"#0066FF", "Blue"}, {"#8800CC", "Purple"},
        };
        for (String[] c : pure) {
            row.add(colorDot(ColorUtils.hexStringToColor(c[0]), c[0] + " " + c[1], parameter));
        }
        p.add(row);

        // Recents
        List<NamedColor> recent = JIPipeColorHistory.get().getRecent();
        if (!recent.isEmpty()) {
            JPanel recents = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            recents.setOpaque(false);
            for (NamedColor rc : recent) {
                recents.add(colorDot(rc.getColor(), ColorUtils.colorToHexString(rc.getColor()), parameter));
            }
            p.add(recents);
        }
        return p;
    }

    private JButton colorDot(Color color, String tooltip, JIPipeFontParameter parameter) {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(16, 16));
        b.setMinimumSize(new Dimension(16, 16));
        b.setMaximumSize(new Dimension(16, 16));
        b.setBackground(color);
        b.setContentAreaFilled(true);
        b.setBorder(BorderFactory.createLineBorder(new Color(110, 110, 110), 1, true));
        b.setFocusable(false);
        b.setToolTipText(tooltip);
        b.addActionListener(e -> {
            parameter.setColor(color);
            setParameter(parameter, false);
        });
        return b;
    }

    /**
     * Hides the popout first, then shows the modal color dialog — the value is
     * applied live, and the inline preview is updated on return (no stacked
     * popups; #1339).
     */
    private void pickCustomColor(JIPipeFontParameter parameter) {
        if (popup != null && popup.isVisible()) {
            popup.setVisible(false);
        }
        Color chosen = UIUtils.selectColor(preview, "Select color", parameter.getColor(), false);
        if (chosen != null) {
            parameter.setColor(chosen);
            setParameter(parameter, false);
        }
        preview.repaint();
    }

    // ------------------------------------------------------------------
    // Preview painting
    // ------------------------------------------------------------------

    /**
     * The two checkerboard tones for the font preview, derived from the theme
     * surface color (spec: the preview background must be theme-derived so it
     * works in light AND dark themes). The surface tone is the theme's panel
     * background; the alternate tone is a fixed mid-gray that is clearly
     * distinct from near-white and near-black surfaces. The mid tone guarantees
     * that font colors matching the surface (white text on a light theme, black
     * text on a dark theme) still remain visible — fixing the blank font
     * preview (#1339 S8). Pure, no Swing state, headless-testable.
     *
     * @param surface the theme's panel background color
     * @return {@code {surfaceTone, alternateTone}}
     */
    static Color[] checkerColors(Color surface) {
        // A mid-gray that contrasts against both near-white and near-black
        // surfaces while staying clearly distinct from them (gap >= 0.15 in
        // relative luminance — pinned by FontPreviewCheckerTest).
        Color mid = new Color(0x8a, 0x8a, 0x8a);
        return new Color[]{surface, mid};
    }

    /**
     * Paints a {@code sq}×{@code sq} checkerboard across the component using
     * {@link #checkerColors(Color)} derived from the theme surface.
     */
    private static void paintCheckerboard(Graphics2D g, JComponent c, int sq) {
        Color[] tones = checkerColors(UIManager.getColor("Panel.background"));
        int w = c.getWidth(), h = c.getHeight();
        for (int y = 0; y < h; y += sq) {
            for (int x = 0; x < w; x += sq) {
                boolean light = ((x / sq + y / sq) & 1) == 0;
                g.setColor(light ? tones[0] : tones[1]);
                g.fillRect(x, y, Math.min(sq, w - x), Math.min(sq, h - y));
            }
        }
    }

    /** The inline preview: sample text in the current font, scaled to the row. */
    private class FontPreviewControl extends JComponent {
        FontPreviewControl() {
            setToolTipText("Click to edit the font");
            addHierarchyListener(e -> repaint());
        }

        void updateFrom(JIPipeFontParameter parameter) {
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            JIPipeFontParameter parameter = getParameter();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            // Theme-derived checkerboard so light AND dark font colors are
            // visible (a flat panel background hid the default white text).
            paintCheckerboard(g2, this, 6);
            Font font = parameter.toFont();
            float maxH = getHeight() * 0.78f;
            Font render = font.getSize2D() > maxH ? font.deriveFont(maxH) : font;
            g2.setFont(render);
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(parameter.getColor());
            g2.drawString(SAMPLE_TEXT, 6, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.setColor(new Color(110, 110, 110, 90));
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            g2.dispose();
        }
    }

    /** The larger preview band inside the popout (renders at true size, clipped). */
    private class FontPreviewBand extends JComponent {
        @Override
        protected void paintComponent(Graphics g) {
            JIPipeFontParameter parameter = getParameter();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            paintCheckerboard(g2, this, 6);
            Font font = parameter.toFont();
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(parameter.getColor());
            g2.drawString(SAMPLE_TEXT, 8, Math.min((getHeight() - fm.getHeight()) / 2 + fm.getAscent(), getHeight() - 4));
            g2.setColor(new Color(110, 110, 110, 90));
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            g2.dispose();
        }
    }
}
