package org.hkijena.acaq5.ui.grapheditor.settings;

import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.stream.Collectors;

public class ACAQAlgorithmSettingsUI extends ACAQUIPanel {
    private ACAQAlgorithmGraph graph;
    private ACAQAlgorithm algorithm;

    public ACAQAlgorithmSettingsUI(ACAQWorkbenchUI workbenchUI, ACAQAlgorithmGraph graph, ACAQAlgorithm algorithm) {
        super(workbenchUI);
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

        ACAQTraitEditorUI traitEditorUI = new ACAQTraitEditorUI(algorithm, graph);
        tabbedPane.addTab("Annotations", UIUtils.getIconFromResources("label.png"),
                traitEditorUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        if(algorithm.getCategory() != ACAQAlgorithmCategory.Internal) {
            ACAQTestBenchSetupUI testBenchSetupUI = new ACAQTestBenchSetupUI(getWorkbenchUI(), algorithm);
            tabbedPane.addTab("Testbench", UIUtils.getIconFromResources("testbench.png"),
                    testBenchSetupUI,
                    DocumentTabPane.CloseMode.withoutCloseButton,
                    false);
        }

        add(tabbedPane, BorderLayout.CENTER);

        initializeToolbar();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        JLabel nameLabel = new JLabel(algorithm.getName(), new ColorIcon(16, 16, algorithm.getCategory().getColor(0.1f, 0.9f)), JLabel.LEFT);
        nameLabel.setToolTipText(TooltipUtils.getAlgorithmTooltip(algorithm.getClass()));
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
        if(!parameters.isEmpty()) {
            for(String key : parameters.keySet().stream().sorted().collect(Collectors.toList())) {
                ACAQParameterAccess parameterAccess = parameters.get(key);
                ACAQParameterEditorUI ui = ACAQRegistryService.getInstance()
                        .getUIParametertypeRegistry().createEditorFor(parameterAccess);

                if(ui.isUILabelEnabled())
                    formPanel.addToForm(ui, new JLabel(parameterAccess.getName()), null);
                else
                    formPanel.addToForm(ui, null);
            }
        }
        else {
            formPanel.addToForm(new JLabel("This algorithm has no parameters",
                    UIUtils.getIconFromResources("info.png"), JLabel.LEFT),
                    null);
        }
        formPanel.addVerticalGlue();
    }

    public ACAQAlgorithm getAlgorithm() {
        return algorithm;
    }
}
