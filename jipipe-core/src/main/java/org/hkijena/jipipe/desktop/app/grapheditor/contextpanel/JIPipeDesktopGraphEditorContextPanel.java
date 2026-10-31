package org.hkijena.jipipe.desktop.app.grapheditor.contextpanel;

import org.hkijena.jipipe.api.grapheditortool.JIPipeDesktopToggleableGraphEditorTool;
import org.hkijena.jipipe.api.grapheditortool.tools.DefaultGraphEditorTool;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class JIPipeDesktopGraphEditorContextPanel extends JIPipeDesktopWorkbenchPanel {
    private final JIPipeDesktopGraphEditorUI graphEditorUI;
    private final JIPipeDesktopFormPanel formPanel;
    /**
     * The islands added by the current rebuild pass. A rebuild replaces every
     * island, so the previous pass's islands are disposed at its start (see
     * {@link #rebuild()}) — otherwise their resources (e.g. the note tools'
     * palette settings bindings, which are rebuilt on every node selection)
     * would leak.
     */
    private final List<JIPipeDesktopGraphEditorContextPanelIsland> currentIslands = new ArrayList<>();

    public JIPipeDesktopGraphEditorContextPanel(JIPipeDesktopGraphEditorUI graphEditorUI) {
        super(graphEditorUI.getDesktopWorkbench());
        this.graphEditorUI = graphEditorUI;
        this.formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING | JIPipeDesktopFormPanel.TRANSPARENT_BACKGROUND);

        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout(8, 8));
        add(formPanel, BorderLayout.CENTER);
    }

    public JIPipeDesktopGraphEditorUI getGraphEditorUI() {
        return graphEditorUI;
    }

    public void rebuild() {
        // Tear down the previous pass first: a rebuild replaces every island, so
        // each of them must release its resources exactly once. This is the
        // only per-rebuild teardown hook (formPanel.clear() only drops the
        // wrapper panels added by addIsland()).
        JIPipeDesktopGraphEditorContextPanelIsland.disposePreviousIslands(currentIslands);

        JScrollPane scrollPane = formPanel.getScrollPane();
        int oldValue = scrollPane.getVerticalScrollBar().getValue();
        formPanel.clear();

        // Add tool settings panel
        JIPipeDesktopToggleableGraphEditorTool currentTool = graphEditorUI.getCurrentTool();
        if (currentTool == null) {
            currentTool = new DefaultGraphEditorTool();
        }
        addIsland(currentTool.createPropertiesPanel(graphEditorUI));

        // Add selection panel
        if (!graphEditorUI.getSelectionManager().getSelection().isEmpty()) {
            addIsland(new SelectionPanel(graphEditorUI));
        }

        SwingUtilities.invokeLater(() -> {
            scrollPane.getVerticalScrollBar().setValue(oldValue);
        });
    }

    private void addIsland(JIPipeDesktopGraphEditorContextPanelIsland island) {
        currentIslands.add(island);
        island.initializeContent();
        island.postInitializeContent();
        formPanel.addWideToForm(UIUtils.wrapInBackgroundIslandPanelIfNeeded(island));
    }
}
