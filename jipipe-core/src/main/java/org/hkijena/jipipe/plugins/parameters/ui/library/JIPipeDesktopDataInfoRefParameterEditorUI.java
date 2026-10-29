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
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.selection.JIPipeDesktopRichSelectionBox;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeDataParameterSettings;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.classfilters.AnyClassFilter;
import org.hkijena.jipipe.utils.classfilters.ClassFilter;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Parameter for {@link JIPipeDataInfoRef}.
 * <p>
 * #1339 item 1: the flat data-type picker is now a
 * {@link JIPipeDesktopRichSelectionBox} - the searchable "new combobox" used by
 * the enum/dynamic-set editors - instead of a display button + modal picker
 * window. The box's dropdown groups by type category and filters as you type.
 */
public class JIPipeDesktopDataInfoRefParameterEditorUI extends JIPipeDesktopParameterEditorUI<JIPipeDataInfoRef> {

    private JIPipeDesktopRichSelectionBox selectionBox;
    private List<JIPipeDataInfo> availableInfos = new ArrayList<>();

    public JIPipeDesktopDataInfoRefParameterEditorUI(InitializationParameters parameters) {
        super(JIPipeDataInfoRef.class, parameters);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        collectAvailableInfos();
        availableInfos.sort(Comparator.comparing(JIPipeDataInfo::getName, String.CASE_INSENSITIVE_ORDER));

        selectionBox = new JIPipeDesktopRichSelectionBox("Select data type", false,
                this::options,
                selected -> {
                    if (selected.isEmpty()) {
                        JIPipeDataInfoRef ref = new JIPipeDataInfoRef();
                        setParameter(ref, true);
                    } else {
                        setParameter(new JIPipeDataInfoRef((JIPipeDataInfo) selected.get(0).getValue()), true);
                    }
                });
        add(selectionBox, BorderLayout.CENTER);
    }

    private void collectAvailableInfos() {
        availableInfos = new ArrayList<>();
        Class<? extends JIPipeData> baseClass = JIPipeData.class;
        ClassFilter classFilter = new AnyClassFilter();
        boolean showHidden = false;
        JIPipeDataParameterSettings settings = getParameterAccess().getAnnotationOfType(JIPipeDataParameterSettings.class);
        if (settings != null) {
            baseClass = settings.dataBaseClass();
            showHidden = settings.showHidden();
            classFilter = (ClassFilter) ReflectionUtils.newInstance(settings.dataClassFilter());
        }

        Set<JIPipeDataInfo> unique = new HashSet<>();
        for (Class<? extends JIPipeData> klass : JIPipe.getDataTypes().getRegisteredDataTypes().values()) {
            JIPipeDataInfo info = JIPipeDataInfo.getInstance(klass);
            if (info.isHidden() && !showHidden)
                continue;
            if (baseClass.isAssignableFrom(info.getDataClass()) && classFilter.test(info.getDataClass())) {
                unique.add(info);
            }
        }
        availableInfos.addAll(unique);
    }

    private List<JIPipeDesktopRichSelectionBox.Option<?>> options() {
        List<JIPipeDesktopRichSelectionBox.Option<?>> result = new ArrayList<>();
        for (JIPipeDataInfo info : availableInfos) {
            result.add(new JIPipeDesktopRichSelectionBox.Option<>(info,
                    info.getName(),
                    JIPipe.getDataTypes().getIconFor(info.getDataClass()),
                    info.getMenuPath(),
                    TooltipUtils.getDataTooltip(info)));
        }
        return result;
    }

    @Override
    public void reload() {
        if (selectionBox == null) {
            return;
        }
        JIPipeDataInfo info;
        try {
            info = getParameter().getInfo();
        } catch (NullPointerException ignored) {
            info = null;
        }
        if (info != null) {
            selectionBox.setSelectedValues(List.of(info));
        } else {
            selectionBox.setSelectedValues(List.of());
        }
        selectionBox.refreshDisplay();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }
}