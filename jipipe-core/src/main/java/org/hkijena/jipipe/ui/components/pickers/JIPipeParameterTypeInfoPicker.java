/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.components.pickers;

import com.google.common.eventbus.EventBus;
import com.google.common.primitives.Ints;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.ui.components.MultiSelectionModel;
import org.hkijena.jipipe.ui.components.SingleSelectionModel;
import org.hkijena.jipipe.ui.components.renderers.JIPipeParameterTypeInfoListCellRenderer;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Panel that allows to pick {@link JIPipeNodeInfo}
 */
public class JIPipeParameterTypeInfoPicker extends PickerDialog<JIPipeParameterTypeInfo> {
    public JIPipeParameterTypeInfoPicker(Window parent) {
        super(parent);
        setCellRenderer(new JIPipeParameterTypeInfoListCellRenderer());
        ArrayList<JIPipeParameterTypeInfo> infos = new ArrayList<>(JIPipe.getParameterTypes().getRegisteredParameters().values());
        infos.sort(Comparator.comparing(JIPipeParameterTypeInfo::getName));
        setAvailableItems(infos);
    }

    @Override
    protected String getSearchString(JIPipeParameterTypeInfo item) {
        return item.getName() + item.getDescription();
    }
}
