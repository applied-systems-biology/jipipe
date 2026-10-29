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

package org.hkijena.jipipe.desktop.commons.components.color;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.commons.theme.ui.JIPipeDesktopModernPillTabbedPaneUI;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.color.ColorSpaces;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.IntFunction;

/**
 * A modern color chooser dialog (#1339) replacing {@link JColorChooser}:
 * three color spaces (RGB / HSB / CIE L*a*b*), a two-way hex field
 * ({@code #RRGGBB} / {@code #RRGGBBAA}), palettes (pure colors, the GNOME
 * HIG named library, last selected, user-saved), and optional alpha support.
 * <p>
 * Modal, same contract as {@link JColorChooser#showDialog}: {@link #show}
 * returns the chosen color or {@code null} on cancel. Out-of-gamut L*a*b*
 * values clamp to the nearest sRGB live (via {@link ColorSpaces#fromLab}),
 * so the hex readout always shows a valid color.
 */
public class JIPipeDesktopColorChooserDialog extends JDialog {

    // ------------------------------------------------------------------
    // Public entry point (same contract as JColorChooser.showDialog)
    // ------------------------------------------------------------------

    private final boolean withTransparency;

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------
    private final JTextField hexField = new JTextField(12);
    private final JTabbedPane spaceTabs = new JTabbedPane();
    // RGB rails
    private final ColorRail rRail = new ColorRail(0, 255, i -> new Color((int) i, 0, 0));
    private final ColorRail gRail = new ColorRail(0, 255, i -> new Color(0, (int) i, 0));
    private final ColorRail blueRail = new ColorRail(0, 255, i -> new Color(0, 0, (int) i));
    private final JPanel savedRow;
    private Color currentColor;
    private Color result;
    private boolean updating = false;
    // HSB
    private HsbWell hsbWell;
    private ColorRail hueRail;
    private ColorRail satRail;
    private ColorRail briRail;

    // LAB
    private LabWell labWell;
    private ColorRail lRail;
    private ColorRail aRail;
    private ColorRail blueRailLab;

    private CheckerSwatch currentSwatch;
    /**
     * Creates the dialog (owner may be null; the dialog is not shown by this
     * constructor — use {@link #show} or {@code setVisible(true)}).
     */
    public JIPipeDesktopColorChooserDialog(Window owner, String title, Color initial, boolean withTransparency) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.withTransparency = withTransparency;
        this.currentColor = initial;

        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.add(buildTop(), BorderLayout.NORTH);
        body.add(buildSpaceTabs(), BorderLayout.CENTER);
        body.add(buildHexRow(), BorderLayout.SOUTH);

