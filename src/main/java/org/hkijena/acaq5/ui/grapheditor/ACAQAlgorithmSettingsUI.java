package org.hkijena.acaq5.ui.grapheditor;

import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.ACAQAlgorithm;
import org.hkijena.acaq5.api.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.ACAQParameterAccess;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.stream.Collectors;

public class ACAQAlgorithmSettingsUI extends JPanel {
    private ACAQAlgorithmGraph graph;
    private ACAQAlgorithm algorithm;

    public ACAQAlgorithmSettingsUI(ACAQAlgorithmGraph graph, ACAQAlgorithm algorithm) {
        this.graph = graph;
        this.algorithm = algorithm;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        DocumentTabPane tabbedPane = new DocumentTabPane();

        FormPanel formPanel = new FormPanel("documentation/algorithm-graph.md", true);
        initializeParameterPanel(formPanel);
        tabbedPane.addTab("Parameters", UIUtils.getIconFromResources("cog.png"),
                formPanel,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        ACAQSlotEditorUI slotEditorUI = new ACAQSlotEditorUI(algorithm);
        tabbedPane.addTab("Slots", UIUtils.getIconFromResources("database.png"),
                slotEditorUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        add(tabbedPane, BorderLayout.CENTER);

        initializeToolbar();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        JLabel nameLabel = new JLabel(algorithm.getName(), new ColorIcon(16, 16, algorithm.getCategory().getColor(0.1f, 0.9f)), JLabel.LEFT);
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        JButton deleteButton = new JButton(UIUtils.getIconFromResources("delete.png"));
        deleteButton.setToolTipText("Delete algorithm");
        deleteButton.addActionListener(e -> deleteAlgorithm());
        toolBar.add(deleteButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void deleteAlgorithm() {
        graph.removeNode(algorithm);
    }

    private void initializeParameterPanel(FormPanel formPanel) {
        Map<String, ACAQParameterAccess> parameters = ACAQParameterAccess.getParameters(algorithm);
        for(String key : parameters.keySet().stream().sorted().collect(Collectors.toList())) {
            ACAQParameterAccess parameterAccess = parameters.get(key);
            ACAQParameterEditorUI ui = ACAQRegistryService.getInstance()
                    .getUIParametertypeRegistry().createEditorFor(parameterAccess);

            if(ui.isUILabelEnabled())
                formPanel.addToForm(ui, new JLabel(parameterAccess.getName()), null);
            else
                formPanel.addToForm(ui, null);
        }
        formPanel.addVerticalGlue();
    }

    public ACAQAlgorithm getAlgorithm() {
        return algorithm;
    }
}
