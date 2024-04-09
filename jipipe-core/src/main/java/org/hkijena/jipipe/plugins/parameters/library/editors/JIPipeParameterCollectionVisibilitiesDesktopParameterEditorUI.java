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

package org.hkijena.jipipe.plugins.parameters.library.editors;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollectionVisibilities;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UI around {@link JIPipeParameterCollectionVisibilities}
 */
public class JIPipeParameterCollectionVisibilitiesDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    private JIPipeDesktopFormPanel formPanel;

    /**
     * Creates new instance
     *
     * @param workbench       workbench
     * @param parameterAccess Parameter
     */
    public JIPipeParameterCollectionVisibilitiesDesktopParameterEditorUI(JIPipeDesktopWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(UIUtils.createControlBorder());

        formPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.NONE);
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
                return parameterAccess.isHidden();
            });

            if (parameterIds.isEmpty())
                continue;

            formPanel.addGroupHeader("", null);

            for (String key : parameterIds) {
                JIPipeParameterAccess parameterAccess = parameters.get(key);

                JIPipeDesktopParameterEditorUI ui = JIPipe.getInstance()
                        .getParameterTypeRegistry().createEditorFor(getDesktopWorkbench(), new JIPipeParameterTree(parameterAccess), parameterAccess);

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
