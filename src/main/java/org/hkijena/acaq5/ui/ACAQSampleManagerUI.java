package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.api.ACAQProjectSample;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class ACAQSampleManagerUI extends ACAQUIPanel {

    private JList<ACAQProjectSample> sampleJList;

    public ACAQSampleManagerUI(ACAQWorkbenchUI workbenchUI) {
        super(workbenchUI);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        initializeToolbar();

        sampleJList = new JList<>();
        add(sampleJList, BorderLayout.CENTER);
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        add(toolBar, BorderLayout.NORTH);

        JButton addSingleSampleButton = new JButton("Add ...", UIUtils.getIconFromResources("add.png"));
        toolBar.add(addSingleSampleButton);

        JButton addMultipleSamplesButton = new JButton("Batch import ...", UIUtils.getIconFromResources("import.png"));
        addMultipleSamplesButton.addActionListener(e -> addMultipleSamples());
        toolBar.add(addMultipleSamplesButton);

        toolBar.add(Box.createHorizontalGlue());

        JButton removeButton = new JButton("Remove selected", UIUtils.getIconFromResources("delete.png"));
        toolBar.add(removeButton);
    }

    private void addMultipleSamples() {
        getWorkbenchUI().getDocumentTabPane().addTab("Sample batch import",
                UIUtils.getIconFromResources("import.png"),
                new ACAQSampleBatchImporterUI(getWorkbenchUI()),
                DocumentTabPane.CloseMode.withAskOnCloseButton,
                false);
        getWorkbenchUI().getDocumentTabPane().switchToLastTab();
    }
}
