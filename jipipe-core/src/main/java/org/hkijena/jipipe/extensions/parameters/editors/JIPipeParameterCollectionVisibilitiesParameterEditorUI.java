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

package org.hkijena.jipipe.extensions.parameters.editors;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollectionVisibilities;
import org.hkijena.jipipe.api.parameters.JIPipeParameterVisibility;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UI around {@link JIPipeParameterCollectionVisibilities}
 */
public class JIPipeParameterCollectionVisibilitiesParameterEditorUI extends JIPipeParameterEditorUI {

    private FormPanel formPanel;

    /**
     * Creates new instance
     *
     * @param workbench       workbench
     * @param parameterAccess Parameter
     */
    public JIPipeParameterCollectionVisibilitiesParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());

        formPanel = new FormPanel(null, FormPanel.NONE);
        add(formPanel, BorderLayout.CENTER);
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        formPanel.clear();
        JIPipeParameterCollectionVisibilities visibilities = getParameter(JIPipeParameterCollectionVisibilities.class);
        Map<String, JIPipeParameterAccess> parameters = visibilities.getAvailableParameters();
        Map<Object, List<String>> groupedByHolder = parameters.keySet().stream().collect(Collectors.groupingBy(key -> parameters.get(key).getSource()));

        for (Object parameterHolder : groupedByHolder.keySet()) {

            List<String> parameterIds = groupedByHolder.get(parameterHolder).stream().sorted().collect(Collectors.toList());
            parameterIds.removeIf(key -> {
                JIPipeParameterAccess parameterAccess = parameters.get(key);
                return parameterAccess.getVisibility() == JIPipeParameterVisibility.Hidden ||
                        parameterAccess.getVisibility() == JIPipeParameterVisibility.Visible;
            });

            if (parameterIds.isEmpty())
                continue;

            formPanel.addGroupHeader("", null);

            for (String key : parameterIds) {
                JIPipeParameterAccess parameterAccess = parameters.get(key);

                JIPipeParameterEditorUI ui = JIPipe.getInstance()
                        .getParameterTypeRegistry().createEditorFor(getWorkbench(), parameterAccess);

                JPanel labelPanel = new JPanel(new BorderLayout(8, 8));
                JToggleButton exportParameterToggle = new JToggleButton(UIUtils.getIconFromResources("actions/eye.png"));
                UIUtils.makeFlat25x25(exportParameterToggle);
                exportParameterToggle.setToolTipText("If enabled, the parameter can be changed by the user.");
                exportParameterToggle.setSelected(visibilities.isVisible(key));
                exportParameterToggle.setIcon(exportParameterToggle.isSelected() ? UIUtils.getIconFromResources("actions/eye.png") :
                        UIUtils.getIconFromResources("actions/eye-slash.png"));
                JIPipeParameterCollectionVisibilities finalVisibilities = visibilities;
                exportParameterToggle.addActionListener(e -> {
                    finalVisibilities.setVisibility(key, exportParameterToggle.isSelected());
                    exportParameterToggle.setIcon(exportParameterToggle.isSelected() ? UIUtils.getIconFromResources("actions/eye.png") :
                            UIUtils.getIconFromResources("actions/eye-slash.png"));
                });
                labelPanel.add(exportParameterToggle, BorderLayout.WEST);

                if (ui.isUILabelEnabled())
                    labelPanel.add(new JLabel(parameterAccess.getName()));

                formPanel.addToForm(ui, labelPanel, null);
            }
        }
    }
}
