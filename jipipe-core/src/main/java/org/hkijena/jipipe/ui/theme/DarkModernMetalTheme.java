/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.theme;

import org.hkijena.jipipe.utils.CheckBoxIcon;
import org.hkijena.jipipe.utils.CheckBoxMenuItemIcon;
import org.hkijena.jipipe.utils.RoundedLineBorder;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.Arrays;
import java.util.List;

public class DarkModernMetalTheme extends DefaultMetalTheme {
    public static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    public static final ColorUIResource PRIMARY1 =
            new ColorUIResource(0x65a4e3); // Progress bar text, focus
    public static final ColorUIResource PRIMARY2 =
            new ColorUIResource(0x444444);
    public static final ColorUIResource PRIMARY3 =
            new ColorUIResource(0x7777777); // Scroll bar pattern here
    public static final ColorUIResource SECONDARY1 =
            new ColorUIResource(0x323232);
    public static final ColorUIResource SECONDARY2 =
            new ColorUIResource(0x313131);
    public static final ColorUIResource SECONDARY3 =
            new ColorUIResource(0x333333);
    public static final ColorUIResource PRIMARY4 =
            new ColorUIResource(0x2f2f2f);
    public static final ColorUIResource PRIMARY5 =
            new ColorUIResource(0x65a4e3);
    public static final ColorUIResource PRIMARY6 =
            new ColorUIResource(0xaa87de);
    public static final Color DARK_GRAY = new ColorUIResource(0x2f2f2f);
    public static final Color GRAY = new ColorUIResource(0x212121);
    public static final ColorUIResource CONTROL_TEXT_COLOR =
            new ColorUIResource(0xeeeeee);
    public static final ColorUIResource INACTIVE_CONTROL_TEXT_COLOR =
            new ColorUIResource(0x999999);
    public static final ColorUIResource MENU_DISABLED_FOREGROUND =
            new ColorUIResource(0x999999);
    public static final ColorUIResource OCEAN_BLACK =
            new ColorUIResource(0xeeeeee);
    private static final Border NO_BORDER = BorderFactory.createEmptyBorder();
    private static final Border BUTTON_BORDER = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
            BorderFactory.createCompoundBorder(new RoundedLineBorder(new Color(0x5f6265), 1, 2),
                    BorderFactory.createEmptyBorder(3, 3, 3, 3)));
    private static final Border INVISIBLE_BORDER = BorderFactory.createEmptyBorder(5, 5, 5, 5);
    static ColorUIResource BLACK = new ColorUIResource(0x222222);

    /**
     * Creates an instance of <code>OceanTheme</code>
     */
    public DarkModernMetalTheme() {
    }

    /**
     * Add this theme's custom entries to the defaults table.
     *
     * @param table the defaults table, non-null
     * @throws NullPointerException if {@code table} is {@code null}
     */
    public void addCustomEntriesToTable(UIDefaults table) {
        Object focusBorder = new BorderUIResource.LineBorderUIResource(getPrimary1());
        Object directoryIcon = UIUtils.getIconFromResources("places/folder-blue.png");
        Object fileIcon = UIUtils.getIconFromResources("mimetypes/application-x-kgeo.png");
        List<Object> sliderGradient = Arrays.asList(new Object[]{
                .3f, .2f,
                PRIMARY4, getWhite(), new ColorUIResource(SECONDARY2)});

        Object[] defaults = new Object[]{
                "Button.background", new Color(0x30353a),
                "Button.rollover", Boolean.TRUE,
                "Button.toolBarBorderBackground", new Color(0x5f6265),
                "Button.disabledToolBarBorderBackground", new Color(0x5f6265),
                "Button.rolloverIconType", "ocean",
                "Button.border", BUTTON_BORDER,
                "Button.focus", new Color(0x65a4e3),
                "ScrollPane.border", BorderFactory.createEmptyBorder(),
                "Button.borderColor", new Color(0x5f6265),

                "ComboBox.background", new Color(0x1b1e20),
                "ComboBox.buttonBackground", new Color(0x1b1e20),
                "ComboBox.buttonHighlight", new Color(0x1b1e20),
                "ComboBox.buttonDarkShadow", new Color(0x1b1e20),
                "ComboBox.disabledBackground", new Color(0x1b1e20),

                "Separator.foreground", new Color(0x5f6265),
                "Separator.background", new Color(0x5f6265),
                "Separator.highlight", new Color(0x5f6265),
                "Separator.shadow", false,

                "controlHighlight", new Color(0x5f6265),

                "CheckBox.rollover", Boolean.TRUE,
                "CheckBox.icon", new CheckBoxIcon(new Color(0x2a2e32)),

                // home2
                "FileChooser.homeFolderIcon",
                UIUtils.getIconFromResources("actions/go-home.png"),
                // directory2
                "FileChooser.newFolderIcon",
                UIUtils.getIconFromResources("actions/folder-new.png"),
                // updir2
                "FileChooser.upFolderIcon",
                UIUtils.getIconFromResources("actions/go-parent-folder.png"),

                // computer2
                "FileView.computerIcon",
                UIUtils.getIconFromResources("devices/computer.png"),
                "FileView.directoryIcon", directoryIcon,
                // disk2
                "FileView.hardDriveIcon",
                UIUtils.getIconFromResources("devices/drive-harddisk.png"),
                "FileView.fileIcon", fileIcon,
                // floppy2
                "FileView.floppyDriveIcon",
                UIUtils.getIconFromResources("devices/media-floppy.png"),

                "Label.disabledForeground", getInactiveControlTextColor(),

                "Menu.opaque", Boolean.FALSE,
                "MenuItem.acceleratorForeground", new Color(0xaa87de),

                "PopupMenu.border", BorderFactory.createLineBorder(new Color(0x5f6265), 1, true),

                "MenuBar.background", new Color(0x31363b),
                "MenuBar.borderColor", new Color(0x5f6265),
                "CheckBoxMenuItem.background", new Color(0x2a2e32),
                "CheckBoxMenuItem.selectionBackground", PRIMARY5,
                "CheckBoxMenuItem.selectionForeground", new Color(0xfcfcfc),
                "CheckBoxMenuItem.borderPainted", false,
                "CheckBoxMenuItem.checkIcon", new CheckBoxMenuItemIcon(new Color(0xfcfcfc)),
                "MenuItem.selectionBackground", new Color(0x65a4e3),
                "MenuItem.selectionForeground", new Color(0xfcfcfc),
                "MenuItem.borderPainted", false,
                "Menu.selectionBackground", new Color(0x65a4e3),
                "Menu.selectionForeground", new Color(0xfcfcfc),
                "Menu.borderPainted", false,

                "InternalFrame.activeTitleBackground", new Color(0x31363b),
                // close2
                "InternalFrame.closeIcon",
                UIUtils.getIconFromResources("actions/close-tab.png"),
                // minimize
                "InternalFrame.iconifyIcon",
                UIUtils.getIconFromResources("actions/xfce-wm-minimize.png"),
                // restore
                "InternalFrame.minimizeIcon",
                UIUtils.getIconFromResources("actions/xfce-wm-minimize.png"),
                // menubutton3
                "InternalFrame.icon",
                UIUtils.getIconFromResources("actions/open-menu.png"),
                // maximize2
                "InternalFrame.maximizeIcon",
                UIUtils.getIconFromResources("actions/xfce-wm-maximize.png"),
                // paletteclose
                "InternalFrame.paletteCloseIcon",
                UIUtils.getIconFromResources("actions/close-tab.png"),

                "List.focusCellHighlightBorder", focusBorder,

                "MenuBarUI", "javax.swing.plaf.metal.MetalMenuBarUI",

                "OptionPane.errorIcon",
                UIUtils.getIcon32FromResources("dialog-error.png"),
                "OptionPane.informationIcon",
                UIUtils.getIcon32FromResources("dialog-info.png"),
                "OptionPane.questionIcon",
                UIUtils.getIcon32FromResources("dialog-question.png"),
                "OptionPane.warningIcon",
                UIUtils.getIcon32FromResources("dialog-warning.png"),

                "RadioButton.background", new Color(0x2a2e32),
                "RadioButton.rollover", Boolean.TRUE,

                "Spinner.arrowButtonBorder", BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(0x5f6265)),
                "Spinner.arrowButtonInsets", new Insets(2, 2, 2, 2),
                "Spinner.arrowButtonSize", new Dimension(16, 16),
                "Spinner.background", new Color(0x1b1e20),
                "Spinner.border", INVISIBLE_BORDER,

                "RadioButtonMenuItem.background", new Color(0x26292d),

                "ScrollBar.background", new Color(0x1b1e20),
                "ScrollBar.thumbHighlight", TRANSPARENT,
                "ScrollBar.thumbShadow", TRANSPARENT,
                "ScrollBar.trackHighlight", TRANSPARENT,
                "ScrollBar.thumb", new Color(0x6f7275),
                "ScrollBar.width", 12,

                "Slider.altTrackColor", new ColorUIResource(0xD2E2EF),
                "Slider.gradient", sliderGradient,
                "Slider.focusGradient", sliderGradient,

                "SplitPane.oneTouchButtonsOpaque", Boolean.FALSE,
                "SplitPane.dividerFocusColor", new Color(0x2a2e32),
                "SplitPane.border", BorderFactory.createEmptyBorder(),
                "SplitPane.highlight", new Color(0x2a2e32),

                "TabbedPane.highlight", new Color(0x26292c),
                "TabbedPane.light", new Color(0x26292c),
                "TabbedPane.selectHighlight", new Color(0x26292d),
                "TabbedPane.borderHightlightColor", new Color(0x5f6265),
                "TabbedPane.contentAreaColor", new Color(0x26292d),
                "TabbedPane.contentBorderInsets", new Insets(2, 2, 3, 3),
                "TabbedPane.selected", new Color(0x26292d),
                "TabbedPane.tabAreaBackground", new Color(0x26292d),
                "TabbedPane.tabAreaInsets", new Insets(2, 2, 0, 6),
                "TabbedPane.unselectedBackground", new Color(0x26292c),

                "Table.focusCellHighlightBorder", focusBorder,
                "Table.gridColor", new Color(0x1b1e20),
                "TableHeader.focusCellBackground", new Color(0x1b1e20),
                "TableHeader.cellBorder", new Color(0x1b1e20),

                "ToggleButton.background", new Color(0x31363b),
                "ToggleButton.border", BUTTON_BORDER,
                "ToggleButton.select", new Color(0x65a4e3),

                "TextField.inactiveForeground", new Color(0xD2E2EF),
                "TextField.background", new Color(0x1b1e20),
                "TextField.border", NO_BORDER,

                "TextArea.background", new Color(0x1b1e20),

                "TextPane.background", new Color(0x1b1e20),
                "TextPane.foreground", new Color(0xfcfcfc),

                "PasswordField.background", new Color(0x1b1e20),
                "PasswordField.foreground", new Color(0xfcfcfc),

                "ToolBar.border", BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x5f6265)),
                "ToolBar.isRollover", Boolean.TRUE,

                "ToolTip.background", new Color(0x31363b),
                "ToolTip.backgroundInactive", new Color(0x31363b),
                "ToolTip.border", new RoundedLineBorder(new Color(0xfcfcfc), 1, 3),
                "ToolTip.borderInactive", new RoundedLineBorder(new Color(0xfcfcfc), 1, 3),
                "ToolTip.foreground", new Color(0xfcfcfc),
                "ToolTip.foregroundInactive", new Color(0xfcfcfc),

                "Tree.closedIcon", directoryIcon,

                "Tree.background", new Color(0x1b1e20),
                "Tree.collapsedIcon", UIUtils.getIcon8FromResources("tree-expand.png"),
                "Tree.expandedIcon", UIUtils.getIcon8FromResources("tree-shrink.png"),
                "Tree.leafIcon", fileIcon,
                "Tree.openIcon", directoryIcon,
                "Tree.selectionBorderColor", new Color(0x65a4e3),
                "Tree.dropLineColor", new Color(0x65a4e3),
                "Table.dropLineColor", new Color(0x65a4e3),
                "Table.dropLineShortColor", new Color(0x1b1e20),
                "Table.background", new Color(0x1b1e20),
                "TableHeader.background", new Color(0x1b1e20),

                "Table.dropCellBackground", new Color(0x1b1e20),
                "Tree.dropCellBackground", new Color(0x1b1e20),
                "List.dropCellBackground", new Color(0x1b1e20),
                "List.dropLineColor", new Color(0x65a4e3),

                "ProgressBar.background", new Color(0xfcfcfc),
                "ProgressBar.foreground", new Color(0x65a4e3),
                "ProgressBar.border", new RoundedLineBorder(new Color(0x5f6265), 1, 2),

                "List.background", new Color(0x1b1e20),
                "List.selectionBackground", new Color(0x65a4e3),
                "EditorPane.background", new Color(0x1b1e20),
                "FormattedTextField.background", new Color(0x1b1e20),

                "TabbedPane.tabAreaBackground", new Color(0x1b1e20),
                "Panel.background", new Color(0x2a2e32)
        };
        table.putDefaults(defaults);
    }

    /**
     * Overriden to enable picking up the system fonts, if applicable.
     */
    boolean isSystemTheme() {
        return true;
    }

    /**
     * Return the name of this theme, "Ocean".
     *
     * @return "Ocean"
     */
    public String getName() {
        return "Ocean";
    }

    /**
     * Returns the primary 1 color. This returns a color with an rgb hex value
     * of {@code 0x6382BF}.
     *
     * @return the primary 1 color
     * @see Color#decode
     */
    protected ColorUIResource getPrimary1() {
        return PRIMARY1;
    }

    /**
     * Returns the primary 2 color. This returns a color with an rgb hex value
     * of {@code 0xA3B8CC}.
     *
     * @return the primary 2 color
     * @see Color#decode
     */
    protected ColorUIResource getPrimary2() {
        return PRIMARY2;
    }

    /**
     * Returns the primary 3 color. This returns a color with an rgb hex value
     * of {@code 0xB8CFE5}.
     *
     * @return the primary 3 color
     * @see Color#decode
     */
    protected ColorUIResource getPrimary3() {
        return PRIMARY3;
    }

    /**
     * Returns the secondary 1 color. This returns a color with an rgb hex
     * value of {@code 0x7A8A99}.
     *
     * @return the secondary 1 color
     * @see Color#decode
     */
    protected ColorUIResource getSecondary1() {
        return SECONDARY1;
    }

    /**
     * Returns the secondary 2 color. This returns a color with an rgb hex
     * value of {@code 0xB8CFE5}.
     *
     * @return the secondary 2 color
     * @see Color#decode
     */
    protected ColorUIResource getSecondary2() {
        return SECONDARY2;
    }

    /**
     * Returns the secondary 3 color. This returns a color with an rgb hex
     * value of {@code 0xEEEEEE}.
     *
     * @return the secondary 3 color
     * @see Color#decode
     */
    protected ColorUIResource getSecondary3() {
        return SECONDARY3;
    }

    /**
     * Returns the black color. This returns a color with an rgb hex
     * value of {@code 0x333333}.
     *
     * @return the black color
     * @see Color#decode
     */
    protected ColorUIResource getBlack() {
        return OCEAN_BLACK;
    }

    /**
     * Returns the desktop color. This returns a color with an rgb hex
     * value of {@code 0xFFFFFF}.
     *
     * @return the desktop color
     * @see Color#decode
     */
    public ColorUIResource getDesktopColor() {
        return BLACK;
    }

    /**
     * Returns the inactive control text color. This returns a color with an
     * rgb hex value of {@code 0x999999}.
     *
     * @return the inactive control text color
     */
    public ColorUIResource getInactiveControlTextColor() {
        return INACTIVE_CONTROL_TEXT_COLOR;
    }

    /**
     * Returns the control text color. This returns a color with an
     * rgb hex value of {@code 0x333333}.
     *
     * @return the control text color
     */
    public ColorUIResource getControlTextColor() {
        return CONTROL_TEXT_COLOR;
    }

    /**
     * Returns the menu disabled foreground color. This returns a color with an
     * rgb hex value of {@code 0x999999}.
     *
     * @return the menu disabled foreground color
     */
    public ColorUIResource getMenuDisabledForeground() {
        return MENU_DISABLED_FOREGROUND;
    }

}
