package org.hkijena.jipipe.desktop.app.customizer;

import com.google.common.collect.ImmutableSet;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.LayerUI;
import javax.swing.plaf.metal.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.function.Supplier;

public class ThemePreviewPanel extends JPanel {

    public static final class Config {
        public final float uiScale;          // e.g. 1.0f, 1.25f, 1.5f, 2.0f
        public final MetalTheme metalTheme;  // your custom theme or OceanTheme
        public final boolean paintScale;     // true = also scale Graphics2D

        public Config(float uiScale, MetalTheme theme, boolean paintScale) {
            this.uiScale = uiScale;
            this.metalTheme = Objects.requireNonNull(theme);
            this.paintScale = paintScale;
        }
    }

    /**
     * Creates a preview panel that is visually independent from your app’s global UI settings.
     * It swaps Metal theme and scaled defaults only during subtree construction, then restores.
     */
    public static ThemePreviewPanel create(Config cfg) {
        assert EventQueue.isDispatchThread();
        // Snapshot current app-wide state
        LookAndFeel lafBefore = UIManager.getLookAndFeel();
        UIDefaults defaultsBefore = UIManager.getLookAndFeelDefaults();
        MetalTheme themeBefore = (lafBefore instanceof MetalLookAndFeel)
                ? MetalLookAndFeel.getCurrentTheme()
                : null;

        try {
            // Install Metal + desired theme for construction
            MetalLookAndFeel.setCurrentTheme(cfg.metalTheme);
            if (!(lafBefore instanceof MetalLookAndFeel)) {
                UIManager.setLookAndFeel(new MetalLookAndFeel());
            } else {
                // refresh defaults after theme swap
                UIManager.setLookAndFeel(lafBefore);
            }

            // Build a *scaled* defaults table we’ll apply to the subtree
            UIDefaults scaled = copyAndScaleDefaults(UIManager.getLookAndFeelDefaults(), cfg.uiScale);
            // Build subtree under these defaults by temporarily swapping UIManager defaults
            return buildWithTemporaryDefaults(() -> new ThemePreviewPanel(cfg, scaled), scaled, cfg.paintScale);
        } catch (UnsupportedLookAndFeelException e) {
            throw new RuntimeException(e);
        } finally {
            // Restore global theme/LAF immediately so nothing outside is affected
            try {
                if (themeBefore != null) MetalLookAndFeel.setCurrentTheme(themeBefore);
                UIManager.setLookAndFeel(lafBefore);
            } catch (UnsupportedLookAndFeelException ignored) { }
            // Re-attach the original defaults table
            UIManager.getLookAndFeelDefaults().putAll(defaultsBefore);
        }
    }

    private final Config cfg;
    private final UIDefaults localDefaults;
    private JLayer<JComponent> scaledLayer;

    private ThemePreviewPanel(Config cfg, UIDefaults localDefaults) {
        super(new BorderLayout());
        this.cfg = cfg;
        this.localDefaults = localDefaults;

        // Build the miniature “scene”
        JComponent scene = buildScene();

        // Apply per-subtree defaults via a root JComponent client property trick:
        // Many UIs read defaults on install; we force an update under our local table.
        applyDefaultsToSubtree(scene, localDefaults);

        if (cfg.paintScale && cfg.uiScale != 1.0f) {
            scaledLayer = new JLayer<>(scene, new ScaleLayerUI(cfg.uiScale));
            add(scaledLayer, BorderLayout.CENTER);
        } else {
            add(scene, BorderLayout.CENTER);
        }

        setBorder(new EmptyBorder(8, 8, 8, 8));
    }

    // ---- Utility: Build with temporary defaults so UIs install with our table ----
    private static ThemePreviewPanel buildWithTemporaryDefaults(
            Supplier<ThemePreviewPanel> builder,
            UIDefaults local, boolean paintScale) {
        UIDefaults before = UIManager.getLookAndFeelDefaults();
        try {
            UIManager.getLookAndFeelDefaults().putAll(local);
            ThemePreviewPanel p = builder.get();
            SwingUtilities.updateComponentTreeUI(p);
            return p;
        } finally {
            UIManager.getLookAndFeelDefaults().putAll(before);
        }
    }

