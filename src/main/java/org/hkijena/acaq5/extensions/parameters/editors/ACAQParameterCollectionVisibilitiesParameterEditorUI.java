package org.hkijena.acaq5.extensions.parameters.editors;

import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollectionVisibilities;
import org.hkijena.acaq5.api.parameters.ACAQParameterVisibility;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UI around {@link ACAQParameterCollectionVisibilities}
 */
public class ACAQParameterCollectionVisibilitiesParameterEditorUI extends ACAQParameterEditorUI {

    private FormPanel formPanel;

    /**
     * Creates new instance
     *
     * @param workbench        workbench
     * @param parameterAccess Parameter
     */
    public ACAQParameterCollectionVisibilitiesParameterEditorUI(ACAQWorkbench workbench, ACAQParameterAccess parameterAccess) {
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
        ACAQParameterCollectionVisibilities visibilities = getParameter(ACAQParameterCollectionVisibilities.class);
        Map<String, ACAQParameterAccess> parameters = visibilities.getAvailableParameters();
        Map<Object, List<String>> groupedByHolder = parameters.keySet().stream().collect(Collectors.groupingBy(key -> parameters.get(key).getSource()));

        for (Object parameterHolder : groupedByHolder.keySet()) {

            List<String> parameterIds = groupedByHolder.get(parameterHolder).stream().sorted().collect(Collectors.toList());
            parameterIds.removeIf(key -> {
                ACAQParameterAccess parameterAccess = parameters.get(key);
                return parameterAccess.getVisibility() == ACAQParameterVisibility.Hidden ||
                        parameterAccess.getVisibility() == ACAQParameterVisibility.Visible;
            });

            if (parameterIds.isEmpty())
                continue;

            formPanel.addGroupHeader("", null);

            for (String key : parameterIds) {
                ACAQParameterAccess parameterAccess = parameters.get(key);

                ACAQParameterEditorUI ui = ACAQDefaultRegistry.getInstance()
                        .getUIParameterTypeRegistry().createEditorFor(getWorkbench(), parameterAccess);

                JPanel labelPanel = new JPanel(new BorderLayout(8, 8));
                JToggleButton exportParameterToggle = new JToggleButton(UIUtils.getIconFromResources("eye.png"));
                UIUtils.makeFlat25x25(exportParameterToggle);
                exportParameterToggle.setToolTipText("If enabled, the parameter can be changed by the user.");
                exportParameterToggle.setSelected(visibilities.isVisible(key));
                exportParameterToggle.setIcon(exportParameterToggle.isSelected() ? UIUtils.getIconFromResources("eye.png") : UIUtils.getIconFromResources("eye-slash.png"));
                ACAQParameterCollectionVisibilities finalVisibilities = visibilities;
                exportParameterToggle.addActionListener(e -> {
                    finalVisibilities.setVisibility(key, exportParameterToggle.isSelected());
                    exportParameterToggle.setIcon(exportParameterToggle.isSelected() ? UIUtils.getIconFromResources("eye.png") : UIUtils.getIconFromResources("eye-slash.png"));
                });
                labelPanel.add(exportParameterToggle, BorderLayout.WEST);

                if (ui.isUILabelEnabled())
                    labelPanel.add(new JLabel(parameterAccess.getName()));

                formPanel.addToForm(ui, labelPanel, null);
            }
        }
    }
}
