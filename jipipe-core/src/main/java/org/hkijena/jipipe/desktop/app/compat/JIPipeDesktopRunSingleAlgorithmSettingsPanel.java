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

package org.hkijena.jipipe.desktop.app.compat;

import org.hkijena.jipipe.api.compat.SingleImageJAlgorithmRunConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.documentation.JIPipeDesktopAlgorithmCompendiumUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.awt.*;

public class JIPipeDesktopRunSingleAlgorithmSettingsPanel extends JIPipeDesktopWorkbenchPanel {

    private final JIPipeNodeInfo nodeInfo;
    private final JIPipeGraphNode node;
    private final SingleImageJAlgorithmRunConfiguration run;

    public JIPipeDesktopRunSingleAlgorithmSettingsPanel(JIPipeDesktopWorkbench workbench, JIPipeNodeInfo nodeInfo) {
        super(workbench);
        this.nodeInfo = nodeInfo;
        this.node = nodeInfo.newInstance();
        this.run = new SingleImageJAlgorithmRunConfiguration(node);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JIPipeDesktopTabPane tabPane = new JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.TabPlacement.Top);
        add(tabPane, BorderLayout.CENTER);

        JIPipeDesktopAlgorithmCompendiumUI algorithmCompendiumUI = new JIPipeDesktopAlgorithmCompendiumUI();

        tabPane.addTab("Documentation",
                UIUtils.getIconFromResources("actions/help.png"),
                new JIPipeDesktopMarkdownReader(true, algorithmCompendiumUI.generateCompendiumFor(nodeInfo, true)),
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
        JIPipeDesktopRunSingleAlgorithmSettingsPanelIOEditor ioEditor = new JIPipeDesktopRunSingleAlgorithmSettingsPanelIOEditor(this);
        tabPane.addTab("Data inputs/outputs", UIUtils.getIconFromResources("actions/database.png"), ioEditor, JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
        tabPane.addTab("Parameters",
                UIUtils.getIconFromResources("actions/configuration.png"),
                new JIPipeDesktopParameterFormPanel(getDesktopWorkbench(),
                        node,
                        TooltipUtils.getAlgorithmDocumentation(node),
                        JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR | JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterFormPanel.WITH_SCROLLING),
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
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
