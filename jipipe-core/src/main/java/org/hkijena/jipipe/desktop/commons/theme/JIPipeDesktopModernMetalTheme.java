package org.hkijena.jipipe.desktop.commons.theme;

import com.google.common.collect.ImmutableList;
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
import java.util.*;
import java.util.List;

public class JIPipeDesktopModernMetalTheme extends DefaultMetalTheme {

    private final JIPipeDesktopModernThemeStyle style;
    private final Font defaultFont;
    private final Border buttonBorder;
    private final Border textFieldBorder;
    private final Border noBorder = BorderFactory.createEmptyBorder();
    private final Border focusBorder;
    private final Color transparent = new Color(0, 0, 0, 0);
    private final Icon directoryIcon;
    private final Icon fileIcon;

    public JIPipeDesktopModernMetalTheme(JIPipeDesktopModernThemeStyle style) {
        this.style = style;
        
        // Fonts
        defaultFont = new Font(Font.DIALOG, Font.PLAIN, 12);
        
        // Borders
        buttonBorder = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
                BorderFactory.createCompoundBorder(new RoundedLineBorder(style.getBorderColor(), 1, 5),
                        BorderFactory.createEmptyBorder(3, 3, 3, 3)));
        textFieldBorder = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
                new RoundedLineBorder(style.getBorderColor(), 1, 5));
        focusBorder = new BorderUIResource.LineBorderUIResource(getPrimary1());

        // TODO: Icon init
        directoryIcon = UIUtils.getIconFromResources("places/folder-blue.png");
        fileIcon = UIUtils.getIconFromResources("mimetypes/gtk-file.png");
        
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

        // Helper for colors (overwrite all colors with RED!)
        for (Object key : ImmutableList.copyOf(table.keySet())) {
            Object value = table.get(key);
            if(value instanceof ColorUIResource) {
                table.put(key, toResource(Color.RED));
            }
        }
        
        configureLightAndShadows(table);
        configureForeground(table);
        configureSelection(table);
        configureFocus(table);

        configureLabel(table);
        configurePanel(table);
        configureButton(table);
        configureToggleButton(table);
        configureSeparator(table);
        configureCheckBox(table);
        configureList(table);
        configureScrollPane(table);
        configureTabbedPane(table);
        configureProgressBar(table);
        configureFileChooser(table);
        configureFileView(table);
        configureOptionPane(table);
        configureSlider(table);
        configureMenu(table);
        configurePopupMenu(table);
        configureMenuItem(table);
        configureCheckBoxMenuItem(table);
        configureRadioButtonMenuItem(table);
        configureMenuBar(table);
        configureInternalFrame(table);
        configureRadioButton(table);
        configureSpinner(table);
        configureScrollBar(table);
        configureViewport(table);
        configureSplitPane(table);
        configureTable(table);
        configureTableHeader(table);
        configureTextField(table);
        configureEditorPane(table);
        configureTextPane(table);
        configureTextArea(table);
        configureToolBar(table);
        configureToolTip(table);
        configureTree(table);

        // Helper for colors (overwrite all colors with RED!)
        List<String> missingKeys = new ArrayList<>();
        for (Object key : ImmutableList.copyOf(table.keySet())) {
            Object value = table.get(key);
            if(value instanceof ColorUIResource) {
               if(((ColorUIResource) value).getRed() == 255 && ((ColorUIResource) value).getGreen() == 0 && ((ColorUIResource) value).getBlue() == 0) {
                   missingKeys.add(key.toString());
               }
            }
        }
        missingKeys.sort(Comparator.naturalOrder());
        for (String missingKey : missingKeys) {
            System.out.println("Missing key: " + missingKey);
        }


