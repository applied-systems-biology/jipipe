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

package org.hkijena.jipipe.extensions.parameters.primitives;

import com.google.common.collect.Comparators;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.MultiSelectionModel;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.ModernMetalTheme;
import org.hkijena.jipipe.utils.ReflectionUtils;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

/**
 * A parameter editor UI that works for all enumerations
 */
public class DynamicSetParameterEditorUI extends JIPipeParameterEditorUI {

    private JList<Object> jList;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public DynamicSetParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        DynamicSetParameter<Object> parameter = getParameter(DynamicSetParameter.class);
       Set<Object> currentlySelected = new HashSet<>(jList.getSelectedValuesList());
       if(!currentlySelected.equals(parameter.getValues())) {
           TIntList indices = new TIntArrayList();
           List<Object> inModel = new ArrayList<>();
           for (int i = 0; i < jList.getModel().getSize(); i++) {
               inModel.add(jList.getModel().getElementAt(i));
           }
           for (Object value : parameter.getValues()) {
               int i = inModel.indexOf(value);
               if(i >= 0) {
                   indices.add(i);
               }
           }
            jList.setSelectedIndices(indices.toArray());
       }
    }


    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(ModernMetalTheme.MEDIUM_GRAY));

        DynamicSetParameter<Object> parameter = getParameter(DynamicSetParameter.class);
        Object[] values;
        if (parameter.getAllowedValues() != null) {
            values = parameter.getAllowedValues().toArray();
        } else {
            DynamicSetParameterSettings settings = getParameterAccess().getAnnotationOfType(DynamicSetParameterSettings.class);
            if (settings != null) {
                Supplier<List<Object>> supplier = (Supplier<List<Object>>) ReflectionUtils.newInstance(settings.supplier());
                values = supplier.get().toArray();
            } else {
                values = new Object[0];
                System.err.println("In " + this + ": " + getParameterAccess().getFieldClass() + " not provided with a generator supplier!");
            }
        }
        Arrays.sort(values, Comparator.comparing(parameter::renderLabel));
        jList = new JList<>(values);
//        jList.setSelectionModel(new MultiSelectionModel());
        jList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        reload();
        jList.addListSelectionListener(e -> {
            parameter.getValues().clear();
            parameter.getValues().addAll(jList.getSelectedValuesList());
            setParameter(parameter, false);
        });
        jList.setCellRenderer(new Renderer(parameter));
        add(jList, BorderLayout.CENTER);
    }

    /**
     * Renders items in enum parameters
     */
    private static class Renderer extends JLabel implements ListCellRenderer<Object> {

        private final DynamicSetParameter<Object> parameter;

        public Renderer(DynamicSetParameter<Object> parameter) {
            this.parameter = parameter;
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            setIcon(parameter.renderIcon(value));
            setText(parameter.renderLabel(value));
            setToolTipText(parameter.renderTooltip(value));
            if (isSelected || cellHasFocus) {
                setBackground(new Color(184, 207, 229));
            } else {
                setBackground(new Color(255, 255, 255));
            }
            return this;
        }
    }
}
