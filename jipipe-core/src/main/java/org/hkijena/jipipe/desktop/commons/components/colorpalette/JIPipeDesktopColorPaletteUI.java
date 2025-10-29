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

package org.hkijena.jipipe.desktop.commons.components.colorpalette;

import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A component that allows the user to pick foreground/background color. Supports transparency
 */
public class JIPipeDesktopColorPaletteUI extends JPanel {
    /**
     * No flags, i.e. foreground + background color and no alpha
     */
    public static final int NONE = 0;
    /**
     * Disable background color selection (will be black)
     */
    public static final int NO_BACKGROUND = 1;
    /**
     * Enable alpha selection
     */
    public static final int WITH_ALPHA = 2;

    private final boolean enableBackgroundColorSelection;
    private final boolean enableAlphaColorSelection;
    private final List<JIPipeDesktopColorPaletteColor> defaultColors;
    private List<JIPipeDesktopColorPaletteColor> userColors = new ArrayList<>();
    private final JPanel defaultColorsPanel = new JPanel();
    private final JPanel userColorsPanel = new JPanel();
    private JIPipeDesktopColorPaletteColor selectedColor;

    public JIPipeDesktopColorPaletteUI() {
        this(NONE, JIPipeDesktopColorPalette.PASTEL);
    }

    public JIPipeDesktopColorPaletteUI(int flags, JIPipeDesktopColorPaletteColor... defaultColors) {
        this.enableAlphaColorSelection = (flags & WITH_ALPHA) == WITH_ALPHA;
        this.enableBackgroundColorSelection = (flags & NO_BACKGROUND) == NO_BACKGROUND;
        this.defaultColors = new ArrayList<>();
        for (JIPipeDesktopColorPaletteColor color : defaultColors) {
            this.defaultColors.add(new JIPipeDesktopColorPaletteColor(color));
        }
        initialize();
        rebuild();
        setSelectedColor(defaultColors[0]);
    }

    private void initialize() {
        defaultColorsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        userColorsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        setLayout(new BorderLayout());
        add(UIUtils.boxVertical(defaultColorsPanel, userColorsPanel), BorderLayout.CENTER);
    }

    public void rebuild() {
        defaultColorsPanel.removeAll();
        for (JIPipeDesktopColorPaletteColor color : defaultColors) {
            addSelectableColor(defaultColorsPanel, color);
        }
    }

    private void addSelectableColor(JPanel target, JIPipeDesktopColorPaletteColor color) {
        JButton button = new JButton();
        button.setIcon(new JIPipeDesktopColorPaletteColorIcon(32,color, this));
        button.setBorder(null);
        button.setOpaque(false);
        button.addActionListener(e -> {
            setSelectedColor(color);
        });
        target.add(button);
    }

    public List<JIPipeDesktopColorPaletteColor> getUserColors() {
        return userColors;
    }

    public void setUserColors(List<JIPipeDesktopColorPaletteColor> userColors) {
        this.userColors = userColors;
        rebuild();
    }

    public static void main(String[] args) throws UnsupportedLookAndFeelException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        ThemeUtils.applyThemeFromSettings();
        JFrame frame = new JFrame("JIPipeDesktopColorPaletteUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JIPipeDesktopColorPaletteUI ui = new JIPipeDesktopColorPaletteUI();
        frame.setContentPane(ui);
        frame.pack();
        frame.setSize(640,480);
        frame.setVisible(true);
    }

    public JIPipeDesktopColorPaletteColor getSelectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(JIPipeDesktopColorPaletteColor selectedColor) {
        this.selectedColor = selectedColor;
        repaint(50);
    }
}
