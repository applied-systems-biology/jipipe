package org.hkijena.jipipe.desktop.commons.theme;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import java.awt.*;

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
        return toResource(style.getForeground());
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
        return toResource(style.getFocusRing());
    }

    @Override
    public ColorUIResource getTextHighlightColor() {
        return toResource(style.getSelectionBackground());
    }

    @Override
    public ColorUIResource getHighlightedTextColor() {
        return toResource(style.getSelectionForeground());
    }

    private static ColorUIResource toResource(Color color) {
        return new ColorUIResource(color);
    }

    // Inject additional custom keys
    @Override
    public void addCustomEntriesToTable(UIDefaults table) {
        super.addCustomEntriesToTable(table);

        table.put("Panel.background", toResource(style.getButtonBackground()));
        table.put("Panel.foreground", toResource(style.getForeground()));
        table.put("Label.foreground", toResource(style.getForeground()));
        table.put("Label.disabledForeground", toResource(style.getSecondaryForeground()));
        table.put("TextField.background", toResource(style.getTextFieldBackground()));
        table.put("TextField.foreground", toResource(style.getTextFieldForeground()));
        table.put("TextField.caretForeground", toResource(style.getForeground()));
        table.put("ToolTip.background", toResource(style.getTooltipBackground()));
        table.put("ToolTip.foreground", toResource(style.getTooltipForeground()));
        table.put("Button.background", toResource(style.getButtonBackground()));
        table.put("Button.select", toResource(style.getButtonPressed()));
        table.put("Button.focus", toResource(style.getFocusRing()));
//        table.put("TabbedPane.selected", toResource(style.tabSelected != null ? style.tabSelected : style.panelBackground));
        table.put("SplitPane.dividerFocusColor", toResource(style.getDividerColor()));
        table.put("SplitPane.background", toResource(style.getPanelBackground()));
        table.put("Separator.foreground", toResource(style.getDividerColor()));
        table.put("Separator.background", toResource(style.getPanelBackground()));
        table.put("nimbusFocus", toResource(style.getFocusRing())); // in case Nimbus is used somewhere
    }
}

