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

package org.hkijena.jipipe.desktop.app.grapheditor.compartments.properties;

import com.google.common.collect.ImmutableSet;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.bookmarks.JIPipeDesktopBookmarkListPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorMinimap;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.history.JIPipeDesktopHistoryJournalUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * UI when multiple {@link JIPipeProjectCompartment} instances are selected
 */
public class JIPipeDesktopMultiCompartmentSelectionPanelUI extends JIPipeDesktopProjectWorkbenchPanel {
    private final JIPipeDesktopGraphCanvasUI canvas;
    private final Set<JIPipeProjectCompartment> compartments;

    /**
     * @param workbenchUI  The workbench UI
     * @param compartments The compartment selection
     * @param canvas       the graph canvas
     */
    public JIPipeDesktopMultiCompartmentSelectionPanelUI(JIPipeDesktopProjectWorkbench workbenchUI, Set<JIPipeProjectCompartment> compartments, JIPipeDesktopGraphCanvasUI canvas) {
        super(workbenchUI);
        this.compartments = compartments;
        this.canvas = canvas;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(AutoResizeSplitPane.TOP_BOTTOM, AutoResizeSplitPane.RATIO_1_TO_3);
        add(splitPane, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new BorderLayout());
        JIPipeDesktopTabPane tabPane = new JIPipeDesktopTabPane(false, JIPipeDesktopTabPane.TabPlacement.Right);

        tabPane.addTab("Selection", UIUtils.getIcon32FromResources("actions/edit-select-all.png"),
                actionPanel, JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        tabPane.addTab("Bookmarks", UIUtils.getIcon32FromResources("actions/bookmarks.png"),
                new JIPipeDesktopBookmarkListPanel(getDesktopWorkbench(), canvas.getGraph(), canvas.getGraphEditorUI(), new HashSet<>(compartments)), JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        tabPane.addTab("Journal",
                UIUtils.getIcon32FromResources("actions/edit-undo-history.png"),
                new JIPipeDesktopHistoryJournalUI(canvas.getHistoryJournal()),
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        splitPane.setBottomComponent(tabPane);
        splitPane.setTopComponent(new JIPipeDesktopGraphEditorMinimap(canvas.getGraphEditorUI()));

        initializeToolbar(actionPanel);
        initializeActionPanel(actionPanel);
    }

    private void initializeActionPanel(JPanel actionPanel) {
        JIPipeDesktopFormPanel content = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        Set<JIPipeDesktopGraphNodeUI> nodeUIs = canvas.getNodeUIsFor(compartments);
        boolean canAddSeparator = false;
        for (NodeUIContextAction action : canvas.getContextActions()) {
            if (action == null) {
                if (canAddSeparator) {
                    content.addWideToForm(new JSeparator());
                    canAddSeparator = false;
                }
                continue;
            }
            if (action.isHidden())
                continue;
            if (!action.showInMultiSelectionPanel())
                continue;
            boolean matches = action.matches(nodeUIs);
            if (!matches && !action.disableOnNonMatch())
                continue;

            JButton item = new JButton("<html>" + action.getName() + "<br/><small>" + action.getDescription() + "</small></html>", action.getIcon());
            item.setHorizontalAlignment(SwingConstants.LEFT);
            item.setToolTipText(action.getDescription());
            if (matches) {
                item.addActionListener(e -> action.run(canvas, ImmutableSet.copyOf(nodeUIs)));
                content.addWideToForm(item);
                canAddSeparator = true;
            }
        }
        content.addVerticalGlue();
        actionPanel.add(content, BorderLayout.CENTER);
    }

    private void initializeToolbar(JPanel actionPanel) {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JLabel nameLabel = new JLabel(compartments.size() + " compartments", UIUtils.getIconFromResources("actions/edit-select-all.png"), JLabel.LEFT);
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        JIPipeDesktopGraphEditorUI.installContextActionsInto(toolBar,
                canvas.getNodeUIsFor(compartments),
                canvas.getContextActions(),
                canvas);

        JButton openButton = new JButton("Open in editor", UIUtils.getIconFromResources("actions/edit.png"));
        openButton.addActionListener(e -> openInEditor());
        toolBar.add(openButton);

        actionPanel.add(toolBar, BorderLayout.NORTH);
    }

    private void openInEditor() {
        for (JIPipeProjectCompartment compartment : compartments) {
            getDesktopProjectWorkbench().getOrOpenPipelineEditorTab(compartment, true);
        }
    }
}
