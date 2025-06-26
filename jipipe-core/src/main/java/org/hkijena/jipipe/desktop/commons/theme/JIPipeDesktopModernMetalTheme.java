package org.hkijena.jipipe.desktop.commons.theme;

import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.CheckBoxIcon;
import org.hkijena.jipipe.utils.ui.CheckBoxMenuItemIcon;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.IconUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class JIPipeDesktopModernMetalTheme extends DefaultMetalTheme {

    private final JIPipeDesktopModernThemeStyle style;

    public JIPipeDesktopModernMetalTheme(JIPipeDesktopModernThemeStyle style) {
        this.style = style;
    }

    // Core LAF overrides
    @Override
    public String getName() {
        return style.getName();
    }

    @Override
    protected ColorUIResource getPrimary1() {
        return toResource(style.getPrimaryColor().darker());
    }

    @Override
    protected ColorUIResource getPrimary2() {
        return toResource(style.getPrimaryColor());
    }

    @Override
    protected ColorUIResource getPrimary3() {
        return toResource(style.getPrimaryColor().brighter());
    }

    @Override
    public ColorUIResource getControl() {
        return toResource(style.getPanelBackground());
    }

    @Override
    public ColorUIResource getControlTextColor() {
        return toResource(style.getTextForeground());
    }

    @Override
    public ColorUIResource getWindowBackground() {
        return toResource(style.getWindowBackground());
    }

    @Override
    public ColorUIResource getWindowTitleBackground() {
        return toResource(style.getPrimaryColor());
    }

    @Override
    public ColorUIResource getWindowTitleForeground() {
        return toResource(style.getSelectionForeground());
    }

    @Override
    public ColorUIResource getFocusColor() {
        return toResource(style.getPrimaryColor());
    }

    @Override
    public ColorUIResource getTextHighlightColor() {
        return toResource(style.getSelectionBackground());
    }

    @Override
    public ColorUIResource getHighlightedTextColor() {
        return toResource(style.getSelectionForeground());
    }

    // Inject additional custom keys
    @Override
    public void addCustomEntriesToTable(UIDefaults table) {
        super.addCustomEntriesToTable(table);

        // Fonts
        final Font defaultFont = new Font(Font.DIALOG, Font.PLAIN, 12);

        // Borders
        final Border buttonBorder = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
                BorderFactory.createCompoundBorder(new RoundedLineBorder(style.getBorderColor(), 1, 5),
                        BorderFactory.createEmptyBorder(3, 3, 3, 3)));
        final Border textFieldBorder = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
                new RoundedLineBorder(style.getBorderColor(), 1, 5));
        final Border noBorder = BorderFactory.createEmptyBorder();

        // Colors
        final Color transparent = new Color(0, 0, 0, 0);

        // Misc
        final Border focusBorder = new BorderUIResource.LineBorderUIResource(getPrimary1());
        final Icon directoryIcon = UIUtils.getIconFromResources("places/folder-blue.png");
        final Icon fileIcon = UIUtils.getIconFromResources("mimetypes/gtk-file.png");
        final List<Object> sliderGradient = Arrays.asList(new Object[]{.3f, .2f, getPrimary3(), getWhite(), new ColorUIResource(getSecondary1())});

        // Setup fonts
        table.put("Button.font", toResource(defaultFont));
        table.put("ToggleButton.font", toResource(defaultFont));
        table.put("CheckBox.font", toResource(defaultFont));
        table.put("Label.font", toResource(defaultFont));
        table.put("List.font", toResource(defaultFont));
        table.put("Panel.font", toResource(defaultFont));
        table.put("ScrollPane.font", toResource(defaultFont));
        table.put("TabbedPane.font", toResource(defaultFont));

        // Panels
        table.put("Panel.background", toResource(style.getPanelBackground()));

        // Customize button style
        table.put("Button.background", toResource(style.getButtonBackground()));
        table.put("Button.rollover", Boolean.TRUE);
        table.put("Button.toolBarBorderBackground", toResource(style.getBorderColor()));
        table.put("Button.disabledToolBarBorderBackground", toResource(style.getBorderColor()));
        table.put("Button.rolloverIconType", "ocean");
        table.put("Button.border", toResource(buttonBorder));
        table.put("Button.borderColor", toResource(style.getBorderColor()));
        table.put("Button.focus", toResource(style.getButtonToggled()));

        // Toggle button
        table.put("ToggleButton.background", toResource(style.getButtonBackground()));
        table.put("ToggleButton.border", toResource(buttonBorder));
        table.put("ToggleButton.select", toResource(style.getButtonToggled()));

        // Separators
        table.put("Separator.foreground", toResource(style.getBorderColor()));
        table.put("Separator.background", toResource(style.getWindowBackground()));
        table.put("Separator.highlight", toResource(style.getWindowBackground()));
        table.put("Separator.shadow", Boolean.FALSE);

        // Checkbox
        table.put("CheckBox.rollover", Boolean.TRUE);
        table.put("CheckBox.icon", toResource(new CheckBoxIcon(style.getTextForegroundInverted())));

        // Swing file chooser icons
        table.put("FileChooser.homeFolderIcon", toResource(UIUtils.getIconFromResources("actions/go-home.png")));
        table.put("FileChooser.newFolderIcon", toResource(UIUtils.getIconFromResources("actions/folder-new.png")));
        table.put("FileChooser.upFolderIcon", toResource(UIUtils.getIconFromResources("actions/go-parent-folder.png")));

        // Swing file view icons
        table.put("FileView.computerIcon", toResource(UIUtils.getIconFromResources("devices/computer.png")));
        table.put("FileView.directoryIcon", toResource(directoryIcon));
        table.put("FileView.hardDriveIcon", toResource(UIUtils.getIconFromResources("devices/drive-harddisk.png")));
        table.put("FileView.fileIcon", toResource(fileIcon));
        table.put("FileView.floppyDriveIcon", toResource(UIUtils.getIconFromResources("devices/media-floppy.png")));

        // Custom label color
        table.put("Label.disabledForeground", getInactiveControlTextColor());

        // Customize menus
        table.put("Menu.opaque", Boolean.FALSE);
        table.put("Menu.background", toResource(style.getMenuBackground()));
        table.put("MenuItem.acceleratorForeground", toResource(style.getTextMuted()));

        table.put("PopupMenu.border", toResource(new RoundedLineBorder(style.getBorderColor(), 1, 5)));
        table.put("PopupMenu.background", toResource(style.getMenuBackground()));
        table.put("MenuItem.background", toResource(style.getMenuBackground()));

        table.put("Menu.border", toResource(BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        table.put("MenuItem.border", toResource(BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        table.put("CheckBoxMenuItem.border", toResource(BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        table.put("MenuItem.borderPainted", Boolean.FALSE);
        table.put("CheckBoxMenuItem.borderPainted", Boolean.FALSE);
        table.put("CheckBoxMenuItem.background", toResource(style.getMenuBackground()));
        table.put("RadioButtonMenuItem.background", toResource(style.getMenuBackground()));
        table.put("Menu.borderPainted", Boolean.FALSE);

        table.put("MenuBarUI", "javax.swing.plaf.metal.MetalMenuBarUI");
        table.put("MenuBar.background", toResource(style.getWindowBackground()));
        table.put("MenuBar.borderColor", toResource(style.getWindowBackground()));
        table.put("MenuItem.selectionBackground", toResource(style.getSelectionBackground()));
        table.put("MenuItem.selectionForeground", toResource(style.getSelectionForeground()));

        table.put("CheckBoxMenuItem.selectionBackground", toResource(style.getSelectionBackground()));
        table.put("CheckBoxMenuItem.selectionForeground", toResource(style.getSelectionForeground()));
        table.put("CheckBoxMenuItem.checkIcon", toResource(new CheckBoxMenuItemIcon(style.getTextForegroundInverted())));
        table.put("Menu.selectionBackground", toResource(style.getSelectionBackground()));
        table.put("Menu.selectionForeground", toResource(style.getSelectionForeground()));

        // Internal frames
        table.put("InternalFrame.activeTitleBackground", toResource(style.getWindowBackground()));
        table.put("InternalFrame.closeIcon", toResource(UIUtils.getIconFromResources("actions/close-tab.png")));
        table.put("InternalFrame.iconifyIcon", toResource(UIUtils.getIconFromResources("actions/xfce-wm-minimize.png")));
        table.put("InternalFrame.minimizeIcon", toResource(UIUtils.getIconFromResources("actions/xfce-wm-minimize.png")));
        table.put("InternalFrame.icon", toResource(UIUtils.getIconFromResources("actions/hamburger-menu.png")));
        table.put("InternalFrame.maximizeIcon", toResource(UIUtils.getIconFromResources("actions/xfce-wm-maximize.png")));
        table.put("InternalFrame.paletteCloseIcon", toResource(UIUtils.getIconFromResources("actions/close-tab.png")));

        table.put("List.focusCellHighlightBorder", toResource(focusBorder));

        // Dialogs
        table.put("OptionPane.errorIcon", toResource(UIUtils.getIcon32FromResources("dialog-error.png")));
        table.put("OptionPane.informationIcon", toResource(UIUtils.getIcon32FromResources("dialog-info.png")));
        table.put("OptionPane.questionIcon", toResource(UIUtils.getIcon32FromResources("dialog-question.png")));
        table.put("OptionPane.warningIcon", toResource(UIUtils.getIcon32FromResources("dialog-warning.png")));

        // Radio button
        table.put("RadioButton.background", toResource(style.getPanelBackground()));
        table.put("RadioButton.rollover", Boolean.TRUE);

        // Spinner
        table.put("Spinner.arrowButtonBorder", toResource(BorderFactory.createMatteBorder(0, 1, 0, 0, style.getBorderColor())));
        table.put("Spinner.arrowButtonInsets", new Insets(2, 2, 2, 2));
        table.put("Spinner.arrowButtonSize", new Dimension(16, 16));
        table.put("Spinner.border", toResource(buttonBorder));
        table.put("Spinner.background", toResource(style.getPanelBackground()));

        // Scroll pane/bar
        table.put("ScrollPane.border", toResource(noBorder));
        table.put("ScrollBar.background", toResource(style.getPanelBackground()));
        table.put("ScrollBar.thumbHighlight", toResource(transparent));
        table.put("ScrollBar.thumbShadow", toResource(transparent));
        table.put("ScrollBar.trackHighlight", toResource(transparent));
        table.put("ScrollBar.thumb", toResource(style.getScrollBarThumb()));
        table.put("ScrollBar.width", 12);

        // Viewport
        table.put("Viewport.background", toResource(style.getViewportBackground()));

        // Slider
        table.put("Slider.altTrackColor", new ColorUIResource(0xD2E2EF));
        table.put("Slider.gradient", sliderGradient);
        table.put("Slider.focusGradient", sliderGradient);

        // Split pane
        table.put("SplitPane.oneTouchButtonsOpaque", Boolean.FALSE);
        table.put("SplitPane.dividerFocusColor", toResource(style.getWindowBackground()));
        table.put("SplitPane.border", toResource(BorderFactory.createEmptyBorder()));

        // Tabbed pane
        table.put("TabbedPane.background", toResource(style.getWindowBackground()));
        table.put("TabbedPane.borderHightlightColor", toResource(style.getPanelBackground()));
        table.put("TabbedPane.contentAreaColor", toResource(style.getPanelBackground()));
        table.put("TabbedPane.contentBorderInsets", new Insets(2, 2, 3, 3));
        table.put("TabbedPane.selected", toResource(style.getSelectionHighlight()));
        table.put("TabbedPane.tabAreaBackground", toResource(style.getWindowBackground()));
        table.put("TabbedPane.tabAreaInsets", new Insets(2, 2, 0, 6));
        table.put("TabbedPane.unselectedBackground", toResource(style.getWindowBackground()));

        // Table
        table.put("Table.focusCellHighlightBorder", toResource(focusBorder));
        table.put("Table.gridColor", toResource(style.getBorderColor()));
        table.put("TableHeader.focusCellBackground", toResource(style.getSelectionHighlight()));

        // Text field
        table.put("TextField.background", toResource(style.getFormBackground()));
        table.put("TextField.foreground", toResource(style.getTextForeground()));
        table.put("TextField.inactiveForeground", toResource(style.getTextMuted()));
        table.put("TextField.border", toResource(textFieldBorder));

        // Editor panes
        table.put("EditorPane.background", toResource(style.getFormBackground()));

        // Toolbar
        table.put("ToolBar.background", toResource(style.getPanelBackground()));
        table.put("ToolBar.border", toResource(BorderFactory.createMatteBorder(0, 0, 1, 0, style.getBorderColor())));
        table.put("ToolBar.isRollover", Boolean.TRUE);

        // Tooltips
        table.put("ToolTip.background", toResource(style.getTooltipBackground()));
        table.put("ToolTip.backgroundInactive", toResource(style.getTooltipBackground()));
        table.put("ToolTip.border", toResource(BorderFactory.createLineBorder(style.getBorderColor())));
        table.put("ToolTip.borderInactive", toResource(BorderFactory.createLineBorder(style.getBorderColor())));
        table.put("ToolTip.foreground", toResource(style.getTooltipForeground()));
        table.put("ToolTip.foregroundInactive", toResource(style.getTooltipForeground()));

        // Trees
        table.put("Tree.closedIcon", toResource(directoryIcon));
        table.put("Tree.collapsedIcon", toResource(UIUtils.getIcon8FromResources("tree-expand.png")));
        table.put("Tree.expandedIcon", toResource(UIUtils.getIcon8FromResources("tree-shrink.png")));
        table.put("Tree.leafIcon", toResource(fileIcon));
        table.put("Tree.openIcon", toResource(directoryIcon));
        table.put("Tree.selectionBorderColor", toResource(getPrimary1()));
        table.put("Tree.dropLineColor", toResource(getPrimary1()));

        // Tables
        table.put("Table.dropLineColor", toResource(getPrimary1()));
        table.put("Table.dropLineShortColor", toResource(style.getTextForeground()));

        table.put("Table.dropCellBackground", toResource(style.getSelectionHighlight()));
        table.put("Tree.dropCellBackground", toResource(style.getSelectionHighlight()));

        // Lists
        table.put("List.background", toResource(style.getFormBackground()));
        table.put("List.dropCellBackground", toResource(style.getSelectionHighlight()));
        table.put("List.dropLineColor", toResource(getPrimary1()));
        table.put("List.selectionBackground", toResource(new Color(0xE0EDFA)));

        // Progress bar
        table.put("ProgressBar.background", toResource(style.getFormBackground()));
        table.put("ProgressBar.foreground", toResource(style.getPrimaryColor()));
        table.put("ProgressBar.border", toResource(new RoundedLineBorder(style.getBorderColor(), 1, 2)));
    }

    private static IconUIResource toResource(Icon icon) {
        return new IconUIResource(icon);
    }

    private static BorderUIResource toResource(Border border) {
        return new BorderUIResource(border);
    }

    private static FontUIResource toResource(Font font) {
        return new FontUIResource(font);
    }

    private static ColorUIResource toResource(Color color) {
        return new ColorUIResource(color);
    }
}

