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

package org.hkijena.jipipe.ui.grapheditor.settings;

import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.registries.JIPipeNodeRegistry;
import org.hkijena.jipipe.api.testbench.JIPipeTestBenchSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.cache.JIPipeAlgorithmCacheBrowserUI;
import org.hkijena.jipipe.ui.compendium.JIPipeAlgorithmCompendiumUI;
import org.hkijena.jipipe.ui.components.ColorIcon;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphEditorUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;

/**
 * UI for a single {@link JIPipeGraphNode}
 */
public class JIPipeSingleAlgorithmSelectionPanelUI extends JIPipeProjectWorkbenchPanel {
    private final JIPipeGraphEditorUI graphEditorUI;
    private final JIPipeGraphCanvasUI canvas;
    private final JIPipeGraphNode algorithm;
    private JPanel testBenchTabContent;
    private JPanel cacheBrowserTabContent;
    private DocumentTabPane tabbedPane;

    /**
     * @param graphEditorUI the graph editor
     * @param algorithm     the algorithm
     */
    public JIPipeSingleAlgorithmSelectionPanelUI(JIPipeGraphEditorUI graphEditorUI, JIPipeGraphNode algorithm) {
        super((JIPipeProjectWorkbench) graphEditorUI.getWorkbench());
        this.graphEditorUI = graphEditorUI;
        this.canvas = graphEditorUI.getCanvasUI();
        this.algorithm = algorithm;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        tabbedPane = new DocumentTabPane();

        ParameterPanel parametersUI = new ParameterPanel(getProjectWorkbench(),
                algorithm,
                TooltipUtils.getAlgorithmDocumentation(algorithm.getInfo()),
                ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_BELOW | ParameterPanel.WITH_SEARCH_BAR);
        tabbedPane.addTab("Parameters", UIUtils.getIconFromResources("cog.png"),
                parametersUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        JIPipeSlotEditorUI slotEditorUI = new JIPipeSlotEditorUI(graphEditorUI, algorithm);
        tabbedPane.addTab("Slots", UIUtils.getIconFromResources("plug.png"),
                slotEditorUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        cacheBrowserTabContent = new JPanel(new BorderLayout());
        tabbedPane.addTab("Cache browser", UIUtils.getIconFromResources("database.png"),
                cacheBrowserTabContent,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        testBenchTabContent = new JPanel(new BorderLayout());
        tabbedPane.addTab("Quick run", UIUtils.getIconFromResources("play.png"),
                testBenchTabContent,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);


        add(tabbedPane, BorderLayout.CENTER);

        tabbedPane.getTabbedPane().addChangeListener(e -> activateLazyContent(tabbedPane));

        initializeToolbar();
    }

    private void activateLazyContent(DocumentTabPane tabbedPane) {
        if (testBenchTabContent != null && tabbedPane.getCurrentContent() == testBenchTabContent) {
            if (testBenchTabContent.getComponentCount() == 0) {
                JIPipeTestBenchSetupUI testBenchSetupUI = new JIPipeTestBenchSetupUI(getProjectWorkbench(), algorithm);
                testBenchTabContent.add(testBenchSetupUI, BorderLayout.CENTER);
            }
        }
        if (cacheBrowserTabContent != null && tabbedPane.getCurrentContent() == cacheBrowserTabContent) {
            if (cacheBrowserTabContent.getComponentCount() == 0) {
                JIPipeAlgorithmCacheBrowserUI browserUI = new JIPipeAlgorithmCacheBrowserUI(getProjectWorkbench(), algorithm);
                cacheBrowserTabContent.add(browserUI, BorderLayout.CENTER);
            }
        }
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JLabel nameLabel = new JLabel(algorithm.getName(), new ColorIcon(16, 16, UIUtils.getFillColorFor(algorithm.getInfo())), JLabel.LEFT);
        nameLabel.setToolTipText(TooltipUtils.getAlgorithmTooltip(algorithm.getInfo()));
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        JIPipeGraphEditorUI.installContextActionsInto(toolBar,
                canvas.getNodeUIsFor(Collections.singleton(algorithm)),
                canvas.getContextActions(),
                canvas);

        if (JIPipeNodeRegistry.getInstance().getRegisteredNodeInfos().containsValue(algorithm.getInfo())) {
            JButton openCompendiumButton = new JButton(UIUtils.getIconFromResources("help.png"));
            openCompendiumButton.setToolTipText("Open in algorithm compendium");
            openCompendiumButton.addActionListener(e -> {
                JIPipeAlgorithmCompendiumUI compendiumUI = new JIPipeAlgorithmCompendiumUI();
                compendiumUI.selectItem(algorithm.getInfo());
                getWorkbench().getDocumentTabPane().addTab("Algorithm compendium",
                        UIUtils.getIconFromResources("help.png"),
                        compendiumUI,
                        DocumentTabPane.CloseMode.withSilentCloseButton,
                        true);
                getWorkbench().getDocumentTabPane().switchToLastTab();
            });
            toolBar.add(openCompendiumButton);
        }

        add(toolBar, BorderLayout.NORTH);
    }

    /**
     * @return the algorithm
     */
    public JIPipeGraphNode getAlgorithm() {
        return algorithm;
    }

    /**
     * Activates and runs the quick run as automatically as possible.
     *  @param showResults show results after a successful run
     * @param showCache   show slot cache after a successful run
     * @param saveOutputs if the run should save outputs
     */
    public void runTestBench(boolean showResults, boolean showCache, boolean saveOutputs) {
        // Activate the quick run
        tabbedPane.switchToContent(testBenchTabContent);
        JIPipeTestBenchSetupUI testBenchSetupUI = (JIPipeTestBenchSetupUI) testBenchTabContent.getComponent(0);
        JIPipeTestBenchSettings settings = new JIPipeTestBenchSettings();
        settings.setSaveOutputs(saveOutputs);
        boolean success = testBenchSetupUI.tryAutoRun(showResults, settings, testBench -> {
            if (showCache) {
                SwingUtilities.invokeLater(() -> tabbedPane.switchToContent(cacheBrowserTabContent));
            }
        });
        if (!success) {
            SwingUtilities.invokeLater(() -> tabbedPane.switchToContent(testBenchTabContent));
        }
    }
}
