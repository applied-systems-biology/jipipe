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

package org.hkijena.jipipe.desktop.commons.theme;

import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.CheckBoxIcon;
import org.hkijena.jipipe.utils.ui.CheckBoxMenuItemIcon;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class JIPipeDesktopModernMetalTheme extends DefaultMetalTheme {
    public static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    public static final ColorUIResource PRIMARY1 =
            new ColorUIResource(0x8EBFEF); // Progress bar text, focus
    public static final ColorUIResource PRIMARY2 =
            new ColorUIResource(0xe1e1e1);
    public static final ColorUIResource PRIMARY3 =
            new ColorUIResource(0xd5d5d5); // Scroll bar pattern here
    public static final ColorUIResource SECONDARY1 =
            new ColorUIResource(0xd5d5d5);
    public static final ColorUIResource SECONDARY2 =
            new ColorUIResource(0xd5d5d5);
    public static final ColorUIResource SECONDARY3 =
            new ColorUIResource(0xF7F8FA);
    public static final ColorUIResource PRIMARY4 =
            new ColorUIResource(0xF7F8FA);
    public static final ColorUIResource PRIMARY5 =
            new ColorUIResource(0x5d85ae);
    public static final ColorUIResource CONTROL_TOGGLED =
            new ColorUIResource(0xDEE0E4);
    public static final ColorUIResource CONTROL_HIGHLIGHTED =
            new ColorUIResource(0xE0EDFA);

    public static final ColorUIResource PRIMARY6 =
            new ColorUIResource(0xaa87de);
    public static final Color DARK_GRAY = new ColorUIResource(Color.DARK_GRAY);
    public static final Color MEDIUM_GRAY = new ColorUIResource(0xDEE0E4);
    public static final Color GRAY = new ColorUIResource(0xe6e6e6);
    public static final Color LIGHT_GRAY = new ColorUIResource(0xF7F8FA);
    public static final ColorUIResource CONTROL_TEXT_COLOR =
            new ColorUIResource(0x1d1d1d);
    public static final ColorUIResource INACTIVE_CONTROL_TEXT_COLOR =
            new ColorUIResource(0x999999);
    public static final ColorUIResource MENU_DISABLED_FOREGROUND =
            new ColorUIResource(0x999999);
    public static final ColorUIResource OCEAN_BLACK =
            new ColorUIResource(0x1d1d1d);
    public static final ColorUIResource OCEAN_DROP =
            new ColorUIResource(0xD2E9FF);
    private static final Border NO_BORDER = BorderFactory.createEmptyBorder();
    private static final Border BUTTON_BORDER = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
            BorderFactory.createCompoundBorder(new RoundedLineBorder(MEDIUM_GRAY, 1, 5),
                    BorderFactory.createEmptyBorder(3, 3, 3, 3)));

    private static final Border TEXTFIELD_BORDER = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
            new RoundedLineBorder(MEDIUM_GRAY, 1, 5));
    private static final Border INVISIBLE_BORDER = BorderFactory.createEmptyBorder(5, 5, 5, 5);
    static ColorUIResource WHITE = new ColorUIResource(255, 255, 255);

    /**
     * Creates an instance of {@code OceanTheme}
     */
    public JIPipeDesktopModernMetalTheme() {
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
        Object fileIcon = UIUtils.getIconFromResources("mimetypes/gtk-file.png");
        List<Object> sliderGradient = Arrays.asList(new Object[]{
                .3f, .2f,
                PRIMARY4, getWhite(), new ColorUIResource(SECONDARY2)});

        final Font defaultFont = new Font(Font.DIALOG, Font.PLAIN, 12);

        Object[] defaults = new Object[]{
                // Since new Fiji version
                "Button.font", defaultFont,
                "ToggleButton.font", defaultFont,
                "CheckBox.font", defaultFont,
                "Label.font", defaultFont,
                "List.font", defaultFont,
                "Panel.font", defaultFont,
                "ScrollPane.font", defaultFont,
                "TabbedPane.font", defaultFont,

                "Button.background", LIGHT_GRAY,
                "Button.rollover", Boolean.TRUE,
                "Button.toolBarBorderBackground", INACTIVE_CONTROL_TEXT_COLOR,
                "Button.disabledToolBarBorderBackground", GRAY,
                "Button.rolloverIconType", "ocean",
                "Button.border", BUTTON_BORDER,
                "Button.borderColor", MEDIUM_GRAY,
                "Button.focus", CONTROL_TOGGLED,
                "ScrollPane.border", NO_BORDER,

                "Separator.foreground", GRAY,
                "Separator.background", PRIMARY4,
                "Separator.highlight", PRIMARY4,
                "Separator.shadow", false,

                "CheckBox.rollover", Boolean.TRUE,
                "CheckBoxMenuItem.background", WHITE,
                "CheckBox.icon", new CheckBoxIcon(WHITE),

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
                "Menu.background", Color.WHITE,
                "MenuItem.acceleratorForeground", DARK_GRAY,

                "PopupMenu.border", new RoundedLineBorder(MEDIUM_GRAY, 1, 5),
                "PopupMenu.background", Color.WHITE,
                "MenuItem.background", Color.WHITE,

                "Menu.border", BorderFactory.createEmptyBorder(5, 5, 5, 5),
                "MenuItem.border", BorderFactory.createEmptyBorder(5, 5, 5, 5),
                "CheckBoxMenuItem.border", BorderFactory.createEmptyBorder(5, 5, 5, 5),
                "MenuItem.borderPainted", false,
                "CheckBoxMenuItem.borderPainted", false,
                "Menu.borderPainted", false,

                "MenuBar.background", LIGHT_GRAY,
                "MenuBar.borderColor", GRAY,
                "MenuItem.selectionBackground", CONTROL_HIGHLIGHTED,
                "MenuItem.selectionForeground", CONTROL_TEXT_COLOR,

                "CheckBoxMenuItem.background", Color.WHITE,
                "CheckBoxMenuItem.selectionBackground", CONTROL_HIGHLIGHTED,
                "CheckBoxMenuItem.selectionForeground", CONTROL_TEXT_COLOR,

                "CheckBoxMenuItem.checkIcon", new CheckBoxMenuItemIcon(WHITE),
                "Menu.selectionBackground", CONTROL_HIGHLIGHTED,
                "Menu.selectionForeground", CONTROL_TEXT_COLOR,


                "InternalFrame.activeTitleBackground", LIGHT_GRAY,
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
                UIUtils.getIconFromResources("actions/hamburger-menu.png"),
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

                "RadioButton.background", LIGHT_GRAY,
                "RadioButton.rollover", Boolean.TRUE,

                "Spinner.arrowButtonBorder", BorderFactory.createMatteBorder(0, 1, 0, 0, GRAY),
                "Spinner.arrowButtonInsets", new Insets(2, 2, 2, 2),
                "Spinner.arrowButtonSize", new Dimension(16, 16),
                "Spinner.border", BUTTON_BORDER,
                "Spinner.background", WHITE,

                "RadioButtonMenuItem.background", LIGHT_GRAY,

                "ScrollBar.background", GRAY,
                "ScrollBar.thumbHighlight", TRANSPARENT,
                "ScrollBar.thumbShadow", TRANSPARENT,
                "ScrollBar.trackHighlight", TRANSPARENT,
                "ScrollBar.thumb", MEDIUM_GRAY,
                "ScrollBar.width", 12,

                "Slider.altTrackColor", new ColorUIResource(0xD2E2EF),
                "Slider.gradient", sliderGradient,
                "Slider.focusGradient", sliderGradient,

                "SplitPane.oneTouchButtonsOpaque", Boolean.FALSE,
                "SplitPane.dividerFocusColor", PRIMARY4,
                "SplitPane.border", BorderFactory.createEmptyBorder(),

                "TabbedPane.borderHightlightColor", LIGHT_GRAY,
                "TabbedPane.contentAreaColor", PRIMARY4,
                "TabbedPane.contentBorderInsets", new Insets(2, 2, 3, 3),
                "TabbedPane.selected", WHITE,
                "TabbedPane.tabAreaBackground", LIGHT_GRAY,
                "TabbedPane.tabAreaInsets", new Insets(2, 2, 0, 6),
                "TabbedPane.unselectedBackground", SECONDARY3,

                "Table.focusCellHighlightBorder", focusBorder,
                "Table.gridColor", SECONDARY1,
                "TableHeader.focusCellBackground", PRIMARY4,

                "ToggleButton.background", LIGHT_GRAY,
                "ToggleButton.border", BUTTON_BORDER,
                "ToggleButton.select", CONTROL_TOGGLED,

                "TextField.inactiveForeground", PRIMARY1,
                "TextField.border", TEXTFIELD_BORDER,

//                "ToolBar.borderColor", GRAY2,
                "ToolBar.border", BorderFactory.createMatteBorder(0, 0, 1, 0, GRAY),
                "ToolBar.isRollover", Boolean.TRUE,

                "ToolTip.background", WHITE,
                "ToolTip.backgroundInactive", WHITE,
                "ToolTip.border", BorderFactory.createLineBorder(DARK_GRAY),
                "ToolTip.borderInactive", BorderFactory.createLineBorder(DARK_GRAY),
                "ToolTip.foreground", Color.BLACK,
                "ToolTip.foregroundInactive", Color.BLACK,

                "Tree.closedIcon", directoryIcon,

                "Tree.collapsedIcon", UIUtils.getIcon8FromResources("tree-expand.png"),
                "Tree.expandedIcon", UIUtils.getIcon8FromResources("tree-shrink.png"),
                "Tree.leafIcon", fileIcon,
                "Tree.openIcon", directoryIcon,
                "Tree.selectionBorderColor", getPrimary1(),
                "Tree.dropLineColor", getPrimary1(),
                "Table.dropLineColor", getPrimary1(),
                "Table.dropLineShortColor", OCEAN_BLACK,

                "Table.dropCellBackground", OCEAN_DROP,
                "Tree.dropCellBackground", OCEAN_DROP,
                "List.dropCellBackground", OCEAN_DROP,
                "List.dropLineColor", getPrimary1(),

                "List.selectionBackground", new Color(0xE0EDFA),

                "ProgressBar.background", WHITE,
                "ProgressBar.foreground", PRIMARY5,
                "ProgressBar.border", new RoundedLineBorder(MEDIUM_GRAY, 1, 2)
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
        return WHITE;
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
