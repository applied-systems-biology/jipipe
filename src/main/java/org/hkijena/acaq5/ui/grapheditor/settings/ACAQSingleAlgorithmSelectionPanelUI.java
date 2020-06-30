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

package org.hkijena.acaq5.ui.grapheditor.settings;

import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.testbench.ACAQTestBenchSettings;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.cache.ACAQAlgorithmCacheBrowserUI;
import org.hkijena.acaq5.ui.compendium.ACAQAlgorithmCompendiumUI;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphEditorUI;
import org.hkijena.acaq5.ui.parameters.ParameterPanel;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.util.Collections;

/**
 * UI for a single {@link ACAQGraphNode}
 */
public class ACAQSingleAlgorithmSelectionPanelUI extends ACAQProjectWorkbenchPanel {
    private final ACAQAlgorithmGraphEditorUI graphEditorUI;
    private final ACAQAlgorithmGraphCanvasUI canvas;
    private final ACAQGraphNode algorithm;
    private JPanel testBenchTabContent;
    private JPanel cacheBrowserTabContent;
    private DocumentTabPane tabbedPane;

    /**
     * @param graphEditorUI the graph editor
     * @param algorithm     the algorithm
     */
    public ACAQSingleAlgorithmSelectionPanelUI(ACAQAlgorithmGraphEditorUI graphEditorUI, ACAQGraphNode algorithm) {
        super((ACAQProjectWorkbench) graphEditorUI.getWorkbench());
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
                TooltipUtils.getAlgorithmDocumentation(algorithm.getDeclaration()),
                ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_BELOW | ParameterPanel.WITH_SEARCH_BAR);
        tabbedPane.addTab("Parameters", UIUtils.getIconFromResources("cog.png"),
                parametersUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        ACAQSlotEditorUI slotEditorUI = new ACAQSlotEditorUI(graphEditorUI, algorithm);
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
                ACAQTestBenchSetupUI testBenchSetupUI = new ACAQTestBenchSetupUI(getProjectWorkbench(), algorithm);
                testBenchTabContent.add(testBenchSetupUI, BorderLayout.CENTER);
            }
        }
        if (cacheBrowserTabContent != null && tabbedPane.getCurrentContent() == cacheBrowserTabContent) {
            if (cacheBrowserTabContent.getComponentCount() == 0) {
                ACAQAlgorithmCacheBrowserUI browserUI = new ACAQAlgorithmCacheBrowserUI(getProjectWorkbench(), algorithm);
                cacheBrowserTabContent.add(browserUI, BorderLayout.CENTER);
            }
        }
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JLabel nameLabel = new JLabel(algorithm.getName(), new ColorIcon(16, 16, UIUtils.getFillColorFor(algorithm.getDeclaration())), JLabel.LEFT);
        nameLabel.setToolTipText(TooltipUtils.getAlgorithmTooltip(algorithm.getDeclaration()));
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        ACAQAlgorithmGraphEditorUI.installContextActionsInto(toolBar,
                canvas.getNodeUIsFor(Collections.singleton(algorithm)),
                canvas.getContextActions(),
                canvas);

        if (ACAQAlgorithmRegistry.getInstance().getRegisteredAlgorithms().containsValue(algorithm.getDeclaration())) {
            JButton openCompendiumButton = new JButton(UIUtils.getIconFromResources("help.png"));
            openCompendiumButton.setToolTipText("Open in algorithm compendium");
            openCompendiumButton.addActionListener(e -> {
                ACAQAlgorithmCompendiumUI compendiumUI = new ACAQAlgorithmCompendiumUI();
                compendiumUI.selectItem(algorithm.getDeclaration());
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
    public ACAQGraphNode getAlgorithm() {
        return algorithm;
    }

    /**
     * Activates and runs the quick run as automatically as possible.
     *
     * @param showResults show results after a successful run
     * @param showCache   show slot cache after a successful run
     */
    public void runTestBench(boolean showResults, boolean showCache) {
        // Activate the quick run
        tabbedPane.switchToContent(testBenchTabContent);
        ACAQTestBenchSetupUI testBenchSetupUI = (ACAQTestBenchSetupUI) testBenchTabContent.getComponent(0);
        boolean success = testBenchSetupUI.tryAutoRun(showResults, new ACAQTestBenchSettings(), testBench -> {
            if (showCache) {
                SwingUtilities.invokeLater(() -> tabbedPane.switchToContent(cacheBrowserTabContent));
            }
        });
        if (!success) {
            SwingUtilities.invokeLater(() -> tabbedPane.switchToContent(testBenchTabContent));
        }
    }
}
