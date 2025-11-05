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

package org.hkijena.jipipe.desktop.commons.components.color.palette;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.layouts.JIPipeDesktopWrapLayout;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.parameters.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.parameters.ui.library.ColorParameterSettings;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.collections.IdentityArrayList;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * A component that allows the user to pick foreground/background color. Supports transparency
 */
public class JIPipeDesktopColorPaletteUI extends JIPipeDesktopWorkbenchPanel {
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
    private final JPanel colorsPanel = new JPanel();
    private final SelectedEventEmitter selectedEventEmitter = new SelectedEventEmitter();
    private JIPipeDesktopColorPaletteUserColorStorage userColors = new JIPipeDesktopSimpleColorPaletteUserColorStorage();
    private JIPipeDesktopColorPaletteColor selectedColor;

    public JIPipeDesktopColorPaletteUI(JIPipeDesktopWorkbench workbench) {
        this(workbench, NONE, JIPipeDesktopColorPalette.PASTEL);
    }

    public JIPipeDesktopColorPaletteUI(JIPipeDesktopWorkbench workbench, int flags, JIPipeDesktopColorPaletteColor... defaultColors) {
        super(workbench);
        this.enableAlphaColorSelection = (flags & WITH_ALPHA) == WITH_ALPHA;
        this.enableBackgroundColorSelection = (flags & NO_BACKGROUND) != NO_BACKGROUND;
        this.defaultColors = new IdentityArrayList<>();
        for (JIPipeDesktopColorPaletteColor color : defaultColors) {
            this.defaultColors.add(new JIPipeDesktopColorPaletteColor(color));
        }
        initialize();
        rebuild();
        resetSelectedColor();
    }

    private void initialize() {
        colorsPanel.setLayout(new JIPipeDesktopWrapLayout(FlowLayout.LEFT));
        setLayout(new BorderLayout());
        add(colorsPanel, BorderLayout.CENTER);
    }

    public void rebuild() {
        colorsPanel.removeAll();
        for (JIPipeDesktopColorPaletteColor color : defaultColors) {
            addSelectableColor(colorsPanel, color, false);
        }
        for (JIPipeDesktopColorPaletteColor color : userColors.getColors()) {
            JButton button = addSelectableColor(colorsPanel, color, true);
            JPopupMenu popupMenu = UIUtils.addRightClickPopupMenuToButton(button);
            popupMenu.add(UIUtils.createMenuItem("Edit", "Edits the color", JIPipe.RESOURCES.getIcon16("actions/edit.png"), () -> {
                editUserColor(color);
            }));
            popupMenu.add(UIUtils.createMenuItem("Delete", "Deletes the color", JIPipe.RESOURCES.getIcon16("actions/edit-delete.png"), () -> {
                deleteUserColor(color);
            }));
        }

        // Button to create new colors
        JButton addUserColorButton = new JButton();
        addUserColorButton.setToolTipText("Add custom color");
        addUserColorButton.setIcon(JIPipe.RESOURCES.getIcon32("actions/list-add.png"));
        addUserColorButton.setBorder(null);
        addUserColorButton.setOpaque(false);
        addUserColorButton.addActionListener(e -> {
            addUserColor();
        });
        colorsPanel.add(addUserColorButton);

        revalidate();
        repaint(50);
    }

