package org.hkijena.acaq5.ui.grapheditor;

import org.hkijena.acaq5.api.ACAQAlgorithm;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class ACAQAlgorithmSettingsUI extends JPanel {
    private ACAQAlgorithm algorithm;

    public ACAQAlgorithmSettingsUI(ACAQAlgorithm algorithm) {
        this.algorithm = algorithm;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        DocumentTabPane tabbedPane = new DocumentTabPane();

        FormPanel formPanel = new FormPanel("documentation/algorithm-graph.md", true);
        initializeParameterPanel(formPanel);
        tabbedPane.addTab("Parameters", UIUtils.getIconFromResources("cog.png"),
                formPanel,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        ACAQSlotEditorUI slotEditorUI = new ACAQSlotEditorUI(algorithm);
        tabbedPane.addTab("Slots", UIUtils.getIconFromResources("database.png"),
                slotEditorUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void initializeParameterPanel(FormPanel formPanel) {

    }

    public ACAQAlgorithm getAlgorithm() {
        return algorithm;
    }
}
