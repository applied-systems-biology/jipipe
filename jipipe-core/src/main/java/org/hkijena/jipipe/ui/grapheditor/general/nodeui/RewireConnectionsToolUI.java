package org.hkijena.jipipe.ui.grapheditor.general.nodeui;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class RewireConnectionsToolUI extends JDialog {
    private final JIPipeDataSlot currentSlot;
    private final Set<JIPipeDataSlot> currentConnections;

    public RewireConnectionsToolUI(JIPipeDataSlot currentSlot, Set<JIPipeDataSlot> currentConnections) {
        this.currentSlot = currentSlot;
        this.currentConnections = currentConnections;

        initialize();
    }

    private void initialize() {
        UIUtils.addEscapeListener(this);
        setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        setContentPane(new JPanel(new BorderLayout()));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(AutoResizeSplitPane.LEFT_RIGHT, AutoResizeSplitPane.RATIO_1_TO_1);
        FormPanel connectionsList = new FormPanel(FormPanel.WITH_SCROLLING);
        JList<JIPipeDataSlot> alternativesList = new JList<>();

        splitPane.setLeftComponent(connectionsList);
        splitPane.setRightComponent(alternativesList);

        initializeConnectionsList(connectionsList);

        pack();
        setModal(true);
        setSize(800,600);
    }

    private void initializeConnectionsList(FormPanel connectionsList) {
        connectionsList.addGroupHeader("List of rewired connections", UIUtils.getIconFromResources(""))
    }
}
