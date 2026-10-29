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
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.selection.JIPipeDesktopRichSelectionBox;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeNodeInfoRef;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Parameter for {@link JIPipeNodeInfoRef}.
 * <p>
 * #1339 item 1: the flat algorithm picker is now a
 * {@link JIPipeDesktopRichSelectionBox} — the searchable "new combobox" used by
 * the enum/dynamic-set editors — instead of a display button + modal picker
 * window.
 */
public class JIPipeDesktopNodeInfoRefParameterEditorUI extends JIPipeDesktopParameterEditorUI<JIPipeNodeInfoRef> {

    private JIPipeDesktopRichSelectionBox selectionBox;
    private List<JIPipeNodeInfo> availableInfos = new ArrayList<>();

    public JIPipeDesktopNodeInfoRefParameterEditorUI(InitializationParameters parameters) {
        super(JIPipeNodeInfoRef.class, parameters);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        availableInfos = JIPipe.getNodes().getRegisteredNodeInfos().values().stream()
                .filter(d -> !d.isHidden())
                .sorted(Comparator.comparing(JIPipeNodeInfo::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        selectionBox = new JIPipeDesktopRichSelectionBox("Select algorithm type", false,
                this::options,
                selected -> {
                    if (selected.isEmpty()) {
                        JIPipeNodeInfoRef ref = new JIPipeNodeInfoRef();
                        setParameter(ref, true);
                    } else {
                        JIPipeNodeInfoRef ref = getParameter();
                        ref.setInfo((JIPipeNodeInfo) selected.get(0).getValue());
                        setParameter(ref, true);
                    }
                });
        add(selectionBox, BorderLayout.CENTER);
    }

    private List<JIPipeDesktopRichSelectionBox.Option<?>> options() {
        List<JIPipeDesktopRichSelectionBox.Option<?>> result = new ArrayList<>();
        for (JIPipeNodeInfo info : availableInfos) {
            result.add(new JIPipeDesktopRichSelectionBox.Option<>(info,
                    info.getName(),
                    UIUtils.getIconFromColor(ThemeUtils.getNodeFillColor(info)),
                    null,
                    TooltipUtils.getAlgorithmTooltip(info)));
        }
        return result;
    }

    @Override
    public void reload() {
        if (selectionBox == null) {
            return;
        }
        JIPipeNodeInfo info = getParameter().getInfo();
        selectionBox.setSelectedValues(info == null ? List.of() : List.of(info));
        selectionBox.refreshDisplay();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }
}