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

package org.hkijena.jipipe.plugins.parameters.ui.library;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.selection.JIPipeDesktopRichSelectionBox;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeParameterTypeInfoRef;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parameter for {@link JIPipeParameterTypeInfoRef}.
 * <p>
 * #1339 item 1: the flat parameter-type picker is now a
 * {@link JIPipeDesktopRichSelectionBox} - the searchable "new combobox" used by
 * the enum/dynamic-set editors - instead of a display button + modal picker.
 */
public class JIPipeDesktopParameterTypeInfoRefParameterEditorUI extends JIPipeDesktopParameterEditorUI<JIPipeParameterTypeInfoRef> {

    private JIPipeDesktopRichSelectionBox selectionBox;
    private List<JIPipeParameterTypeInfo> availableInfos = new ArrayList<>();

    public JIPipeDesktopParameterTypeInfoRefParameterEditorUI(InitializationParameters parameters) {
        super(JIPipeParameterTypeInfoRef.class, parameters);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        availableInfos = new ArrayList<>(JIPipe.getParameterTypes().getRegisteredParameters().values());
        availableInfos.sort(Comparator.comparing(JIPipeParameterTypeInfo::getName, String.CASE_INSENSITIVE_ORDER));

        selectionBox = new JIPipeDesktopRichSelectionBox("Select parameter type", false,
                this::options,
                selected -> {
                    if (selected.isEmpty()) {
                        setParameter(new JIPipeParameterTypeInfoRef(), true);
                    } else {
                        setParameter(new JIPipeParameterTypeInfoRef((JIPipeParameterTypeInfo) selected.get(0).getValue()), true);
                    }
                });
        add(selectionBox, BorderLayout.CENTER);
    }

    private List<JIPipeDesktopRichSelectionBox.Option<?>> options() {
        // The picker restricted items by the annotation-provided allowed
        // parameter types (via getInfoByFieldClass); mirror that here so the
        // combobox shows the same restricted set.
        List<JIPipeParameterTypeInfo> infos = availableInfos;
        JIPipeParameterTypeInfoRef ref = getParameter();
        Set<Class<?>> allowed = ref.getUiAllowedParameterTypes();
        if (allowed != null && !allowed.isEmpty()) {
            infos = allowed.stream()
                    .map(klass -> JIPipe.getParameterTypes().getInfoByFieldClass(klass))
                    .filter(java.util.Objects::nonNull)
                    .sorted(Comparator.comparing(JIPipeParameterTypeInfo::getName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
        }
        Icon icon = JIPipe.RESOURCES.getIcon16("data-types/parameters.png");
        List<JIPipeDesktopRichSelectionBox.Option<?>> result = new ArrayList<>();
        for (JIPipeParameterTypeInfo info : infos) {
            result.add(new JIPipeDesktopRichSelectionBox.Option<>(info,
                    info.getName(),
                    icon,
                    null,
                    info.getDescription()));
        }
        return result;
    }

    @Override
    public void reload() {
        if (selectionBox == null) {
            return;
        }
        JIPipeParameterTypeInfo info = getParameter().getInfo();
        selectionBox.setSelectedValues(info == null ? List.of() : List.of(info));
        selectionBox.refreshDisplay();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }
}