    private void deleteUserColor(JIPipeDesktopColorPaletteColor color) {
        if (JOptionPane.showConfirmDialog(this, "Do you really want to remove the selected color?", "Remove custom color", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            userColors.removeColor(color);
        }
    }

    private void editUserColor(JIPipeDesktopColorPaletteColor color) {
        if (enableBackgroundColorSelection) {
            PaletteColorParameters newColorParams = new PaletteColorParameters(color);
            if (JIPipeDesktopParameterFormPanel.showDialog(getDesktopWorkbench(), this, newColorParams, MarkdownText.EMPTY, "Edit custom color", JIPipeDesktopFormPanel.WITH_SCROLLING)) {
                JIPipeDesktopColorPaletteColor newColor = new JIPipeDesktopColorPaletteColor(newColorParams.foreground, newColorParams.background);
                if (!enableAlphaColorSelection) {
                    newColor.setForeground(ColorUtils.removeAlpha(newColor.getForeground()));
                    newColor.setBackground(ColorUtils.removeAlpha(newColor.getBackground()));
                }
                if (selectedColor == color) {
                    resetSelectedColor();
                }
                userColors.replaceColor(color, newColor);
                rebuild();
            }
        } else {
            Color newColor = UIUtils.selectColor(this, "Add custom color", defaultColors.isEmpty() ? Color.RED : defaultColors.getFirst().getForeground(), enableAlphaColorSelection);
            if (!enableAlphaColorSelection) {
                newColor = ColorUtils.removeAlpha(newColor);
            }
            userColors.replaceColor(color, new JIPipeDesktopColorPaletteColor(newColor));
            rebuild();
        }
    }

    private void resetSelectedColor() {
        if (!this.defaultColors.isEmpty()) {
            setSelectedColor(this.defaultColors.getFirst());
        } else if (!this.userColors.getColors().isEmpty()) {
            setSelectedColor(this.userColors.getColors().getFirst());
        }
    }

    private void addUserColor() {
        if (enableBackgroundColorSelection) {
            PaletteColorParameters newColorParams = new PaletteColorParameters();
            if (JIPipeDesktopParameterFormPanel.showDialog(getDesktopWorkbench(), this, newColorParams, MarkdownText.EMPTY, "Add custom color", JIPipeDesktopFormPanel.WITH_SCROLLING)) {
                JIPipeDesktopColorPaletteColor newColor = new JIPipeDesktopColorPaletteColor(newColorParams.foreground, newColorParams.background);
                if (!enableAlphaColorSelection) {
                    newColor.setForeground(ColorUtils.removeAlpha(newColor.getForeground()));
                    newColor.setBackground(ColorUtils.removeAlpha(newColor.getBackground()));
                }
                userColors.addColor(newColor);
                rebuild();
            }
        } else {
            Color newColor = UIUtils.selectColor(this, "Add custom color", defaultColors.isEmpty() ? Color.RED : defaultColors.getFirst().getForeground(), enableAlphaColorSelection);
            if (!enableAlphaColorSelection) {
                newColor = ColorUtils.removeAlpha(newColor);
            }
            userColors.addColor(new JIPipeDesktopColorPaletteColor(newColor));
            rebuild();
        }
    }

    private JButton addSelectableColor(JPanel target, JIPipeDesktopColorPaletteColor color, boolean user) {
        JButton button = new JButton();
        button.setIcon(new JIPipeDesktopColorPaletteColorIcon(32, color, user, this));
        button.setBorder(null);
        button.setOpaque(false);
        button.addActionListener(e -> {
            setSelectedColor(color);
        });
        target.add(button);
        return button;
    }

    public JIPipeDesktopColorPaletteUserColorStorage getUserColors() {
        return userColors;
    }

    public void setUserColors(JIPipeDesktopColorPaletteUserColorStorage userColors) {
        this.userColors = userColors;
        rebuild();
    }

    public boolean isEnableAlphaColorSelection() {
        return enableAlphaColorSelection;
    }

    public boolean isEnableBackgroundColorSelection() {
        return enableBackgroundColorSelection;
    }

    public JIPipeDesktopColorPaletteColor getSelectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(JIPipeDesktopColorPaletteColor selectedColor) {
        if (selectedColor == null) {
            resetSelectedColor();
            return;
        }
        if (!defaultColors.contains(selectedColor) && !userColors.getColors().contains(selectedColor)) {
            for (JIPipeDesktopColorPaletteColor color : userColors.getColors()) {
                if (selectedColor.equals(color)) {
                    selectedColor = color;
                    break;
                }
            }
            for (JIPipeDesktopColorPaletteColor color : defaultColors) {
                if (selectedColor.equals(color)) {
                    selectedColor = color;
                    break;
                }
            }
        }
        this.selectedColor = selectedColor;
        repaint(50);
        selectedEventEmitter.emit(new SelectedEvent(this, selectedColor));
    }

    public SelectedEventEmitter getSelectedEventEmitter() {
        return selectedEventEmitter;
    }

    public interface SelectedEventListener {
        void onColorPaletteSelected(SelectedEvent event);
    }

    public static class SelectedEvent extends AbstractJIPipeEvent {
        private final JIPipeDesktopColorPaletteUI colorPaletteUI;
        private final JIPipeDesktopColorPaletteColor color;

        public SelectedEvent(JIPipeDesktopColorPaletteUI colorPaletteUI, JIPipeDesktopColorPaletteColor color) {
            super(colorPaletteUI);
            this.colorPaletteUI = colorPaletteUI;
            this.color = color;
        }

        public JIPipeDesktopColorPaletteUI getColorPaletteUI() {
            return colorPaletteUI;
        }

        public JIPipeDesktopColorPaletteColor getColor() {
            return color;
        }
    }

    public static class SelectedEventEmitter extends JIPipeEventEmitter<SelectedEvent, SelectedEventListener> {

        @Override
        protected void call(SelectedEventListener selectedEventListener, SelectedEvent event) {
            selectedEventListener.onColorPaletteSelected(event);
        }
    }

    public static class PaletteColorParameters extends AbstractJIPipeParameterCollection {
        private Color background = Color.RED.brighter();
        private Color foreground = Color.RED;

        public PaletteColorParameters() {

        }

        public PaletteColorParameters(JIPipeDesktopColorPaletteColor color) {
            this.background = color.getBackground();
            this.foreground = color.getForeground();
        }

        @SetJIPipeDocumentation(name = "Background")
        @JIPipeParameter("background")
        @ColorParameterSettings(withTransparency = true)
        public Color getBackground() {
            return background;
        }

        @JIPipeParameter("background")
        public void setBackground(Color background) {
            this.background = background;
        }

        @SetJIPipeDocumentation(name = "Foreground")
        @JIPipeParameter(value = "foreground", uiOrder = -100)
        @ColorParameterSettings(withTransparency = true)
        public Color getForeground() {
            return foreground;
        }

        @JIPipeParameter("foreground")
        public void setForeground(Color foreground) {
            this.foreground = foreground;
        }
    }
}
