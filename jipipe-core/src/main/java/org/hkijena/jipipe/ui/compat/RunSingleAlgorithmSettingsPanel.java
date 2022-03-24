package org.hkijena.jipipe.ui.compat;

import org.hkijena.jipipe.api.compat.SingleImageJAlgorithmRun;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.documentation.JIPipeAlgorithmCompendiumUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;

public class RunSingleAlgorithmSettingsPanel extends JIPipeWorkbenchPanel {

    private final JIPipeNodeInfo nodeInfo;
    private final JIPipeGraphNode node;
    private final SingleImageJAlgorithmRun run;

    public RunSingleAlgorithmSettingsPanel(JIPipeWorkbench workbench, JIPipeNodeInfo nodeInfo) {
        super(workbench);
        this.nodeInfo = nodeInfo;
        this.node = nodeInfo.newInstance();
        this.run = new SingleImageJAlgorithmRun(node);
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

    public SingleImageJAlgorithmRun getRun() {
        return run;
    }

    public JIPipeNodeInfo getNodeInfo() {
        return nodeInfo;
    }

    public JIPipeGraphNode getNode() {
        return node;
    }
}
