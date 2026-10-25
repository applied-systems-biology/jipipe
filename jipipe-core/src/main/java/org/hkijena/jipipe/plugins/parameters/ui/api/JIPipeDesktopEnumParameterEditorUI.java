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

package org.hkijena.jipipe.plugins.parameters.ui.api;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.selection.JIPipeDesktopRichSelectionBox;
import org.hkijena.jipipe.plugins.parameters.api.enums.EnumParameterSettings;
import org.hkijena.jipipe.plugins.parameters.api.enums.JIPipeDefaultEnumItemInfo;
import org.hkijena.jipipe.plugins.parameters.api.enums.JIPipeEnumItemInfoRenderTarget;
import org.hkijena.jipipe.plugins.parameters.api.enums.JIPipeEnumParameterItemInfo;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * A parameter editor UI that works for all enumerations.
 * <p>
 * #1339: small option sets (<= {@link EnumUiSelection#COMBO_MAX_VALUES}) render
 * a plain combo box without a search button; larger sets render a
 * {@link JIPipeDesktopRichSelectionBox} (searchable popup, closes on pick,
 * no modal).
 */
public class JIPipeDesktopEnumParameterEditorUI extends JIPipeDesktopParameterEditorUI<Object> {

    private JComboBox<Object> comboBox;
    private JIPipeDesktopRichSelectionBox selectionBox;
    private JIPipeEnumParameterItemInfo enumItemInfo = new JIPipeDefaultEnumItemInfo();
    /** The enum constants, sorted by display label (computed once). */
    private Object[] sortedValues;

    public JIPipeDesktopEnumParameterEditorUI(InitializationParameters parameters) {
        super(Object.class, parameters);
        initialize();
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        Object target = getParameterAccess().get(Object.class);
        if (comboBox != null) {
            if (!Objects.equals(target, comboBox.getSelectedItem())) {
                comboBox.setSelectedItem(target);
            }
        } else if (selectionBox != null) {
            if (!selectionBox.getSelectedValues().contains(target)) {
                selectionBox.setSelectedValues(target == null ? List.of() : List.of(target));
            }
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());

        // Option values + item info
        sortedValues = getParameterAccess().getFieldClass().getEnumConstants();
        EnumParameterSettings settings = getParameterAccess().getAnnotationOfType(EnumParameterSettings.class);
        if (settings != null) {
            enumItemInfo = (JIPipeEnumParameterItemInfo) ReflectionUtils.newInstance(settings.itemInfo());
        }
        Arrays.sort(sortedValues, Comparator.comparing(value -> enumItemInfo.getLabel(value, JIPipeEnumItemInfoRenderTarget.ComboBox)));

        // The enum's constant set is fixed, so the mode is computed once.
        EnumUiMode mode = EnumUiSelection.mode(sortedValues.length, false, false);
        if (mode == EnumUiMode.COMBO) {
            createCombo();
        } else {
            createSelectionBox();
        }
    }

    private void createCombo() {
        comboBox = new JComboBox<>(sortedValues);
        comboBox.setSelectedItem(getParameterAccess().get(Object.class));
        comboBox.addActionListener(e -> {
            setParameter(comboBox.getSelectedItem(), false);
        });
        comboBox.setRenderer(new Renderer(enumItemInfo, JIPipeEnumItemInfoRenderTarget.ComboBox));
        add(comboBox, BorderLayout.CENTER);
    }

    private void createSelectionBox() {
        selectionBox = new JIPipeDesktopRichSelectionBox("Select value", false,
                this::options,
                selected -> {
                    if (!selected.isEmpty()) {
                        setParameter(selected.get(0).getValue(), true);
                    }
                });
        add(selectionBox, BorderLayout.CENTER);
        // Show the current value's label/icon on the button (the option index
        // is only built on the first popup open otherwise).
        selectionBox.setSelectedValues(getParameterAccess().get(Object.class) == null
                ? List.of() : List.of(getParameterAccess().get(Object.class)));
        selectionBox.refreshDisplay();
    }

    private List<JIPipeDesktopRichSelectionBox.Option<?>> options() {
        List<JIPipeDesktopRichSelectionBox.Option<?>> result = new ArrayList<>();
        for (Object value : sortedValues) {
            result.add(new JIPipeDesktopRichSelectionBox.Option<>(value,
                    enumItemInfo.getLabel(value, JIPipeEnumItemInfoRenderTarget.List),
                    enumItemInfo.getIcon(value, JIPipeEnumItemInfoRenderTarget.List),
                    null,
                    enumItemInfo.getTooltip(value, JIPipeEnumItemInfoRenderTarget.List)));
        }
        return result;
    }

    /**
     * Renders items in enum parameters
     */
    public static class Renderer extends JLabel implements ListCellRenderer<Object> {

        private final JIPipeEnumParameterItemInfo info;
        private final JIPipeEnumItemInfoRenderTarget renderTarget;

        public Renderer(JIPipeEnumParameterItemInfo info, JIPipeEnumItemInfoRenderTarget renderTarget) {
            this.info = info;
            this.renderTarget = renderTarget;
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            setFont(new Font(Font.DIALOG, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeNormal()));
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            setIcon(info.getIcon(value, renderTarget));
            setText(info.getLabel(value, renderTarget));
            setToolTipText(info.getTooltip(value, renderTarget));
            if (isSelected || cellHasFocus) {
                setBackground(UIManager.getColor("List.selectionBackground"));
            } else {
                setBackground(UIManager.getColor("List.background"));
            }
            return this;
        }
    }
}