//        System.exit(0);
    }


    private void configureTableHeader(UIDefaults table) {
        table.put("TableHeader.focusCellBackground", toResource(style.getSelectionHighlight()));
    }

    private void configureTree(UIDefaults table) {
        table.put("Tree.closedIcon", toResource(directoryIcon));
        table.put("Tree.collapsedIcon", toResource(UIUtils.getIcon8FromResources("tree-expand.png")));
        table.put("Tree.expandedIcon", toResource(UIUtils.getIcon8FromResources("tree-shrink.png")));
        table.put("Tree.leafIcon", toResource(fileIcon));
        table.put("Tree.openIcon", toResource(directoryIcon));
        table.put("Tree.selectionBorderColor", toResource(getPrimary1()));
        table.put("Tree.dropLineColor", toResource(getPrimary1()));
        table.put("Tree.dropCellBackground", toResource(style.getSelectionHighlight()));
    }

    private void configureToolTip(UIDefaults table) {
        table.put("ToolTip.background", toResource(style.getTooltipBackground()));
        table.put("ToolTip.backgroundInactive", toResource(style.getTooltipBackground()));
        table.put("ToolTip.border", toResource(BorderFactory.createLineBorder(style.getBorderColor())));
        table.put("ToolTip.borderInactive", toResource(BorderFactory.createLineBorder(style.getBorderColor())));
        table.put("ToolTip.foreground", toResource(style.getTooltipForeground()));
        table.put("ToolTip.foregroundInactive", toResource(style.getTooltipForeground()));
    }

    private void configureToolBar(UIDefaults table) {
        table.put("ToolBar.background", toResource(style.getPanelBackground()));
        table.put("ToolBar.border", toResource(BorderFactory.createMatteBorder(0, 0, 1, 0, style.getBorderColor())));
        table.put("ToolBar.isRollover", Boolean.TRUE);
    }

    private void configureTextArea(UIDefaults table) {
        table.put("TextArea.background", toResource(style.getFormBackground()));
    }

    private void configureTextPane(UIDefaults table) {
        table.put("TextPane.background", toResource(style.getFormBackground()));
    }

    private void configureEditorPane(UIDefaults table) {
        table.put("EditorPane.background", toResource(style.getFormBackground()));
    }

    private void configureTextField(UIDefaults table) {
        table.put("TextField.background", toResource(style.getFormBackground()));
        table.put("TextField.border", toResource(textFieldBorder));
    }

    private void configureTable(UIDefaults table) {
        table.put("Table.focusCellHighlightBorder", toResource(focusBorder));
        table.put("Table.gridColor", toResource(style.getBorderColor()));
        table.put("Table.dropLineColor", toResource(getPrimary1()));
        table.put("Table.dropLineShortColor", toResource(style.getTextForeground()));
        table.put("Table.dropCellBackground", toResource(style.getSelectionHighlight()));
    }

    private void configureSplitPane(UIDefaults table) {
        table.put("SplitPane.oneTouchButtonsOpaque", Boolean.FALSE);
        table.put("SplitPane.dividerFocusColor", toResource(style.getWindowBackground()));
        table.put("SplitPane.border", toResource(BorderFactory.createEmptyBorder()));
        table.put("SplitPane.background", toResource(style.getWindowBackground()));

        table.put("SplitPaneDivider.border", BorderFactory.createEmptyBorder());
    }

    private void configureViewport(UIDefaults table) {
        table.put("Viewport.background", toResource(style.getViewportBackground()));
    }

    private void configureScrollBar(UIDefaults table) {
        table.put("ScrollBar.background", toResource(style.getPanelBackground()));
        table.put("ScrollBar.thumbHighlight", toResource(transparent));
        table.put("ScrollBar.thumbShadow", toResource(transparent));
        table.put("ScrollBar.trackHighlight", toResource(transparent));
        table.put("ScrollBar.thumb", toResource(style.getScrollBarThumb()));
        table.put("ScrollBar.width", 12);
    }

    private void configureSpinner(UIDefaults table) {
        table.put("Spinner.arrowButtonBorder", toResource(BorderFactory.createMatteBorder(0, 1, 0, 0, style.getBorderColor())));
        table.put("Spinner.arrowButtonInsets", new Insets(2, 2, 2, 2));
        table.put("Spinner.arrowButtonSize", new Dimension(16, 16));
        table.put("Spinner.border", toResource(buttonBorder));
        table.put("Spinner.background", toResource(style.getPanelBackground()));
    }

    private void configureRadioButton(UIDefaults table) {
        table.put("RadioButton.background", toResource(style.getPanelBackground()));
        table.put("RadioButton.rollover", Boolean.TRUE);
    }

    private void configureInternalFrame(UIDefaults table) {
        table.put("InternalFrame.activeTitleBackground", toResource(style.getWindowBackground()));
        table.put("InternalFrame.closeIcon", toResource(UIUtils.getIconFromResources("actions/close-tab.png")));
        table.put("InternalFrame.iconifyIcon", toResource(UIUtils.getIconFromResources("actions/xfce-wm-minimize.png")));
        table.put("InternalFrame.minimizeIcon", toResource(UIUtils.getIconFromResources("actions/xfce-wm-minimize.png")));
        table.put("InternalFrame.icon", toResource(UIUtils.getIconFromResources("actions/hamburger-menu.png")));
        table.put("InternalFrame.maximizeIcon", toResource(UIUtils.getIconFromResources("actions/xfce-wm-maximize.png")));
        table.put("InternalFrame.paletteCloseIcon", toResource(UIUtils.getIconFromResources("actions/close-tab.png")));
    }

    private void configureMenuBar(UIDefaults table) {
        table.put("MenuBarUI", "javax.swing.plaf.metal.MetalMenuBarUI");
        table.put("MenuBar.background", toResource(style.getWindowBackground()));
        table.put("MenuBar.borderColor", toResource(style.getWindowBackground()));
    }

    private void configureRadioButtonMenuItem(UIDefaults table) {
        table.put("RadioButtonMenuItem.background", toResource(style.getMenuBackground()));
    }

    private void configureCheckBoxMenuItem(UIDefaults table) {
        table.put("CheckBoxMenuItem.border", toResource(BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        table.put("CheckBoxMenuItem.borderPainted", Boolean.FALSE);
        table.put("CheckBoxMenuItem.background", toResource(style.getMenuBackground()));
        table.put("CheckBoxMenuItem.selectionBackground", toResource(style.getSelectionBackground()));
        table.put("CheckBoxMenuItem.selectionForeground", toResource(style.getSelectionForeground()));
        table.put("CheckBoxMenuItem.checkIcon", toResource(new CheckBoxMenuItemIcon(style.getTextForegroundInverted())));
    }

    private void configureMenuItem(UIDefaults table) {
        table.put("MenuItem.background", toResource(style.getMenuBackground()));
        table.put("MenuItem.acceleratorForeground", toResource(style.getTextMuted()));
        table.put("MenuItem.border", toResource(BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        table.put("MenuItem.borderPainted", Boolean.FALSE);
        table.put("MenuItem.selectionBackground", toResource(style.getSelectionBackground()));
        table.put("MenuItem.selectionForeground", toResource(style.getSelectionForeground()));
    }

    private void configurePopupMenu(UIDefaults table) {
        table.put("PopupMenu.border", toResource(new RoundedLineBorder(style.getBorderColor(), 1, 5)));
        table.put("PopupMenu.background", toResource(style.getMenuBackground()));
    }

    private void configureMenu(UIDefaults table) {
        table.put("Menu.opaque", Boolean.FALSE);
        table.put("Menu.background", toResource(style.getMenuBackground()));
        table.put("Menu.border", toResource(BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        table.put("Menu.borderPainted", Boolean.FALSE);
        table.put("Menu.selectionBackground", toResource(style.getSelectionBackground()));
        table.put("Menu.selectionForeground", toResource(style.getSelectionForeground()));
    }

    private void configureSlider(UIDefaults table) {
        final List<Object> sliderGradient = Arrays.asList(new Object[]{.3f, .2f, getPrimary3(), getWhite(), new ColorUIResource(getSecondary1())});
        table.put("Slider.altTrackColor", style.getSelectionHighlight());
        table.put("Slider.gradient", sliderGradient);
        table.put("Slider.focusGradient", sliderGradient);
    }

    private void configureOptionPane(UIDefaults table) {
        table.put("OptionPane.errorIcon", toResource(UIUtils.getIcon32FromResources("dialog-error.png")));
        table.put("OptionPane.informationIcon", toResource(UIUtils.getIcon32FromResources("dialog-info.png")));
        table.put("OptionPane.questionIcon", toResource(UIUtils.getIcon32FromResources("dialog-question.png")));
        table.put("OptionPane.warningIcon", toResource(UIUtils.getIcon32FromResources("dialog-warning.png")));
    }

    private void configureFileView(UIDefaults table) {
        table.put("FileView.computerIcon", toResource(UIUtils.getIconFromResources("devices/computer.png")));
        table.put("FileView.directoryIcon", toResource(directoryIcon));
        table.put("FileView.hardDriveIcon", toResource(UIUtils.getIconFromResources("devices/drive-harddisk.png")));
        table.put("FileView.fileIcon", toResource(fileIcon));
        table.put("FileView.floppyDriveIcon", toResource(UIUtils.getIconFromResources("devices/media-floppy.png")));
    }

    private void configureFileChooser(UIDefaults table) {
        table.put("FileChooser.homeFolderIcon", toResource(UIUtils.getIconFromResources("actions/go-home.png")));
        table.put("FileChooser.newFolderIcon", toResource(UIUtils.getIconFromResources("actions/folder-new.png")));
        table.put("FileChooser.upFolderIcon", toResource(UIUtils.getIconFromResources("actions/go-parent-folder.png")));
    }

    private void configureProgressBar(UIDefaults table) {
        table.put("ProgressBar.background", toResource(style.getFormBackground()));
        table.put("ProgressBar.foreground", toResource(style.getPrimaryColor()));
        table.put("ProgressBar.border", toResource(new RoundedLineBorder(style.getBorderColor(), 1, 2)));
    }

    private void configureTabbedPane(UIDefaults table) {
        table.put("TabbedPane.font", toResource(defaultFont));
        table.put("TabbedPane.background", toResource(style.getWindowBackground()));
        table.put("TabbedPane.borderHightlightColor", toResource(style.getWindowBackground()));
        table.put("TabbedPane.contentAreaColor", toResource(style.getWindowBackground()));
        table.put("TabbedPane.contentBorderInsets", new Insets(2, 2, 3, 3));
        table.put("TabbedPane.selected", toResource(style.getTabSelectedBackground()));
        table.put("TabbedPane.selectHighlight", toResource(style.getTabSelectedHighlight()));
        table.put("TabbedPane.tabAreaBackground", toResource(style.getWindowBackground()));
        table.put("TabbedPane.tabAreaInsets", new Insets(2, 2, 0, 6));
        table.put("TabbedPane.unselectedBackground", toResource(style.getWindowBackground()));
    }

    private void configureScrollPane(UIDefaults table) {
        table.put("ScrollPane.font", toResource(defaultFont));
        table.put("ScrollPane.border", toResource(noBorder));
    }

    private void configureList(UIDefaults table) {
        table.put("List.font", toResource(defaultFont));
        table.put("List.background", toResource(style.getFormBackground()));
        table.put("List.dropCellBackground", toResource(style.getSelectionHighlight()));
        table.put("List.dropLineColor", toResource(getPrimary1()));
        table.put("List.selectionBackground", toResource(style.getSelectionHighlight()));
        table.put("List.focusCellHighlightBorder", toResource(focusBorder));
    }

    private void configureLabel(UIDefaults table) {
        table.put("Label.font", toResource(defaultFont));
        table.put("Label.disabledForeground", getInactiveControlTextColor());
    }

    private void configureCheckBox(UIDefaults table) {
        table.put("CheckBox.font", toResource(defaultFont));
        table.put("CheckBox.rollover", Boolean.TRUE);
        table.put("CheckBox.icon", toResource(new CheckBoxIcon(style.getTextForegroundInverted())));
    }

    private void configureLightAndShadows(UIDefaults table) {
        for (Object o : ImmutableList.copyOf(table.keySet())) {
            if(o.toString().toLowerCase(Locale.ROOT).endsWith("shadow") || o.toString().toLowerCase(Locale.ROOT).endsWith("light")) {
                table.put(o, null);
            }
        }
    }

    private void configureFocus(UIDefaults table) {
        for (Object o : ImmutableList.copyOf(table.keySet())) {
            if(o.toString().endsWith(".focus")) {
                table.put(o, style.getPrimaryColor());
            }
        }
    }

    private void configureSelection(UIDefaults table) {
        for (Object o : ImmutableList.copyOf(table.keySet())) {
            if(o.toString().endsWith(".selectionBackground")) {
                table.put(o, toResource(style.getSelectionBackground()));
            }
            else if (o.toString().endsWith(".selectionForeground")) {
                table.put(o, toResource(style.getTextForeground()));
            }
        }
    }

    private void configureForeground(UIDefaults table) {
        for (Object o : ImmutableList.copyOf(table.keySet())) {
            if(o.toString().endsWith(".foreground")) {
                table.put(o, toResource(style.getTextForeground()));
            }
            else if (o.toString().endsWith(".inactiveForeground") || o.toString().endsWith(".disabledText")) {
                table.put(o, toResource(style.getTextMuted()));
            }
        }
    }

    private void configureSeparator(UIDefaults table) {
        table.put("Separator.foreground", toResource(style.getBorderColor()));
        table.put("Separator.background", toResource(style.getWindowBackground()));
        table.put("Separator.highlight", toResource(style.getWindowBackground()));
    }

    private void configureToggleButton(UIDefaults table) {
        table.put("ToggleButton.font", toResource(defaultFont));
        table.put("ToggleButton.background", toResource(style.getButtonBackground()));
        table.put("ToggleButton.border", toResource(buttonBorder));
        table.put("ToggleButton.select", toResource(style.getButtonToggled()));
    }

    private void configurePanel(UIDefaults table) {
        table.put("Panel.font", toResource(defaultFont));
        table.put("Panel.background", toResource(style.getPanelBackground()));
    }

    private void configureButton(UIDefaults table) {
        table.put("Button.font", toResource(defaultFont));
        table.put("Button.background", toResource(style.getButtonBackground()));
        table.put("Button.rollover", Boolean.TRUE);
        table.put("Button.toolBarBorderBackground", toResource(style.getBorderColor()));
        table.put("Button.disabledToolBarBorderBackground", toResource(style.getBorderColor()));
        table.put("Button.rolloverIconType", "ocean");
        table.put("Button.border", toResource(buttonBorder));
        table.put("Button.borderColor", toResource(style.getBorderColor()));
        table.put("Button.focus", toResource(style.getButtonToggled()));
        table.put("Button.highlight", style.getSelectionHighlight());
        table.put("Button.disabledText", toResource(style.getTextMuted()));
        table.put("Button.select", toResource(style.getButtonToggled()));
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

