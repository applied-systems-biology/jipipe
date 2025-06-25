package org.hkijena.jipipe.desktop.commons.theme;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.awt.*;

public class JIPipeDesktopModernThemeStyle {

    @JsonProperty("name")
    private String name = "Metal Default";
    @JsonProperty("brightness")
    private JIPipeDesktopUIThemeBrightness brightness = JIPipeDesktopUIThemeBrightness.Light;

    @JsonProperty("icon-theme")
    private JIPipeDesktopUIIconVariant iconTheme = JIPipeDesktopUIIconVariant.Light;
    @JsonProperty("icon-theme-inactive")
    private JIPipeDesktopUIIconVariant inactiveIconTheme = JIPipeDesktopUIIconVariant.LightInactive;
    @JsonProperty("icon-theme-accent")
    private JIPipeDesktopUIIconVariant accentIconTheme = JIPipeDesktopUIIconVariant.LightAccent;

    // Colors
    @JsonProperty("window-background")
    private Color windowBackground = new Color(238, 238, 238);
    @JsonProperty("panel-background")
    private Color panelBackground = new Color(238, 238, 238);
    @JsonProperty("border-color")
    private Color borderColor = new Color(172, 168, 153);
    @JsonProperty("foreground")
    private Color foreground = Color.BLACK;
    @JsonProperty("foreground-secondary")
    private Color secondaryForeground = new Color(100, 100, 100);

    @JsonProperty("selection-background")
    private Color selectionBackground = new Color(184, 207, 229);
    @JsonProperty("selection-foreground")
    private Color selectionForeground = Color.BLACK;
    @JsonProperty("primary")
    private Color primaryColor = new Color(142, 191, 239);
    @JsonProperty("focus-ring")
    private Color focusRing = new Color(64, 158, 255);
    @JsonProperty("hover-highlight")
    private Color hoverHighlight = new Color(220, 240, 255);

    @JsonProperty("button-background")
    private Color buttonBackground = new Color(214, 217, 223);
    @JsonProperty("button-hover")
    private Color buttonHover = new Color(202, 215, 235);
    @JsonProperty("button-pressed")
    private Color buttonPressed = new Color(184, 207, 229);

    @JsonProperty("text-field-background")
    private Color textFieldBackground = Color.WHITE;
    @JsonProperty("text-field-foreground")
    private Color textFieldForeground = Color.BLACK;

    @JsonProperty("divider-color")
    private Color dividerColor = new Color(172, 168, 153);

    @JsonProperty("tooltip-background")
    private Color tooltipBackground = new Color(255, 255, 225);
    @JsonProperty("tooltip-foreground")
    private Color tooltipForeground = Color.BLACK;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JIPipeDesktopUIThemeBrightness getBrightness() {
        return brightness;
    }

    public void setBrightness(JIPipeDesktopUIThemeBrightness brightness) {
        this.brightness = brightness;
    }

    public JIPipeDesktopUIIconVariant getIconTheme() {
        return iconTheme;
    }

    public void setIconTheme(JIPipeDesktopUIIconVariant iconTheme) {
        this.iconTheme = iconTheme;
    }

    public JIPipeDesktopUIIconVariant getInactiveIconTheme() {
        return inactiveIconTheme;
    }

    public void setInactiveIconTheme(JIPipeDesktopUIIconVariant inactiveIconTheme) {
        this.inactiveIconTheme = inactiveIconTheme;
    }

    public JIPipeDesktopUIIconVariant getAccentIconTheme() {
        return accentIconTheme;
    }

    public void setAccentIconTheme(JIPipeDesktopUIIconVariant accentIconTheme) {
        this.accentIconTheme = accentIconTheme;
    }

    public Color getWindowBackground() {
        return windowBackground;
    }

    public void setWindowBackground(Color windowBackground) {
        this.windowBackground = windowBackground;
    }

    public Color getPanelBackground() {
        return panelBackground;
    }

    public void setPanelBackground(Color panelBackground) {
        this.panelBackground = panelBackground;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }

    public Color getForeground() {
        return foreground;
    }

    public void setForeground(Color foreground) {
        this.foreground = foreground;
    }

    public Color getSecondaryForeground() {
        return secondaryForeground;
    }

    public void setSecondaryForeground(Color secondaryForeground) {
        this.secondaryForeground = secondaryForeground;
    }

    public Color getSelectionBackground() {
        return selectionBackground;
    }

    public void setSelectionBackground(Color selectionBackground) {
        this.selectionBackground = selectionBackground;
    }

    public Color getSelectionForeground() {
        return selectionForeground;
    }

    public void setSelectionForeground(Color selectionForeground) {
        this.selectionForeground = selectionForeground;
    }

    public Color getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(Color primaryColor) {
        this.primaryColor = primaryColor;
    }

    public Color getFocusRing() {
        return focusRing;
    }

    public void setFocusRing(Color focusRing) {
        this.focusRing = focusRing;
    }

    public Color getHoverHighlight() {
        return hoverHighlight;
    }

    public void setHoverHighlight(Color hoverHighlight) {
        this.hoverHighlight = hoverHighlight;
    }

    public Color getButtonBackground() {
        return buttonBackground;
    }

    public void setButtonBackground(Color buttonBackground) {
        this.buttonBackground = buttonBackground;
    }

    public Color getButtonHover() {
        return buttonHover;
    }

    public void setButtonHover(Color buttonHover) {
        this.buttonHover = buttonHover;
    }

    public Color getButtonPressed() {
        return buttonPressed;
    }

    public void setButtonPressed(Color buttonPressed) {
        this.buttonPressed = buttonPressed;
    }

    public Color getTextFieldBackground() {
        return textFieldBackground;
    }

    public void setTextFieldBackground(Color textFieldBackground) {
        this.textFieldBackground = textFieldBackground;
    }

    public Color getTextFieldForeground() {
        return textFieldForeground;
    }

    public void setTextFieldForeground(Color textFieldForeground) {
        this.textFieldForeground = textFieldForeground;
    }

    public Color getDividerColor() {
        return dividerColor;
    }

    public void setDividerColor(Color dividerColor) {
        this.dividerColor = dividerColor;
    }

    public Color getTooltipBackground() {
        return tooltipBackground;
    }

    public void setTooltipBackground(Color tooltipBackground) {
        this.tooltipBackground = tooltipBackground;
    }

    public Color getTooltipForeground() {
        return tooltipForeground;
    }

    public void setTooltipForeground(Color tooltipForeground) {
        this.tooltipForeground = tooltipForeground;
    }

    public boolean isDark() {
        return brightness == JIPipeDesktopUIThemeBrightness.Dark;
    }
}
