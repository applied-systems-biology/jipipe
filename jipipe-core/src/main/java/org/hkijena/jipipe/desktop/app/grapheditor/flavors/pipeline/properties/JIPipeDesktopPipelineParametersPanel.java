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

package org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.properties;

import org.hkijena.jipipe.api.nodes.JIPipeAdaptiveParametersAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.desktop.api.nodes.JIPipeDesktopNodeQuickAction;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class JIPipeDesktopPipelineParametersPanel extends JIPipeDesktopWorkbenchPanel {

    private final JIPipeDesktopGraphCanvasUI canvas;
    private final JIPipeGraphNode node;
    private JIPipeDesktopParameterFormPanel parametersUI;

    public JIPipeDesktopPipelineParametersPanel(JIPipeDesktopWorkbench desktopWorkbench, JIPipeDesktopGraphCanvasUI canvas, JIPipeGraphNode node) {
        super(desktopWorkbench);
        this.canvas = canvas;
        this.node = node;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        // Parameters
        parametersUI = new JIPipeDesktopParameterFormPanel(getDesktopWorkbench(),
                node,
                TooltipUtils.getAlgorithmDocumentation(node),
                JIPipeDesktopParameterFormPanel.WITH_SCROLLING | JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION |
                        JIPipeDesktopParameterFormPanel.DOCUMENTATION_BELOW | JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR |
                        JIPipeDesktopFormPanel.DOCUMENTATION_EXTERNAL);
        add(parametersUI, BorderLayout.CENTER);

//        JToolBar toolBar = new JToolBar();
//        toolBar.setFloatable(false);
//        panel.add(toolBar, BorderLayout.NORTH);

        if (node instanceof JIPipeParameterSlotAlgorithm) {

            JButton menuButton = new JButton("External", ((JIPipeParameterSlotAlgorithm) node).getParameterSlotAlgorithmSettings().isHasParameterSlot() ?
                    UIUtils.getIconFromResources("emblems/checkbox-checked.png") :
                    UIUtils.getIconFromResources("emblems/checkbox-unchecked.png"));
            JPopupMenu popupMenu = UIUtils.addPopupMenuToButton(menuButton);

            JCheckBoxMenuItem toggle = new JCheckBoxMenuItem("Enable external parameters");
            toggle.setToolTipText("If enabled, the node will include an additional input 'Parameters' that receives parameter sets from an external source. " +
                    "If the parameter data contains multiple items, the node's workload will be repeated for each parameter set.");
            toggle.setSelected(((JIPipeParameterSlotAlgorithm) node).getParameterSlotAlgorithmSettings().isHasParameterSlot());
            toggle.addActionListener(e -> {
                ((JIPipeParameterSlotAlgorithm) node).getParameterSlotAlgorithmSettings().setParameter("has-parameter-slot", toggle.isSelected());
                menuButton.setIcon(toggle.isSelected() ?
                        UIUtils.getIconFromResources("emblems/checkbox-checked.png") :
                        UIUtils.getIconFromResources("emblems/checkbox-unchecked.png"));
            });
            popupMenu.add(toggle);
            popupMenu.addSeparator();

            popupMenu.add(UIUtils.createMenuItem("Configure", "Configure external parameters", UIUtils.getIconFromResources("actions/configure.png"), () -> {
                JIPipeDesktopParameterFormPanel.showDialog(getDesktopWorkbench(),
                        ((JIPipeParameterSlotAlgorithm) node).getParameterSlotAlgorithmSettings(),
                        MarkdownText.fromPluginResource("documentation/multi-parameters.md", Collections.emptyMap()),
                        "Configure external parameters",
                        JIPipeDesktopParameterFormPanel.DEFAULT_DIALOG_FLAGS);
            }));

            popupMenu.add(UIUtils.createMenuItem("What is this?", "Shows a help window", UIUtils.getIconFromResources("actions/help.png"), () -> {
                JIPipeDesktopMarkdownReader.showDialog(MarkdownText.fromPluginResource("documentation/multi-parameters.md", Collections.emptyMap()),
                        true,
                        "About external parameters",
                        this,
                        false);
            }));

//            toolBar.add(menuButton);
            parametersUI.getToolBar().add(menuButton);
        }

        if (node instanceof JIPipeAdaptiveParametersAlgorithm) {

            JButton menuButton = new JButton("Adaptive", ((JIPipeAdaptiveParametersAlgorithm) node).getAdaptiveParameterSettings().isEnabled() ?
                    UIUtils.getIconFromResources("emblems/checkbox-checked.png") :
                    UIUtils.getIconFromResources("emblems/checkbox-unchecked.png"));
            JPopupMenu popupMenu = UIUtils.addPopupMenuToButton(menuButton);

            JCheckBoxMenuItem toggle = new JCheckBoxMenuItem("Enable adaptive parameters");
            toggle.setToolTipText("If enabled, the node will support parameters that are calculated by expressions.");
            toggle.setSelected(((JIPipeAdaptiveParametersAlgorithm) node).getAdaptiveParameterSettings().isEnabled());
            toggle.addActionListener(e -> {
                ((JIPipeAdaptiveParametersAlgorithm) node).getAdaptiveParameterSettings().setParameter("enabled", toggle.isSelected());
                menuButton.setIcon(toggle.isSelected() ?
                        UIUtils.getIconFromResources("emblems/checkbox-checked.png") :
                        UIUtils.getIconFromResources("emblems/checkbox-unchecked.png"));
            });
            popupMenu.add(toggle);
            popupMenu.addSeparator();

            popupMenu.add(UIUtils.createMenuItem("Configure", "Configure external parameters", UIUtils.getIconFromResources("actions/configure.png"), () -> {
                JIPipeDesktopParameterFormPanel.showDialog(getDesktopWorkbench(),
                        ((JIPipeAdaptiveParametersAlgorithm) node).getAdaptiveParameterSettings(),
                        MarkdownText.fromPluginResource("documentation/adaptive-parameters.md", Collections.emptyMap()),
                        "Configure external parameters",
                        JIPipeDesktopParameterFormPanel.DEFAULT_DIALOG_FLAGS);
                node.getParameterUIChangedEventEmitter().emit(new JIPipeParameterCollection.ParameterUIChangedEvent(node));
            }));

            popupMenu.add(UIUtils.createMenuItem("What is this?", "Shows a help window", UIUtils.getIconFromResources("actions/help.png"), () -> {
                JIPipeDesktopMarkdownReader.showDialog(MarkdownText.fromPluginResource("documentation/adaptive-parameters.md", Collections.emptyMap()),
                        true,
                        "About external parameters",
                        this,
                        false);
            }));

//            toolBar.add(menuButton);
            parametersUI.getToolBar().add(menuButton);
        }

        // Create quick action menu
        List<JIPipeDesktopNodeQuickAction> quickActions = JIPipeDesktopNodeQuickAction.getQuickActions(node);
        if (!quickActions.isEmpty()) {
            JPopupMenu quickActionsMenu = new JPopupMenu();
            for (JIPipeDesktopNodeQuickAction quickAction : quickActions) {
                quickActionsMenu.add(UIUtils.createMenuItem(quickAction.getName(),
                        quickAction.getDescription(),
                        UIUtils.getIconFromResources(quickAction.getIcon()),
                        () -> quickAction.getWorkload().accept(node, canvas)));
            }
            JButton button = UIUtils.createButton("Tools", UIUtils.getIconFromResources("actions/quickopen.png"), null);
            UIUtils.makeButtonHighlightedSuccess(button);
            UIUtils.addPopupMenuToButton(button, quickActionsMenu);

            parametersUI.getToolBar().add(button);
        }
    }

    public JIPipeDesktopParameterFormPanel getParametersUI() {
        return parametersUI;
    }
}
