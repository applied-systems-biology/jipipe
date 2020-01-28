package org.hkijena.acaq5.ui;

import javax.swing.*;
import java.awt.*;

public class ACAQDataUI extends ACAQUIPanel {

    private ACAQSampleManagerUI sampleManagerUI;

    public ACAQDataUI(ACAQWorkbenchUI workbenchUI) {
        super(workbenchUI);
        sampleManagerUI = new ACAQSampleManagerUI(getWorkbenchUI());
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sampleManagerUI, new JPanel());
        add(splitPane, BorderLayout.CENTER);
    }

}
