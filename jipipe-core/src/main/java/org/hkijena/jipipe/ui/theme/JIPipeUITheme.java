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

package org.hkijena.jipipe.ui.theme;

import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;

public enum JIPipeUITheme {
    Native("Native", false, false),
    Metal("Metal", false, false),
    ModernLight("Modern light", true, false),
    ModernDark("Modern dark", true, true);

    private static boolean INSTALLED_LISTENER;
    private static boolean IS_UPDATING_THEME;
    private final String name;
    private final boolean isDark;
    private final boolean isModern;

    JIPipeUITheme(String name, boolean isModern, boolean isDark) {
        this.name = name;
        this.isModern = isModern;
        this.isDark = isDark;
    }

    public String getName() {
        return name;
    }

    public boolean isDark() {
        return isDark;
    }

    /**
     * Activates the theme.
     * Should be done before opening any UI elements
     */
    public void install() {
        UIUtils.DARK_THEME = isDark;
        IS_UPDATING_THEME = true;
        switch (this) {
            case Metal:
                try {
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                    UIManager.put("swing.boldMetal", Boolean.FALSE);
                    UIManager.put("Button.borderColor", ModernMetalTheme.MEDIUM_GRAY);
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                         UnsupportedLookAndFeelException e) {
                    e.printStackTrace();
                }
                break;
            case ModernLight:
                try {
                    MetalLookAndFeel.setCurrentTheme(new ModernMetalTheme());
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                    UIManager.put("swing.boldMetal", Boolean.FALSE);
                    UIManager.put("ScrollBarUI", ArrowLessScrollBarUI.class.getName());
                    UIManager.put("SliderUI", ModernSliderUI.class.getName());
                    UIManager.put("SpinnerUI", ModernSpinnerUI.class.getName());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                         UnsupportedLookAndFeelException e) {
                    e.printStackTrace();
                }
                break;
            case ModernDark:
                try {
                    MetalLookAndFeel.setCurrentTheme(new DarkModernMetalTheme());
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                    UIManager.put("swing.boldMetal", Boolean.FALSE);
                    UIManager.put("ScrollBarUI", ArrowLessScrollBarUI.class.getName());
                    UIManager.put("SliderUI", ModernSliderUI.class.getName());
                    UIManager.put("SpinnerUI", ModernSpinnerUI.class.getName());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                         UnsupportedLookAndFeelException e) {
                    e.printStackTrace();
                }
                break;
            default:
                UIManager.put("Button.borderColor", ModernMetalTheme.MEDIUM_GRAY);
                break;
        }
        IS_UPDATING_THEME = false;

        // Prevent external theme changes
        if (!INSTALLED_LISTENER && this != Native) {
            UIManager.addPropertyChangeListener(evt -> {
                if ("lookAndFeel".equals(evt.getPropertyName())) {
                    if (!IS_UPDATING_THEME) {
                        install();
                    }
                }
            });
            INSTALLED_LISTENER = true;
        }
    }

    public boolean isModern() {
        return isModern;
    }


    @Override
    public String toString() {
        return name;
    }
}
