package org.hkijena.acaq5.extensions.standardparametereditors.ui.parametereditors;

import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollectionVisibilities;
import org.hkijena.acaq5.api.parameters.ACAQParameterVisibility;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ACAQParameterCollectionVisibilitiesParameterEditorUI extends ACAQParameterEditorUI {

    private FormPanel formPanel;

    public ACAQParameterCollectionVisibilitiesParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
        super(context, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());

        formPanel = new FormPanel(null, false, false, false);
        add(formPanel, BorderLayout.CENTER);
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        formPanel.clear();
        ACAQParameterCollectionVisibilities visibilities = getParameterAccess().get();
        Map<String, ACAQParameterAccess> parameters = visibilities.getAvailableParameters();
        Map<Object, List<String>> groupedByHolder = parameters.keySet().stream().collect(Collectors.groupingBy(key -> parameters.get(key).getParameterHolder()));

        for (Object parameterHolder : groupedByHolder.keySet()) {

            List<String> parameterIds = groupedByHolder.get(parameterHolder).stream().sorted().collect(Collectors.toList());
            parameterIds.removeIf(key -> {
                ACAQParameterAccess parameterAccess = parameters.get(key);
                return parameterAccess.getVisibility() == ACAQParameterVisibility.Hidden ||
                        parameterAccess.getVisibility() == ACAQParameterVisibility.Visible;
            });

            if (parameterIds.isEmpty())
                continue;

            boolean foundHolderName = false;

            JPanel subAlgorithmGroupTitle = new JPanel(new BorderLayout());

            subAlgorithmGroupTitle.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(8, 0, 4, 0),
                    BorderFactory.createMatteBorder(1, 0, 0, 0, Color.DARK_GRAY)),
                    BorderFactory.createEmptyBorder(4, 4, 4, 4)
            ));
            JLabel holderNameLabel = new JLabel();
            subAlgorithmGroupTitle.add(holderNameLabel, BorderLayout.CENTER);
            formPanel.addToForm(subAlgorithmGroupTitle, null);

            for (String key : parameterIds) {
                ACAQParameterAccess parameterAccess = parameters.get(key);

                if (!foundHolderName) {
                    holderNameLabel.setText(parameterAccess.getHolderName());
                    holderNameLabel.setIcon(UIUtils.getIconFromResources("cog.png"));
                    foundHolderName = true;
                }

                ACAQParameterEditorUI ui = ACAQDefaultRegistry.getInstance()
                        .getUIParametertypeRegistry().createEditorFor(getContext(), parameterAccess);

                JPanel labelPanel = new JPanel(new BorderLayout(8, 8));
                JToggleButton exportParameterToggle = new JToggleButton(UIUtils.getIconFromResources("eye.png"));
                UIUtils.makeFlat25x25(exportParameterToggle);
                exportParameterToggle.setToolTipText("If enabled, the parameter can be changed by the user.");
                exportParameterToggle.setSelected(visibilities.isVisible(key));
                exportParameterToggle.setIcon(exportParameterToggle.isSelected() ? UIUtils.getIconFromResources("eye.png") : UIUtils.getIconFromResources("eye-slash.png"));
                exportParameterToggle.addActionListener(e -> {
                    visibilities.setVisibility(key, exportParameterToggle.isSelected());
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
