package org.hkijena.jipipe.ui.compat;

import org.hkijena.jipipe.api.compat.SingleImageJAlgorithmRunConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.documentation.JIPipeAlgorithmCompendiumUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.awt.BorderLayout;

public class RunSingleAlgorithmSettingsPanel extends JIPipeWorkbenchPanel {

    private final JIPipeNodeInfo nodeInfo;
    private final JIPipeGraphNode node;
    private final SingleImageJAlgorithmRunConfiguration run;

    public RunSingleAlgorithmSettingsPanel(JIPipeWorkbench workbench, JIPipeNodeInfo nodeInfo) {
        super(workbench);
        this.nodeInfo = nodeInfo;
        this.node = nodeInfo.newInstance();
        this.run = new SingleImageJAlgorithmRunConfiguration(node);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        DocumentTabPane tabPane = new DocumentTabPane();
        add(tabPane, BorderLayout.CENTER);

        JIPipeAlgorithmCompendiumUI algorithmCompendiumUI = new JIPipeAlgorithmCompendiumUI();

        tabPane.addTab("Documentation",
                UIUtils.getIconFromResources("actions/help.png"),
                new MarkdownReader(true, algorithmCompendiumUI.generateCompendiumFor(nodeInfo, true)),
                DocumentTabPane.CloseMode.withoutCloseButton);
        RunSingleAlgorithmSettingsPanelIOEditor ioEditor = new RunSingleAlgorithmSettingsPanelIOEditor(this);
        tabPane.addTab("Data inputs/outputs", UIUtils.getIconFromResources("actions/database.png"), ioEditor, DocumentTabPane.CloseMode.withoutCloseButton);
        tabPane.addTab("Parameters",
                UIUtils.getIconFromResources("actions/configuration.png"),
                new ParameterPanel(getWorkbench(),
                        node,
                        TooltipUtils.getAlgorithmDocumentation(node),
                        ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.WITH_SCROLLING),
                DocumentTabPane.CloseMode.withoutCloseButton);
    }

    public SingleImageJAlgorithmRunConfiguration getRun() {
        return run;
    }

    public JIPipeNodeInfo getNodeInfo() {
        return nodeInfo;
    }

    public JIPipeGraphNode getNode() {
        return node;
    }
}
