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

package org.hkijena.jipipe.ui.compartments;

import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.MarkdownReader;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphEditorUI;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * UI when multiple {@link JIPipeProjectCompartment} instances are selected
 */
public class JIPipeMultiCompartmentSelectionPanelUI extends JIPipeProjectWorkbenchPanel {
    private final JIPipeGraphCanvasUI canvas;
    private Set<JIPipeProjectCompartment> compartments;

    /**
     * @param workbenchUI  The workbench UI
     * @param compartments The compartment selection
     * @param canvas       the graph canvas
     */
    public JIPipeMultiCompartmentSelectionPanelUI(JIPipeProjectWorkbench workbenchUI, Set<JIPipeProjectCompartment> compartments, JIPipeGraphCanvasUI canvas) {
        super(workbenchUI);
        this.compartments = compartments;
        this.canvas = canvas;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        MarkdownReader content = new MarkdownReader(false);
        add(content, BorderLayout.CENTER);

        StringBuilder markdownContent = new StringBuilder();
        for (JIPipeProjectCompartment compartment : compartments.stream().sorted(Comparator.comparing(JIPipeGraphNode::getName)).collect(Collectors.toList())) {
            markdownContent.append(TooltipUtils.getProjectCompartmentTooltip(compartment, getProject().getGraph())
                    .replace("<html>", "<div style=\"border: 1px solid gray; border-radius: 4px; margin: 4px; padding: 4px;\">")
                    .replace("</html>", "</div>"));
            markdownContent.append("\n\n");
        }
        content.setDocument(new MarkdownDocument(markdownContent.toString()));
        initializeToolbar();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JLabel nameLabel = new JLabel(compartments.size() + " compartments", UIUtils.getIconFromResources("actions/edit-select-all.png"), JLabel.LEFT);
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        JIPipeGraphEditorUI.installContextActionsInto(toolBar,
                canvas.getNodeUIsFor(compartments),
                canvas.getContextActions(),
                canvas);

        JButton openButton = new JButton("Open in editor", UIUtils.getIconFromResources("actions/edit.png"));
        openButton.addActionListener(e -> openInEditor());
        toolBar.add(openButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void openInEditor() {
        for (JIPipeProjectCompartment compartment : compartments) {
            getProjectWorkbench().openCompartmentGraph(compartment, true);
        }
    }
}
