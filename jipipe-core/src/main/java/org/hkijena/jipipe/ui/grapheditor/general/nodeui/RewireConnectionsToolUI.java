package org.hkijena.jipipe.ui.grapheditor.general.nodeui;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
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

        pack();
        setModal(true);
        setSize(800,600);
    }
}
