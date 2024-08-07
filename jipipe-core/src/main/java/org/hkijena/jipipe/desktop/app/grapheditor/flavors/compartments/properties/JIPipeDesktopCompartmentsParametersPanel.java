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

package org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.properties;

import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeAdaptiveParametersAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.desktop.api.nodes.JIPipeDesktopNodeQuickAction;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
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

public class JIPipeDesktopCompartmentsParametersPanel extends JIPipeDesktopProjectWorkbenchPanel {

    private final JIPipeDesktopGraphCanvasUI canvas;
    private final JIPipeGraphNode node;
    private JIPipeDesktopParameterFormPanel parametersUI;

    public JIPipeDesktopCompartmentsParametersPanel(JIPipeDesktopProjectWorkbench desktopWorkbench, JIPipeDesktopGraphCanvasUI canvas, JIPipeGraphNode node) {
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

        if(node instanceof JIPipeProjectCompartment) {
            // Edit contents button
            JButton editContentsButton = UIUtils.createButton("Edit contents", UIUtils.getIconFromResources("actions/edit.png"), this::editContents);
            UIUtils.makeButtonHighlightedSuccess(editContentsButton);
            parametersUI.getToolBar().add(editContentsButton);
        }
    }

    private void editContents() {
        getDesktopProjectWorkbench().getOrOpenPipelineEditorTab((JIPipeProjectCompartment)node, true);
    }

    public JIPipeDesktopParameterFormPanel getParametersUI() {
        return parametersUI;
    }
}