        savedRow = new JPanel();
        // Item 6: the saved swatches wrap like the other palette rows
        // (item 1: never a horizontal scrollbar).
        savedRow.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));
        savedRow.setAlignmentX(0f);

        JPanel palettes = buildPalettes();
        JScrollPane paletteScroll = new JScrollPane(palettes);
        paletteScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        // Item 1: swatch rows wrap (FlowLayout), so only vertical scrolling is
        // ever needed — no horizontal scrollbar in the palette area.
        paletteScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        paletteScroll.setBorder(BorderFactory.createEmptyBorder());
        paletteScroll.setPreferredSize(new Dimension(0, 200));

        setLayout(new BorderLayout(0, 8));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        add(body, BorderLayout.NORTH);
        add(paletteScroll, BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        pack();
        setSize(new Dimension(Math.max(getWidth(), 380), Math.max(getHeight(), 560)));
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        refresh();
    }

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    /**
     * Shows the dialog modally.
     *
     * @param parent           the parent component
     * @param title            the window title
     * @param initial          the initial color
     * @param withTransparency whether to show the alpha rail + checkerboard
     * @return the chosen color, or {@code null} if the user cancelled
     */
    public static Color show(Component parent, String title, Color initial, boolean withTransparency) {
        Window owner = parent instanceof Window w ? w
                : (parent != null ? SwingUtilities.getWindowAncestor(parent) : null);
        JIPipeDesktopColorChooserDialog dialog = new JIPipeDesktopColorChooserDialog(owner, title,
                initial != null ? initial : Color.WHITE, withTransparency);
        dialog.setVisible(true);
        return dialog.result;
    }

    /**
     * A tab component with the standard JIPipe tab padding (the same border
     * {@link org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane} applies to its document tabs), so the pill is
     * comfortably sized instead of hugging the label text (#1339).
     */
    private static JComponent createPaddedTabComponent(String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        panel.add(new JLabel(title), BorderLayout.CENTER);
        return panel;
    }

    // ------------------------------------------------------------------
    // Test seams (package-private; drive the input plumbing without
    // Swing-structure assertions — see ColorChooserDialogRefreshTest).
    // ------------------------------------------------------------------

    private static Component sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setAlignmentX(0f);
        l.setBorder(BorderFactory.createEmptyBorder(6, 0, 2, 0));
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        l.setOpaque(false);
        return l;
    }

    /**
     * Item 8: the full swatch tooltip — name (if any), {@code #RRGGBB},
     * {@code RGB(r, g, b)}, {@code HSB(h°, s%, b%)}, {@code L*a*b*(L, a, b)}.
     * Shared by every swatch source (pure, HIG, last selected, saved) and the
     * current-swatch header, so the format stays in one place.
     */
    private static String colorTooltip(NamedColor c) {
        Color color = c.getColor();
        double[] hsb = ColorSpaces.toHsb(color);
        double[] lab = ColorSpaces.toLab(color);
        return String.join(System.lineSeparator(),
                c.getName() != null ? c.getName() : " (unnamed)",
                ColorSpaces.toHex(color),
                String.format("RGB(%d, %d, %d)", color.getRed(), color.getGreen(), color.getBlue()),
                String.format("HSB(%.0f°, %.0f%%, %.0f%%)", hsb[0], hsb[1], hsb[2]),
                String.format("L*a*b*(%.1f, %.1f, %.1f)", lab[0], lab[1], lab[2]));
    }

    private static JButton swatchButton(NamedColor c) {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(18, 18));
        b.setMaximumSize(new Dimension(18, 18));
        b.setMinimumSize(new Dimension(18, 18));
        b.setBorder(BorderFactory.createLineBorder(new Color(110, 110, 110), 1, true));
        b.setBackground(c.getColor());
        b.setContentAreaFilled(true);
        b.setFocusable(false);
        b.setToolTipText(colorTooltip(c));
        return b;
    }

    private static JPanel railPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        return p;
    }

    /**
     * Item 5: the 16 4-bit colors (standard 4-bit cube order: B, G, R planes),
     * rendered untitled alongside the GNOME HIG palette.
     */
    private static List<NamedColor> pureColors() {
        String[] data = {
                "000000", "550000", "AA0000", "FF0000",
                "005500", "00AA00", "00FF00", "555500",
                "AAAA00", "FFFF00", "000055", "0000AA",
                "0000FF", "550055", "AA00AA", "FF00FF",
        };
        List<NamedColor> out = new ArrayList<>();
        for (String hex : data) {
            out.add(new NamedColor(ColorSpaces.fromHex("#" + hex), null));
        }
        return out;
    }

    /**
     * The GNOME HIG / libadwaita named palette: 9 hue families × 5 steps
     * (values from the GNOME HIG palette page / libadwaita {@code _palette.scss}).
     */
    private static List<NamedColor> gnomeHig() {
        String[] names = {"Blue", "Green", "Yellow", "Orange", "Red", "Purple", "Brown", "Light", "Dark"};
        String[][] rows = {
                {"#99c1f1", "#62a0ea", "#3584e4", "#1c71d8", "#1a5fb4"},
                {"#8ff0a4", "#57e389", "#33d17a", "#2ec27e", "#26a269"},
                {"#f9f06b", "#f8e45c", "#f6d32d", "#f5c211", "#e5a50a"},
                {"#ffbe6f", "#ffa348", "#ff7800", "#e66100", "#c64600"},
                {"#f66151", "#ed333b", "#e01b24", "#c01c28", "#a51d2d"},
                {"#dc8add", "#c061cb", "#9141ac", "#813d9c", "#613583"},
                {"#cdab8f", "#b5835a", "#986a44", "#865e3c", "#63452c"},
                {"#ffffff", "#f6f5f4", "#deddda", "#c0bfbc", "#9a9996"},
                {"#77767b", "#5e5c64", "#3d3846", "#241f31", "#000000"},
        };
        List<NamedColor> out = new ArrayList<>();
        for (int r = 0; r < names.length; r++) {
            for (int s = 0; s < rows[r].length; s++) {
                out.add(new NamedColor(ColorSpaces.fromHex(rows[r][s]), names[r] + " " + (s + 1)));
            }
        }
        return out;
    }

    /**
     * Shows the LAB tab (used by the screenshot harness).
     */
    public void showLabTab() {
        spaceTabs.setSelectedIndex(2);
        refresh();
    }

    String getHexText() {
        return hexField.getText();
    }

    JComponent getRedRail() {
        return rRail;
    }

    JComponent getHsbWell() {
        return hsbWell;
    }

    JComponent getLabWell() {
        return labWell;
    }

    /**
     * The a/b position the LAB marker is drawn at (package-private test seam):
     * the raw pointer coordinates when a drag happened, otherwise the current
     * color's own a/b. See {@code ColorChooserDialogLabMarkerTest}.
     */
    double[] getLabMarkerAb() {
        return labWell == null ? null : labWell.markerAb();
    }

    private JComponent buildTop() {
        currentSwatch = new CheckerSwatch(44, 26);
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(new JLabel("Select color"), BorderLayout.WEST);
        header.add(currentSwatch, BorderLayout.EAST);
        return header;
    }

    // ------------------------------------------------------------------
    // Palettes
    // ------------------------------------------------------------------

    private JComponent buildSpaceTabs() {
        spaceTabs.addTab("RGB", buildRgbPanel());
        spaceTabs.addTab("HSB", buildHsbPanel());
        spaceTabs.addTab("LAB", buildLabPanel());
        // Item 2: pill tabs under the modern theme (mirrors JIPipeDesktopTabPane);
        // the default UI under the classic theme.
        if (ThemeUtils.isUsingModernTheme()) {
            spaceTabs.setUI(new JIPipeDesktopModernPillTabbedPaneUI());
        }
        // The pill UI inherits Metal's compact tabInsets, so bare addTab() labels
        // render as small pills. The rest of JIPipe gets comfortable pills from
        // JIPipeDesktopTabPane's padded tab components (a JPanel with the
        // standard tab border). Give each tab the same padded component so the
        // RGB/HSB/LAB pills match "the design as usual" (#1339).
        for (int i = 0; i < spaceTabs.getTabCount(); i++) {
            spaceTabs.setTabComponentAt(i, createPaddedTabComponent(spaceTabs.getTitleAt(i)));
        }
        return spaceTabs;
    }

    private JPanel buildRgbPanel() {
        JPanel p = railPanel();
        p.add(railRow("Red", rRail, v -> {
            int rgb = currentColor.getRGB();
            setColor(new Color((int) v, ((rgb >> 8) & 0xFF), ((rgb & 0xFF)), ((rgb >> 24) & 0xFF)));
        }));
        p.add(railRow("Green", gRail, v -> {
            int rgb = currentColor.getRGB();
            setColor(new Color(((rgb >> 16) & 0xFF), (int) v, ((rgb & 0xFF)), ((rgb >> 24) & 0xFF)));
        }));
        p.add(railRow("Blue", blueRail, v -> {
            int rgb = currentColor.getRGB();
            setColor(new Color(((rgb >> 16) & 0xFF), ((rgb >> 8) & 0xFF), (int) v, ((rgb >> 24) & 0xFF)));
        }));
        if (withTransparency) {
            p.add(railRow("Alpha", alphaRail(), v -> setColorAlpha((int) v)));
        }
        return p;
    }

    private JPanel buildHsbPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        hsbWell = new HsbWell();
        p.add(hsbWell, BorderLayout.CENTER);

        JPanel rails = railPanel();
        hueRail = new ColorRail(0, 360, null);
        satRail = new ColorRail(0, 100, null);
        briRail = new ColorRail(0, 100, null);
        rails.add(railRow("Hue", hueRail, v -> {
            double[] hsb = ColorSpaces.toHsb(currentColor);
            setColor(ColorSpaces.fromHsb(v, hsb[1], hsb[2]));
        }));
        rails.add(railRow("Sat", satRail, v -> {
            double[] hsb = ColorSpaces.toHsb(currentColor);
            setColor(ColorSpaces.fromHsb(hsb[0], v, hsb[2]));
        }));
        rails.add(railRow("Bri", briRail, v -> {
            double[] hsb = ColorSpaces.toHsb(currentColor);
            setColor(ColorSpaces.fromHsb(hsb[0], hsb[1], v));
        }));
        if (withTransparency) {
            rails.add(railRow("Alpha", alphaRail(), v -> setColorAlpha((int) v)));
        }
        p.add(rails, BorderLayout.EAST);
        return p;
    }

    private JPanel buildLabPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        labWell = new LabWell();
        p.add(labWell, BorderLayout.CENTER);

        JPanel rails = railPanel();
        lRail = new ColorRail(0, 100, i -> new Color((int) i, (int) i, (int) i));
        aRail = new ColorRail(-128, 127, null);
        blueRailLab = new ColorRail(-128, 127, null);
        rails.add(railRow("L", lRail, v -> setColorLab(v, aRail.getValue(), blueRailLab.getValue())));
        rails.add(railRow("a", aRail, v -> setColorLab(lRail.getValue(), v, blueRailLab.getValue())));
        rails.add(railRow("b", blueRailLab, v -> setColorLab(lRail.getValue(), aRail.getValue(), v)));
        if (withTransparency) {
            rails.add(railRow("Alpha", alphaRail(), v -> setColorAlpha((int) v)));
        }
        p.add(rails, BorderLayout.EAST);
        return p;
    }

    private JPanel buildHexRow() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        hexField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, hexField.getFont().getSize()));
        hexField.addActionListener(e -> commitHex());
        hexField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                commitHex();
            }
        });
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(new JLabel("Hex"));
        left.add(hexField);
        p.add(left, BorderLayout.CENTER);
        return p;
    }

    private JComponent buildButtons() {
        // JIPipe dialog convention (#1339): Cancel on the left, OK on the right
        // (rightmost = confirm), both with the standard dialog icons. The
        // earlier version had OK on the left, which every other JIPipe dialog
        // reverses.
        JButton cancel = new JButton("Cancel", JIPipe.RESOURCES.getIcon16("actions/dialog-cancel.png"));
        cancel.addActionListener(e -> setVisible(false));
        JButton ok = new JButton("OK", JIPipe.RESOURCES.getIcon16("actions/dialog-ok.png"));
        ok.addActionListener(e -> {
            result = currentColor;
            JIPipeColorHistory.get().remember(currentColor);
            setVisible(false);
        });
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        p.setOpaque(false);
        p.add(cancel);
        p.add(ok);
        return p;
    }

    private JPanel buildPalettes() {
        // A viewport-width-tracking panel: its preferred width follows the
        // scroll viewport, so the FlowLayout rows below actually WRAP to a new
        // line instead of overflowing to the right and being clipped (#1339).
        // (A plain BoxLayout/FlowLayout panel reports a preferred width as wide
        // as its longest row, which the scroll pane then clips.)
        JPanel p = new ScrollablePalettePanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(true);
        p.setBackground(UIManager.getColor("Panel.background"));
        // Items 5/7: the 16 pure (4-bit) colors and the GNOME HIG palette
        // render as ONE untitled block — no section titles, a single 8px gap
        // between the two groups.
        p.add(swatchesRow(pureColors()));
        p.add(Box.createVerticalStrut(8));
        p.add(swatchesRow(gnomeHig()));
        p.add(sectionLabel("Last selected"));
        p.add(swatchesRow(JIPipeColorHistory.get().getRecent()));

        // Item 6: the star (save) button right-aligns to the edge of the
        // "Saved" section title — not the far dialog edge.
        JPanel savedHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        savedHeader.setOpaque(false);
        savedHeader.add(sectionLabel("Saved"));
        JButton star = new JButton("☆");
        star.setToolTipText("Save the current color");
        star.setFocusable(false);
        star.addActionListener(e -> {
            JIPipeColorHistory.get().save(new NamedColor(currentColor, null));
            fillSavedRow();
        });
        savedHeader.add(star);
        p.add(savedHeader);
        fillSavedRow();
        p.add(savedRow);
        return p;
    }

    // ------------------------------------------------------------------
    // Color state
    // ------------------------------------------------------------------

    private void fillSavedRow() {
        // Item 6: the saved swatches wrap like the other rows (item 1:
        // never a horizontal scrollbar).
        savedRow.removeAll();
        for (NamedColor c : JIPipeColorHistory.get().getSaved()) {
            JButton b = swatchButton(c);
            b.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        JIPipeColorHistory.get().removeSaved(c.getColor());
                        fillSavedRow();
                    } else {
                        setColor(c.getColor());
                    }
                }
            });
            savedRow.add(b);
        }
        savedRow.revalidate();
        savedRow.repaint();
    }

    /**
     * Item 6: one shared left margin for swatch rows and section titles
     * (the titles are bare, the rows carry a matching left inset); swatch
     * cells uniform 18×18 with a 4px gap. Item 1: rows WRAP so the palette
     * area never needs a horizontal scrollbar (only vertical).
     */
    private JPanel swatchesRow(List<NamedColor> colors) {
        JPanel p = new JPanel();
        p.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        p.setAlignmentX(0f);
        p.setOpaque(false);
        for (NamedColor c : colors) {
            JButton b = swatchButton(c);
            // Left-click sets the color (right-click on the Saved row deletes
            // — wired by fillSavedRow).
            b.addActionListener(e -> setColor(c.getColor()));
            p.add(b);
        }
        return p;
    }

    private void setColorLab(double l, double a, double b) {
        setColor(ColorSpaces.fromLab(l, a, b));
    }

    private void setColorAlpha(int alpha) {
        int rgb = currentColor.getRGB();
        setColor(new Color(((rgb >> 16) & 0xFF), ((rgb >> 8) & 0xFF), ((rgb & 0xFF)), alpha));
    }

    private void setColor(Color c) {
        currentColor = c;
        refresh();
    }

    // ------------------------------------------------------------------
    // Rail plumbing
    // ------------------------------------------------------------------

    private void refresh() {
        if (updating) {
            return;
        }
        // Re-entrancy guard for the real cycle: refresh -> rail.setValue ->
        // listener -> setColor -> refresh. A nested refresh returns early, but
        // the top-level call runs to completion and updates the rails, wells,
        // hex field, and preview swatch.
        updating = true;
        try {
            int rgb = currentColor.getRGB();
            rRail.setValue(((rgb >> 16) & 0xFF));
            gRail.setValue(((rgb >> 8) & 0xFF));
            blueRail.setValue(((rgb & 0xFF)));

            double[] hsb = ColorSpaces.toHsb(currentColor);
            hueRail.setValue(hsb[0]);
            satRail.setValue(hsb[1]);
            briRail.setValue(hsb[2]);
            // Gradient keys: only rebuild a rail's image when its inputs changed
            // (see updateGradient — #1339 lag fix).
            hueRail.updateGradient(null, i -> ColorSpaces.fromHsb(i, 100, 100));
            satRail.updateGradient(new double[]{hsb[0]}, i -> ColorSpaces.fromHsb(hsb[0], i, 100));
            briRail.updateGradient(new double[]{hsb[0], hsb[1]}, i -> ColorSpaces.fromHsb(hsb[0], hsb[1], i));

            double[] lab = ColorSpaces.toLab(currentColor);
            lRail.setValue(lab[0]);
            aRail.setValue(lab[1]);
            blueRailLab.setValue(lab[2]);
            aRail.updateGradient(new double[]{lab[0], lab[2]}, i -> ColorSpaces.fromLab(lab[0], i, lab[2]));
            blueRailLab.updateGradient(new double[]{lab[0], lab[1]}, i -> ColorSpaces.fromLab(lab[0], lab[1], i));

            hexField.setText(ColorSpaces.toHex(currentColor));
            if (hsbWell != null) {
                hsbWell.repaint();
            }
            if (labWell != null) {
                labWell.repaint();
            }
            if (currentSwatch != null) {
                currentSwatch.setColor(currentColor);
                // Item 8: the current-swatch header tooltip carries the same
                // name/hex/RGB/HSB/L*a*b* detail as every palette swatch.
                currentSwatch.setToolTipText(colorTooltip(new NamedColor(currentColor, null)));
                currentSwatch.repaint();
            }
        } finally {
            updating = false;
        }
    }

    private void commitHex() {
        try {
            Color c = ColorSpaces.fromHex(hexField.getText());
            if (!withTransparency) {
                c = new Color(c.getRGB()); // normalize away any alpha
            }
            setColor(c);
        } catch (RuntimeException e) {
            hexField.setText(ColorSpaces.toHex(currentColor));
        }
    }

    private JPanel railRow(String label, ColorRail rail, DoubleConsumer onChange) {
        // GridBag (not BorderLayout): a bare add() into a BorderLayout lands in
        // EAST and would starve the rail (CENTER) of its width.
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        // Item 3: a fixed-width, right-aligned label cell so every rail
        // (RGB/HSB/LAB incl. α) starts at the same x and the labels form a
        // clean column.
        JLabel l = new JLabel(label, SwingConstants.RIGHT);
        l.setOpaque(false);
        l.setPreferredSize(new Dimension(34, l.getPreferredSize().height));
        // Item 4: the rail keeps its ~12px visual height, but the whole row
        // (~22px) is the clickable area — press/drag anywhere on the row
        // (including the label) drives the rail, with x converted to the
        // rail's span.
        rail.setMaximumSize(new Dimension(Integer.MAX_VALUE, 12));
        rail.setMinimumSize(new Dimension(120, 12));
        rail.addChangeListener(onChange::accept);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 4, 6);
        c.anchor = GridBagConstraints.CENTER;
        row.add(l, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        row.add(rail, c);
        MouseAdapter rowForwarder = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                forwardToRail(e, rail, row, true);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                forwardToRail(e, rail, row, false);
            }
        };
        row.addMouseListener(rowForwarder);
        row.addMouseMotionListener(rowForwarder);
        return row;
    }

    /**
     * Converts a press/drag on the row into the rail's value (item 4): the
     * x coordinate is clamped to the rail's horizontal span within the row.
     * The rail's own listener still fires — this is a widening of the hit
     * target, not a replacement.
     */
    private void forwardToRail(MouseEvent e, ColorRail rail, JPanel row, boolean press) {
        if (e.getComponent() == rail) {
            return; // the rail handles its own events
        }
        Point railOrigin = rail.getLocationOnScreen();
        Point point = e.getLocationOnScreen();
        if (point.x < railOrigin.x || point.x >= railOrigin.x + rail.getWidth()) {
            return; // outside the rail's span (the label cell)
        }
        int xInRail = point.x - railOrigin.x;
        int yInRail = Math.max(0, Math.min(rail.getHeight() - 1, point.y - railOrigin.y));
        if (press) {
            rail.dispatchEvent(new MouseEvent(rail, MouseEvent.MOUSE_PRESSED, e.getWhen(), e.getModifiersEx(), xInRail, yInRail, e.getClickCount(), false));
        } else {
            rail.dispatchEvent(new MouseEvent(rail, MouseEvent.MOUSE_DRAGGED, e.getWhen(), e.getModifiersEx(), xInRail, yInRail, e.getClickCount(), false));
        }
    }

    // ------------------------------------------------------------------
    // Palette data
    // ------------------------------------------------------------------

    /**
     * An alpha rail: fully transparent (left, 0) → opaque (right, 255) over a checkerboard.
     */
    private ColorRail alphaRail() {
        return new ColorRail(0, 255, i -> new Color(0, 0, 0, (int) i), true);
    }

    /**
     * A color swatch with a checkerboard behind it (shows alpha).
     */
    private static class CheckerSwatch extends JComponent {
        private Color c;

        CheckerSwatch(int w, int h) {
            setPreferredSize(new Dimension(w, h));
            setMinimumSize(new Dimension(w, h));
        }

        void setColor(Color c) {
            this.c = c;
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth(), h = getHeight();
            int sq = 6;
            for (int y = 0; y < h; y += sq) {
                for (int x = 0; x < w; x += sq) {
                    boolean light = ((x / sq + y / sq) & 1) == 0;
                    g.setColor(light ? new Color(0xdd, 0xdd, 0xdd) : new Color(0xbb, 0xbb, 0xbb));
                    g.fillRect(x, y, Math.min(sq, w - x), Math.min(sq, h - y));
                }
            }
            if (c != null) {
                g.setColor(c);
                g.fillRect(0, 0, w, h);
            }
            g.setColor(new Color(110, 110, 110));
            g.drawRect(0, 0, w - 1, h - 1);
        }
    }

    // ------------------------------------------------------------------
    // Color rail (custom slider)
    // ------------------------------------------------------------------

    /**
     * A palette container whose preferred/minimum width follow the enclosing
     * scroll viewport (#1339). Without this, the swatch rows keep their
     * unwrapped preferred width and the scroll pane clips them at the right
     * edge instead of wrapping them onto the next line.
     */
    private class ScrollablePalettePanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return new Dimension(0, 200);
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 80;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            // Force the width to the viewport width → the FlowLayout rows wrap.
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // 2D wells
    // ------------------------------------------------------------------

    /**
     * A horizontal gradient rail with a draggable handle. Custom-painted so
     * the gradient renders identically in every look & feel (JSlider's track
     * paints over its background). Keyboard accessible (arrows adjust).
     */
    private class ColorRail extends JComponent {
        private final double min;
        private final double max;
        private final boolean checkerboard;
        private IntFunction<Color> gradient;
        private double value;
        private List<DoubleConsumer> listeners = new ArrayList<>();
        private BufferedImage image;
        private boolean gradientDirty = true;
        private double[] gradientKey;

        ColorRail(double min, double max, IntFunction<Color> gradient) {
            this(min, max, gradient, false);
        }

        ColorRail(double min, double max, IntFunction<Color> gradient, boolean checkerboard) {
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

        double getValue() {
            return value;
        }

        void setValue(double v) {
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

        void addChangeListener(DoubleConsumer l) {
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
        void updateGradient(double[] key, IntFunction<Color> newGradient) {
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

    /**
     * Saturation × brightness well at the current hue (HSB space).
     */
    private class HsbWell extends JComponent {
        private BufferedImage bg;
        private double bgHue = Double.NaN;

        HsbWell() {
            setPreferredSize(new Dimension(160, 160));
            setMinimumSize(new Dimension(120, 120));
            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    update(e);
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    update(e);
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        private void update(MouseEvent e) {
            double sat = e.getX() / (double) Math.max(1, getWidth()) * 100;
            double bri = (1.0 - e.getY() / (double) Math.max(1, getHeight())) * 100;
            double hue = ColorSpaces.toHsb(currentColor)[0];
            setColor(ColorSpaces.fromHsb(hue, sat, bri));
        }

        @Override
        protected void paintComponent(Graphics g) {
            ensureBg();
            int w = getWidth(), h = getHeight();
            g.drawImage(bg, 0, 0, w, h, null);
            double[] hsb = ColorSpaces.toHsb(currentColor);
            int x = (int) (hsb[1] / 100 * (w - 1));
            int y = (int) ((1 - hsb[2] / 100) * (h - 1));
            g.setColor(Color.WHITE);
            g.drawOval(x - 5, y - 5, 10, 10);
            g.setColor(Color.BLACK);
            g.drawOval(x - 4, y - 4, 8, 8);
            g.setColor(new Color(110, 110, 110));
            g.drawRect(0, 0, w - 1, h - 1);
        }

        private void ensureBg() {
            double hue = ColorSpaces.toHsb(currentColor)[0];
            if (bg == null || bgHue != hue) {
                int s = 96;
                bg = new BufferedImage(s, s, BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < s; y++) {
                    double bri = (1 - y / (double) (s - 1)) * 100;
                    for (int x = 0; x < s; x++) {
                        double sat = x / (double) (s - 1) * 100;
                        bg.setRGB(x, y, ColorSpaces.fromHsb(hue, sat, bri).getRGB());
                    }
                }
                bgHue = hue;
            }
        }
    }

    /**
     * a/b chromaticity plane at the current L (LAB space, clamped to sRGB).
     */
    private class LabWell extends JComponent {
        private BufferedImage bg;
        private double bgL = Double.NaN;
        // The last pointer position in a/b, in *unclamped* LAB coordinates. The
        // marker is drawn here (not from toLab(currentColor)) so it follows the
        // cursor even when the picked color is out of the sRGB gamut and
        // ColorSpaces.fromLab clamped it — the circle no longer "sticks" at the
        // clamped position (#1339).
        private double pointerA = Double.NaN;
        private double pointerB = Double.NaN;

        LabWell() {
            setPreferredSize(new Dimension(160, 160));
            setMinimumSize(new Dimension(120, 120));
            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    update(e);
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    update(e);
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        private void update(MouseEvent e) {
            double a = (e.getX() / (double) Math.max(1, getWidth()) * 2 - 1) * 128;
            double b = (1 - e.getY() / (double) Math.max(1, getHeight()) * 2 + 1) * 128;
            pointerA = a;
            pointerB = b;
            double L = ColorSpaces.toLab(currentColor)[0];
            setColor(ColorSpaces.fromLab(L, a, b));
        }

        /**
         * @return the a/b the marker is drawn at (pointer position or color's own).
         */
        double[] markerAb() {
            if (Double.isNaN(pointerA) || Double.isNaN(pointerB)) {
                double[] lab = ColorSpaces.toLab(currentColor);
                return new double[]{lab[1], lab[2]};
            }
            return new double[]{pointerA, pointerB};
        }

        @Override
        protected void paintComponent(Graphics g) {
            ensureBg();
            int w = getWidth(), h = getHeight();
            g.drawImage(bg, 0, 0, w, h, null);
            // crosshair axes
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(255, 255, 255, 110));
            g2.drawLine(w / 2, 0, w / 2, h);
            g2.drawLine(0, h / 2, w, h / 2);
            g2.setColor(new Color(0, 0, 0, 110));
            g2.drawLine(w / 2 + 1, 0, w / 2 + 1, h);
            g2.drawLine(0, h / 2 + 1, w, h / 2 + 1);
            // corner tags
            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(9f));
            g2.drawString("-a", 2, h - 3);
            g2.drawString("+a", w - 16, 10);
            g2.drawString("+b", 2, 10);
            g2.drawString("-b", w - 16, h - 3);
            // marker: prefer the raw pointer position (follows the cursor even
            // when the color was gamut-clamped); fall back to the color's own
            // a/b when there is no pointer yet (e.g. externally-set color).
            double[] lab = ColorSpaces.toLab(currentColor);
            double a = Double.isNaN(pointerA) ? lab[1] : pointerA;
            double b = Double.isNaN(pointerB) ? lab[2] : pointerB;
            int x = (int) ((a + 128) / 256 * (w - 1));
            int y = (int) ((128 - b) / 256 * (h - 1));
            g2.setColor(Color.WHITE);
            g2.drawOval(x - 5, y - 5, 10, 10);
            g2.setColor(Color.BLACK);
            g2.drawOval(x - 4, y - 4, 8, 8);
            g2.dispose();
            g.setColor(new Color(110, 110, 110));
            g.drawRect(0, 0, w - 1, h - 1);
        }

        private void ensureBg() {
            double L = ColorSpaces.toLab(currentColor)[0];
            if (bg == null || bgL != L) {
                int s = 96;
                bg = new BufferedImage(s, s, BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < s; y++) {
                    double b = (1 - y / (double) (s - 1)) * 256 - 128;
                    for (int x = 0; x < s; x++) {
                        double a = x / (double) (s - 1) * 256 - 128;
                        bg.setRGB(x, y, ColorSpaces.fromLab(L, a, b).getRGB());
                    }
                }
                bgL = L;
            }
        }
    }
}
