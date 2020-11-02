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
import org.hkijena.jipipe.utils.RoundedLineBorder;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import java.awt.*;
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
            new ColorUIResource(0x2f2f2f);
    public static final ColorUIResource PRIMARY4 =
            new ColorUIResource(0x2f2f2f);
    public static final ColorUIResource PRIMARY5 =
            new ColorUIResource(0x65a4e3);
    public static final ColorUIResource PRIMARY6 =
            new ColorUIResource(0xaa87de);
    public static final Color DARK_GRAY = new ColorUIResource(0x2f2f2f);
    public static final Color MEDIUM_GRAY = new ColorUIResource(0x333333);
    public static final Color GRAY = new ColorUIResource(0x212121);
    public static final Color LIGHT_GRAY = new ColorUIResource(0x2f2f2f);
    public static final Color TEXTAREA_BACKGROUND = new ColorUIResource(0x121212);
    public static final Color SUBCONTROL_BACKGROUND = new ColorUIResource(0x121212);
    public static final ColorUIResource CONTROL_TEXT_COLOR =
            new ColorUIResource(0xeeeeee);
    public static final ColorUIResource INACTIVE_CONTROL_TEXT_COLOR =
            new ColorUIResource(0x999999);
    public static final ColorUIResource MENU_DISABLED_FOREGROUND =
            new ColorUIResource(0x999999);
    public static final ColorUIResource OCEAN_BLACK =
            new ColorUIResource(0xeeeeee);
    public static final ColorUIResource OCEAN_DROP =
            new ColorUIResource(0x65a4e3);
    private static final Border NO_BORDER = BorderFactory.createEmptyBorder();
    private static final Border BUTTON_BORDER = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
            BorderFactory.createCompoundBorder(new RoundedLineBorder(PRIMARY3, 1, 2),
                    BorderFactory.createEmptyBorder(3, 3, 3, 3)));
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
        // .30 0 DDE8F3 white secondary2
//        List<Object> buttonGradient = Arrays.asList(
//                new Object[] {.3f, 0f,
//                        new ColorUIResource(0xDDE8F3), getWhite(), getSecondary2() });
//        List<Object> menuBarGradient = Arrays.asList(new Object[] {
//                new Float(1f), new Float(0f),
//                getWhite(), dadada,
//                new ColorUIResource(dadada) });
        // Other possible properties that aren't defined:
        //
        // Used when generating the disabled Icons, provides the region to
        // constrain grays to.
        // Button.disabledGrayRange -> Object[] of Integers giving min/max
        // InternalFrame.inactiveTitleGradient -> Gradient when the
        //   internal frame is inactive.
        Object directoryIcon = UIUtils.getIconFromResources("places/folder-blue.png");
        Object fileIcon = UIUtils.getIconFromResources("mimetypes/application-x-kgeo.png");
        List<Object> sliderGradient = Arrays.asList(new Object[]{
                .3f, .2f,
                PRIMARY4, getWhite(), new ColorUIResource(SECONDARY2)});

        Object[] defaults = new Object[]{
                "Button.background", LIGHT_GRAY,
                "Button.rollover", Boolean.TRUE,
                "Button.toolBarBorderBackground", INACTIVE_CONTROL_TEXT_COLOR,
                "Button.disabledToolBarBorderBackground", GRAY,
                "Button.rolloverIconType", "ocean",
                "Button.border", BUTTON_BORDER,
                "Button.focus", PRIMARY1,
                "ScrollPane.border", NO_BORDER,
                "Button.borderColor", PRIMARY3,

                "Separator.foreground", GRAY,
                "Separator.background", PRIMARY4,
                "Separator.highlight", PRIMARY4,
                "Separator.shadow", false,

                "CheckBox.rollover", Boolean.TRUE,
                "CheckBoxMenuItem.background", BLACK,
                "CheckBox.icon", new CheckBoxIcon(SUBCONTROL_BACKGROUND),

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
                "MenuItem.acceleratorForeground", DARK_GRAY,

                "PopupMenu.border", BorderFactory.createLineBorder(MEDIUM_GRAY, 1, false),

                "MenuBar.background", LIGHT_GRAY,
                "MenuBar.borderColor", GRAY,
                "MenuItem.selectionBackground", PRIMARY5,
                "MenuItem.selectionForeground", BLACK,
                "MenuItem.borderPainted", false,
                "Menu.selectionBackground", PRIMARY5,
                "Menu.selectionForeground", BLACK,
                "Menu.borderPainted", false,

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

                "RadioButton.background", LIGHT_GRAY,
                "RadioButton.rollover", Boolean.TRUE,

                "Spinner.arrowButtonBorder", BorderFactory.createMatteBorder(0, 1, 0, 0, GRAY),
                "Spinner.arrowButtonInsets", new Insets(2, 2, 2, 2),
                "Spinner.arrowButtonSize", new Dimension(16, 16),

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
                "TabbedPane.selected", BLACK,
                "TabbedPane.tabAreaBackground", LIGHT_GRAY,
                "TabbedPane.tabAreaInsets", new Insets(2, 2, 0, 6),
                "TabbedPane.unselectedBackground", SECONDARY3,

                "Table.focusCellHighlightBorder", focusBorder,
                "Table.gridColor", SECONDARY1,
                "TableHeader.focusCellBackground", PRIMARY4,

                "ToggleButton.background", LIGHT_GRAY,
                "ToggleButton.border", BUTTON_BORDER,
                "ToggleButton.select", PRIMARY5,

                "TextField.inactiveForeground", PRIMARY1,
                "TextField.background", TEXTAREA_BACKGROUND,

                "TextArea.background", TEXTAREA_BACKGROUND,

                "TextPane.background", TEXTAREA_BACKGROUND,
                "TextPane.foreground", CONTROL_TEXT_COLOR,

//                "ToolBar.borderColor", GRAY2,
                "ToolBar.border", BorderFactory.createMatteBorder(0, 0, 1, 0, GRAY),
                "ToolBar.isRollover", Boolean.TRUE,

                "ToolTip.background", BLACK,
                "ToolTip.backgroundInactive", BLACK,
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
                "Table.background", SUBCONTROL_BACKGROUND,
                "TableHeader.background", Color.BLACK,

                "Table.dropCellBackground", OCEAN_DROP,
                "Tree.dropCellBackground", OCEAN_DROP,
                "List.dropCellBackground", OCEAN_DROP,
                "List.dropLineColor", getPrimary1(),

                "ProgressBar.background", BLACK,
                "ProgressBar.foreground", PRIMARY5,
                "ProgressBar.border", new RoundedLineBorder(MEDIUM_GRAY, 1, 2),

                "List.background", SUBCONTROL_BACKGROUND,
                "EditorPane.background", SUBCONTROL_BACKGROUND,

                "TabbedPane.tabAreaBackground", SUBCONTROL_BACKGROUND,
                "Panel.background", MEDIUM_GRAY
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
