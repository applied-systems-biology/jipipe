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

package org.hkijena.jipipe.desktop.commons.components.pickers;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeParameterTypeInfoListCellRenderer;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Panel that allows to pick {@link JIPipeNodeInfo}
 */
public class JIPipeDesktopParameterTypeInfoPicker extends JIPipeDesktopPickerDialog<JIPipeParameterTypeInfo> {
    public JIPipeDesktopParameterTypeInfoPicker(Window parent) {
        super(parent);
        setCellRenderer(new JIPipeParameterTypeInfoListCellRenderer());
        ArrayList<JIPipeParameterTypeInfo> infos = new ArrayList<>(JIPipe.getParameterTypes().getRegisteredParameters().values());
        infos.sort(Comparator.comparing(JIPipeParameterTypeInfo::getName));
        setAvailableItems(infos);
    }

    public JIPipeDesktopParameterTypeInfoPicker(Window parent, Set<Class<?>> allowedParameterTypes) {
        super(parent);
        setCellRenderer(new JIPipeParameterTypeInfoListCellRenderer());
        ArrayList<JIPipeParameterTypeInfo> infos;
        if (allowedParameterTypes == null || allowedParameterTypes.isEmpty()) {
            infos = new ArrayList<>(JIPipe.getParameterTypes().getRegisteredParameters().values());
            infos.sort(Comparator.comparing(JIPipeParameterTypeInfo::getName));
        } else {
            infos = allowedParameterTypes.stream().map(klass -> JIPipe.getParameterTypes().getInfoByFieldClass(klass)).sorted(Comparator.comparing(JIPipeParameterTypeInfo::getName)).collect(Collectors.toCollection(ArrayList::new));
        }
        setAvailableItems(infos);
    }

    @Override
    protected String getSearchString(JIPipeParameterTypeInfo item) {
        return item.getName() + item.getDescription();
    }
}
