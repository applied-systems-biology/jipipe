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
import org.hkijena.jipipe.desktop.commons.components.color.chooser.CheckerSwatch;
import org.hkijena.jipipe.desktop.commons.components.color.chooser.ColorRail;
import org.hkijena.jipipe.desktop.commons.components.color.chooser.HsbWell;
import org.hkijena.jipipe.desktop.commons.components.color.chooser.LabWell;
import org.hkijena.jipipe.desktop.commons.components.layouts.JIPipeDesktopWrapLayout;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.theme.ui.JIPipeDesktopModernPillTabbedPaneUI;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.color.ColorSpaces;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;

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
    // Test seams for the numeric spinners (#1339); see ColorChooserDialogSpinnerTest.
    private JSpinner rSpinner;
    private JSpinner labBSpinner;
    private final java.util.IdentityHashMap<ColorRail, JSpinner> railSpinners = new java.util.IdentityHashMap<>();
    /**
     * Creates the dialog (owner may be null; the dialog is not shown by this
     * constructor - use {@link #show} or {@code setVisible(true)}).
     */
    public JIPipeDesktopColorChooserDialog(Window owner, String title, Color initial, boolean withTransparency) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.withTransparency = withTransparency;
        this.currentColor = initial;
        this.setIconImage(UIUtils.getJIPipeIcon128());

        savedRow = new JPanel();
        savedRow.setOpaque(false);
        savedRow.setLayout(new JIPipeDesktopWrapLayout(FlowLayout.LEFT, 4, 4));
        savedRow.setAlignmentX(0f);

        JPanel body = new JPanel(new BorderLayout(0,8));

        getContentPane().setLayout(new BorderLayout(0,8));
        body.add(UIUtils.wrapInBackgroundIslandPanelIfNeeded(buildSpaceTabs()), BorderLayout.CENTER);
        body.add(buildPreviewPanel(), BorderLayout.SOUTH);



        getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        getContentPane().add(body, BorderLayout.CENTER);
        getContentPane().add(buildButtons(), BorderLayout.SOUTH);

        pack();
        setSize(new Dimension(640, 480));
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
    // Swing-structure assertions - see ColorChooserDialogRefreshTest).
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
     * Item 8: the full swatch tooltip - name (if any), {@code #RRGGBB},
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

    private static JButton createSwatchButton(NamedColor c) {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(18, 18));
        b.setMaximumSize(new Dimension(18, 18));
        b.setMinimumSize(new Dimension(18, 18));
        b.setBorder(BorderFactory.createLineBorder(ThemeUtils.getCurrentStyle().getBorderColor(), 1));
        b.setBackground(c.getColor());
        b.setContentAreaFilled(true);
        b.setFocusable(false);
        b.setToolTipText(colorTooltip(c));
        return b;
    }

    /**
     * Item 5: the 16 4-bit colors (standard 4-bit cube order: B, G, R planes),
     * rendered untitled alongside the GNOME HIG palette.
     */
    private static List<NamedColor> pureColors() {
        String[] data = {
                "000000", "FFFFFF", "550000", "AA0000", "FF0000",
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
     * Shows the named space tab by TITLE (used by the screenshot harness).
     * Title-based because the tab order changed with the redesign
     * (a Palette tab was added at index 0) - a hardcoded index silently
     * selected the wrong tab (#1339).
     */
    public void showTab(String title) {
        for (int i = 0; i < spaceTabs.getTabCount(); i++) {
            if (title.equals(spaceTabs.getTitleAt(i))) {
                spaceTabs.setSelectedIndex(i);
                refresh();
                return;
            }
        }
    }

    /**
     * Shows the LAB tab (used by the screenshot harness + tests).
     */
    public void showLabTab() {
        showTab("LAB");
    }

    String getHexText() {
        return hexField.getText();
    }

    JComponent getRedRail() {
        return rRail;
    }

    /**
     * The Red channel's numeric spinner (package-private test seam).
     * See {@code ColorChooserDialogSpinnerTest}.
     */
    JComponent getRedSpinner() {
        return rSpinner;
    }

    /**
     * The LAB b* channel's numeric spinner (package-private test seam).
     * See {@code ColorChooserDialogSpinnerTest}.
     */
    JComponent getLabBSpinner() {
        return labBSpinner;
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

    private JComponent buildPreviewPanel() {
        currentSwatch = new CheckerSwatch();
        return currentSwatch;
    }

    // ------------------------------------------------------------------
    // Palettes
    // ------------------------------------------------------------------

    private JComponent buildSpaceTabs() {
        spaceTabs.addTab("Palette", buildPalettePanel());
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

    private JPanel buildPalettePanel() {
        JIPipeDesktopFormPanel p = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.TRANSPARENT_BACKGROUND + JIPipeDesktopFormPanel.WITH_SCROLLING);
        UIUtils.makeNonOpaque(p, true);
        p.addWideToForm(buildSwatchesRow(pureColors()));
        p.addWideToForm(buildSwatchesRow(gnomeHig()));
        if(!JIPipeColorHistory.get().getRecent().isEmpty()) {
            JIPipeDesktopFormPanel.GroupHeaderPanel historyHeader = p.addGroupHeader("Previous", JIPipe.RESOURCES.getIcon16("actions/shallow-history.png"));
            p.addWideToForm(buildSwatchesRow(JIPipeColorHistory.get().getRecent()));
        }
        JIPipeDesktopFormPanel.GroupHeaderPanel savedHeader = p.addGroupHeader("Saved", JIPipe.RESOURCES.getIcon16("actions/starred.png"));
        p.addWideToForm(savedRow);
        savedHeader.addToEndOfTitlePanel(UIUtils.createParameterButton("Add", JIPipe.RESOURCES.getIcon16("actions/add.png"), e -> {
            JIPipeColorHistory.get().save(new NamedColor(getCurrentColor(), null));
            fillSavedRow();
        }));
        return p;
    }

    private JPanel buildRgbPanel() {
        JIPipeDesktopFormPanel p = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.TRANSPARENT_BACKGROUND);
        p.addToForm(createRailWithSpinner(rRail, v -> {
            int rgb = getCurrentColor().getRGB();
            setCurrentColor(new Color((int) v, ((rgb >> 8) & 0xFF), ((rgb & 0xFF)), ((rgb >> 24) & 0xFF)));
        }), new JLabel("Red"));
        p.addToForm(createRailWithSpinner(gRail, v -> {
            int rgb = getCurrentColor().getRGB();
            setCurrentColor(new Color(((rgb >> 16) & 0xFF), (int) v, ((rgb & 0xFF)), ((rgb >> 24) & 0xFF)));
        }),  new JLabel("Green"));
        p.addToForm(createRailWithSpinner(blueRail, v -> {
            int rgb = getCurrentColor().getRGB();
            setCurrentColor(new Color(((rgb >> 16) & 0xFF), ((rgb >> 8) & 0xFF), (int) v, ((rgb >> 24) & 0xFF)));
        }),   new JLabel("Blue"));
        if (withTransparency) {
            p.addToForm(createRailWithSpinner(alphaRail(), v -> setColorAlpha((int) v)), new  JLabel("Alpha"));
        }
        hexField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, hexField.getFont().getSize()));
        hexField.addActionListener(e -> commitHex());
        hexField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                commitHex();
            }
        });
        p.addToForm(hexField, new JLabel("Hex"));
        return p;
    }

    private JPanel buildHsbPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());
        hsbWell = new HsbWell(this);
        p.add(hsbWell, BorderLayout.CENTER);

        JIPipeDesktopFormPanel rails = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.TRANSPARENT_BACKGROUND);
        hueRail = new ColorRail(0, 360, null);
        satRail = new ColorRail(0, 100, null);
        briRail = new ColorRail(0, 100, null);
        rails.addToForm(createRailWithSpinner(hueRail, v -> {
            double[] hsb = ColorSpaces.toHsb(getCurrentColor());
            setCurrentColor(ColorSpaces.fromHsb(v, hsb[1], hsb[2]));
        }), new JLabel("Hue"));
        rails.addToForm(createRailWithSpinner(satRail, v -> {
            double[] hsb = ColorSpaces.toHsb(getCurrentColor());
            setCurrentColor(ColorSpaces.fromHsb(hsb[0], v, hsb[2]));
        }),  new JLabel("Sat"));
        rails.addToForm(createRailWithSpinner(briRail, v -> {
            double[] hsb = ColorSpaces.toHsb(getCurrentColor());
            setCurrentColor(ColorSpaces.fromHsb(hsb[0], hsb[1], v));
        }),   new JLabel("Bri"));
        if (withTransparency) {
            rails.addToForm(createRailWithSpinner(alphaRail(), v -> setColorAlpha((int) v)),
                    new JLabel("Alpha"));
        }
        p.add(rails, BorderLayout.EAST);
        return p;
    }

    private JPanel buildLabPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());
        labWell = new LabWell(this);
        p.add(labWell, BorderLayout.CENTER);

        JIPipeDesktopFormPanel rails = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.TRANSPARENT_BACKGROUND);
        lRail = new ColorRail(0, 100, i -> new Color((int) i, (int) i, (int) i));
        aRail = new ColorRail(-128, 127, null);
        blueRailLab = new ColorRail(-128, 127, null);
        rails.addToForm(createRailWithSpinner(lRail, v -> setColorLab(v, aRail.getValue(), blueRailLab.getValue())), new  JLabel("L*"));
        rails.addToForm(createRailWithSpinner(aRail, v -> setColorLab(lRail.getValue(), v, blueRailLab.getValue())), new JLabel("a*"));
        rails.addToForm(createRailWithSpinner(blueRailLab, v -> setColorLab(lRail.getValue(), aRail.getValue(), v)), new  JLabel("b*"));
        if (withTransparency) {
            rails.addToForm(createRailWithSpinner(alphaRail(), v -> setColorAlpha((int) v)), new JLabel("Alpha"));
        }
        p.add(rails, BorderLayout.EAST);
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
            result = getCurrentColor();
            JIPipeColorHistory.get().remember(getCurrentColor());
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
        // render as ONE untitled block - no section titles, a single 8px gap
        // between the two groups.
        p.add(buildSwatchesRow(pureColors()));
        p.add(Box.createVerticalStrut(8));
        p.add(buildSwatchesRow(gnomeHig()));
        p.add(sectionLabel("Last selected"));
        p.add(buildSwatchesRow(JIPipeColorHistory.get().getRecent()));

        // Item 6: the star (save) button right-aligns to the edge of the
        // "Saved" section title - not the far dialog edge.
        JPanel savedHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        savedHeader.setOpaque(false);
        savedHeader.add(sectionLabel("Saved"));
        JButton star = new JButton("☆");
        star.setToolTipText("Save the current color");
        star.setFocusable(false);
        star.addActionListener(e -> {
            JIPipeColorHistory.get().save(new NamedColor(getCurrentColor(), null));
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
            JButton b = createSwatchButton(c);
            b.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        JIPipeColorHistory.get().removeSaved(c.getColor());
                        fillSavedRow();
                    } else {
                        setCurrentColor(c.getColor());
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
    private JPanel buildSwatchesRow(List<NamedColor> colors) {
        JPanel p = new JPanel();
        p.setLayout(new JIPipeDesktopWrapLayout(FlowLayout.LEFT, 4, 4));
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        p.setAlignmentX(0f);
        p.setOpaque(false);
        for (NamedColor c : colors) {
            JButton b = createSwatchButton(c);
            // Left-click sets the color (right-click on the Saved row deletes
            // - wired by fillSavedRow).
            b.addActionListener(e -> setCurrentColor(c.getColor()));
            p.add(b);
        }
        return p;
    }

    private void setColorLab(double l, double a, double b) {
        setCurrentColor(ColorSpaces.fromLab(l, a, b));
    }

    private void setColorAlpha(int alpha) {
        int rgb = getCurrentColor().getRGB();
        setCurrentColor(new Color(((rgb >> 16) & 0xFF), ((rgb >> 8) & 0xFF), ((rgb & 0xFF)), alpha));
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
            int rgb = getCurrentColor().getRGB();
            rRail.setValue(((rgb >> 16) & 0xFF));
            gRail.setValue(((rgb >> 8) & 0xFF));
            blueRail.setValue(((rgb & 0xFF)));

            double[] hsb = ColorSpaces.toHsb(getCurrentColor());
            hueRail.setValue(hsb[0]);
            satRail.setValue(hsb[1]);
            briRail.setValue(hsb[2]);
            // Gradient keys: only rebuild a rail's image when its inputs changed
            // (see updateGradient - #1339 lag fix).
            hueRail.updateGradient(null, i -> ColorSpaces.fromHsb(i, 100, 100));
            satRail.updateGradient(new double[]{hsb[0]}, i -> ColorSpaces.fromHsb(hsb[0], i, 100));
            briRail.updateGradient(new double[]{hsb[0], hsb[1]}, i -> ColorSpaces.fromHsb(hsb[0], hsb[1], i));

            double[] lab = ColorSpaces.toLab(getCurrentColor());
            lRail.setValue(lab[0]);
            aRail.setValue(lab[1]);
            blueRailLab.setValue(lab[2]);
            aRail.updateGradient(new double[]{lab[0], lab[2]}, i -> ColorSpaces.fromLab(lab[0], i, lab[2]));
            blueRailLab.updateGradient(new double[]{lab[0], lab[1]}, i -> ColorSpaces.fromLab(lab[0], lab[1], i));

            hexField.setText(ColorSpaces.toHex(getCurrentColor()));
            if (hsbWell != null) {
                hsbWell.repaint();
            }
            if (labWell != null) {
                labWell.repaint();
            }
            if (currentSwatch != null) {
                currentSwatch.setColor(getCurrentColor());
                // Item 8: the current-swatch header tooltip carries the same
                // name/hex/RGB/HSB/L*a*b* detail as every palette swatch.
                currentSwatch.setToolTipText(colorTooltip(new NamedColor(getCurrentColor(), null)));
                currentSwatch.repaint();
            }
            // #1339: keep the numeric spinners in lockstep with the rails
            // (no-op when unchanged, so the spinner<->rail feedback loop
            // terminates).
            for (java.util.Map.Entry<ColorRail, JSpinner> e : railSpinners.entrySet()) {
                e.getValue().setValue(e.getKey().getValue());
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
            setCurrentColor(c);
        } catch (RuntimeException e) {
            hexField.setText(ColorSpaces.toHex(getCurrentColor()));
        }
    }

    private JPanel createRailWithSpinner(ColorRail rail, DoubleConsumer onChange) {
        // GridBag (not BorderLayout): a bare add() into a BorderLayout lands in
        // EAST and would starve the rail (CENTER) of its width.
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        rail.addChangeListener(onChange::accept);
        row.add(rail, BorderLayout.CENTER);
        // #1339: Krita-style numeric editing - a spinner next to every rail.
        JSpinner spinner = createChannelSpinner(rail);
        row.add(spinner, BorderLayout.EAST);
        if (rail == rRail) {
            rSpinner = spinner; // test seam
        } else if (rail == blueRailLab) {
            labBSpinner = spinner; // test seam
        }
        railSpinners.put(rail, spinner);
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
     * A Krita-style numeric spinner for a channel rail (#1339). Range mirrors
     * the rail; 8-bit channels (RGB, alpha) use integer steps, the LAB/HSB
     * axes use 0.1 steps. Typing or using the up/down buttons drives the rail
     * (and thus the color) through the SAME listener the mouse uses; the
     * rail guards on value equality, so the rail<->spinner feedback loop that
     * {@code refresh()} creates terminates naturally.
     */
    private JSpinner createChannelSpinner(ColorRail rail) {
        double min = rail.getMin();
        double max = rail.getMax();
        boolean integer = max <= 255 && min >= 0; // RGB 0-255, alpha 0-255
        double step = integer ? 1 : 0.1;
        SpinnerModel model = integer
                ? new SpinnerNumberModel((int) min, min, max, 1)
                : new SpinnerNumberModel(min, min, max, step);
        String format = integer ? "0" : "0.0";
        JSpinner spinner = new JSpinner(model);
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, format);
        // Size the editor's text field for the full range ("-128", "255",
        // "127.0") and right-align the value like Krita's channel fields.
        editor.getTextField().setColumns(integer ? 3 : 5);
        editor.getTextField().setHorizontalAlignment(JTextField.RIGHT);
        spinner.setEditor(editor);
        // Height 32 (a 20px spinner clipped the text vertically); width leaves
        // room for the value plus the up/down arrows.
        spinner.setPreferredSize(new Dimension(integer ? 64 : 74, 32));
        spinner.getEditor().setToolTipText("Type the value or use the up/down buttons");
        spinner.addChangeListener(e ->
                rail.setValue(((Number) spinner.getValue()).doubleValue()));
        return spinner;
    }

    /**
     * Converts a press/drag on the row into the rail's value (item 4): the
     * x coordinate is clamped to the rail's horizontal span within the row.
     * The rail's own listener still fires - this is a widening of the hit
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

    public Color getCurrentColor() {
        return currentColor;
    }

    public void setCurrentColor(Color currentColor) {
        this.currentColor = currentColor;
        refresh();
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

    public static void main(String[] args) throws UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(
                new javax.swing.plaf.metal.MetalLookAndFeel()
        );
        ThemeUtils.applyThemeFromSettings();
        JIPipeDesktopColorChooserDialog dialog = new JIPipeDesktopColorChooserDialog(null, "Select color", Color.RED, true);
        dialog.setVisible(true);
    }
}