    // ---- Utility: Apply fonts/colors/icons from local defaults to subtree ----
    private static void applyDefaultsToSubtree(Component c, UIDefaults defs) {
        if (c instanceof JComponent jc) {
            // Try to refresh the UI under our defaults
            jc.putClientProperty("Preview.UIDefaults", defs); // marker if you want later updates
            jc.updateUI();
        }
        if (c instanceof Container cont) {
            for (Component child : cont.getComponents()) applyDefaultsToSubtree(child, defs);
        }
    }

    // ---- Build the little demo scene ----
    private JComponent buildScene() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Overview", buildOverview());
        tabs.add("Controls", buildControls());
        tabs.add("Split", buildSplit());
        return tabs;
    }

    private JComponent buildOverview() {
        JPanel p = new JPanel(new BorderLayout(8,8));
        JLabel title = new JLabel("Preview Title — Metal");
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 2f));
        title.setBorder(new EmptyBorder(4,4,4,4));

        JTextArea info = new JTextArea("""
            This preview shows buttons, a toolbar, tabs, lists, and a table.
            Fonts, colors, and icons reflect the *local* defaults for this component only.
            """);
        info.setEditable(false);
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        info.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Description"),
                new EmptyBorder(6,6,6,6)));

        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.add(new AbstractAction("New", UIManager.getIcon("FileView.fileIcon")) {
            public void actionPerformed(ActionEvent e) {}
        });
        tb.add(new AbstractAction("Open", UIManager.getIcon("FileView.directoryIcon")) {
            public void actionPerformed(ActionEvent e) {}
        });
        tb.addSeparator();
        tb.add(new AbstractAction("Save", UIManager.getIcon("FileView.floppyDriveIcon")) {
            public void actionPerformed(ActionEvent e) {}
        });

        p.add(title, BorderLayout.NORTH);
        p.add(new JScrollPane(info), BorderLayout.CENTER);
        p.add(tb, BorderLayout.SOUTH);
        return p;
    }

    private JComponent buildControls() {
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4,4,4,4);
        gc.gridx = 0; gc.gridy = 0; gc.anchor = GridBagConstraints.LINE_END;
        p.add(new JLabel("Text field:"), gc);
        gc.gridx = 1; gc.anchor = GridBagConstraints.LINE_START;
        p.add(new JTextField("Hello Metal", 14), gc);

        gc.gridy++; gc.gridx = 0; gc.anchor = GridBagConstraints.LINE_END;
        p.add(new JLabel("Combo:"), gc);
        gc.gridx = 1; gc.anchor = GridBagConstraints.LINE_START;
        p.add(new JComboBox<>(new String[]{"Alpha","Beta","Gamma"}), gc);

        gc.gridy++; gc.gridx = 0; p.add(new JLabel("Check:"), gc);
        gc.gridx = 1; p.add(new JCheckBox("Enable feature", true), gc);

        gc.gridy++; gc.gridx = 0; p.add(new JLabel("Radio:"), gc);
        gc.gridx = 1;
        ButtonGroup g = new ButtonGroup();
        JRadioButton r1 = new JRadioButton("Small");
        JRadioButton r2 = new JRadioButton("Medium", true);
        JRadioButton r3 = new JRadioButton("Large");
        g.add(r1); g.add(r2); g.add(r3);
        JPanel radios = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        radios.add(r1); radios.add(r2); radios.add(r3);
        p.add(radios, gc);

        gc.gridy++; gc.gridx = 0; p.add(new JLabel("Buttons:"), gc);
        gc.gridx = 1;
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        btns.add(new JButton("Primary"));
        btns.add(new JButton("Secondary"));
        JButton danger = new JButton("Danger");
        danger.setForeground(new Color(180, 0, 0));
        btns.add(danger);
        p.add(btns, gc);

        gc.gridy++; gc.gridx = 0; p.add(new JLabel("List:"), gc);
        gc.gridx = 1;
        JList<String> list = new JList<>(new String[]{"One","Two","Three","Four"});
        list.setVisibleRowCount(3);
        p.add(new JScrollPane(list), gc);

        gc.gridy++; gc.gridx = 0; p.add(new JLabel("Table:"), gc);
        gc.gridx = 1;
        JTable table = new JTable(new Object[][]{
                {"A", 1, true}, {"B", 2, false}, {"C", 3, true}
        }, new Object[]{"Col", "Num", "Flag"});
        table.setPreferredScrollableViewportSize(new Dimension(240, 64));
        p.add(new JScrollPane(table), gc);

        return p;
    }

    private JComponent buildSplit() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(new JTextArea("Left pane\n\nResizable...")),
                new JScrollPane(new JTextArea("Right pane\n\nResizable...")));
        split.setDividerLocation(0.5);
        split.setResizeWeight(0.5);
        return split;
    }

    // ---- Scale fonts and some numeric defaults ----
    private static UIDefaults copyAndScaleDefaults(UIDefaults base, float scale) {
        UIDefaults scaled = new UIDefaults();
        for (Object k : ImmutableSet.copyOf(base.keySet())) {
            Object v = base.get(k);
            if (v instanceof Font f) {
                scaled.put(k, f.deriveFont(f.getSize2D() * scale));
            } else if (v instanceof Integer i && isSizeKey(String.valueOf(k))) {
                scaled.put(k, Math.max(1, Math.round(i * scale)));
            } else {
                scaled.put(k, v);
            }
        }
        // A few common keys to ensure readable typography
        Font control = (Font) scaled.get("controlFont");
        if (control != null) scaled.put("Label.font", control);
        return scaled;
    }

    private static boolean isSizeKey(String key) {
        // Heuristic: scale keys that look like sizes/widths/margins
        String k = key.toLowerCase();
        return k.endsWith("size") || k.endsWith("width") || k.endsWith("height")
                || k.endsWith("padding") || k.contains("insets") || k.endsWith("thickness");
    }

    // ---- Optional paint-time scale to simulate different DPIs without relayout ----
    private static final class ScaleLayerUI extends LayerUI<JComponent> {
        private final float scale;
        private ScaleLayerUI(float scale) { this.scale = scale; }
        @Override public void paint(Graphics g, JComponent c) {
            if (scale == 1.0f) { super.paint(g, c); return; }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.scale(scale, scale);
            // Clip to unscaled logical bounds so scrollbars etc. behave
            Shape old = g2.getClip();
            g2.setClip(0, 0, (int)(c.getWidth() / scale), (int)(c.getHeight() / scale));
            super.paint(g2, c);
            g2.setClip(old);
            g2.dispose();
        }
        @Override public Dimension getPreferredSize(JComponent c) {
            Dimension d = super.getPreferredSize(c);
            return new Dimension((int)(d.width * scale), (int)(d.height * scale));
        }
    }

    // --- Convenience demo frame (optional) ---
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Preview Sandbox");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            // Example with a custom Metal theme
            MetalTheme theme = new DefaultMetalTheme() {
                private final ColorUIResource primary1 = new ColorUIResource(0x355D8A);
                private final ColorUIResource primary2 = new ColorUIResource(0x6A8DB8);
                private final ColorUIResource primary3 = new ColorUIResource(0xC7D7EA);
                @Override protected ColorUIResource getPrimary1() { return primary1; }
                @Override protected ColorUIResource getPrimary2() { return primary2; }
                @Override protected ColorUIResource getPrimary3() { return primary3; }
                @Override public String getName() { return "Custom Blue"; }
            };

            JPanel grid = new JPanel(new GridLayout(1, 3, 12, 12));
            grid.add(ThemePreviewPanel.create(new Config(1.0f, theme, false)));
            grid.add(ThemePreviewPanel.create(new Config(1.25f, theme, false)));
            grid.add(ThemePreviewPanel.create(new Config(1.5f, theme, true))); // also paint-scale

            grid.setBorder(new EmptyBorder(12,12,12,12));
            f.setContentPane(grid);
            f.pack();
            f.setLocationByPlatform(true);
            f.setVisible(true);
        });
    }
}
