package org.hkijena.acaq5.ui.samplemanagement;

import org.hkijena.acaq5.api.ACAQProjectSample;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;

public class ACAQDataUI extends ACAQUIPanel {

    private ACAQSampleManagerUI sampleManagerUI;
    private ACAQProjectSample currentlyDisplayedSample;
    private JSplitPane splitPane;

    public ACAQDataUI(ACAQWorkbenchUI workbenchUI) {
        super(workbenchUI);
        sampleManagerUI = new ACAQSampleManagerUI(getWorkbenchUI());
        initialize();
        getProject().getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sampleManagerUI, new JPanel());
        add(splitPane, BorderLayout.CENTER);

        sampleManagerUI.getSampleTree().addTreeSelectionListener(e -> {
            Object pathComponent = e.getPath().getLastPathComponent();
            if(pathComponent != null) {
                DefaultMutableTreeNode nd = (DefaultMutableTreeNode) pathComponent;
                if(nd.getUserObject() instanceof ACAQProjectSample) {
                    if(currentlyDisplayedSample != nd.getUserObject()) {
                        setCurrentlyDisplayedSample((ACAQProjectSample)nd.getUserObject());
                    }
                }
                else {
                    setCurrentlyDisplayedSample(null);
                }
            }
        });
    }

    private void setCurrentlyDisplayedSample(ACAQProjectSample sample) {
        if(currentlyDisplayedSample == sample)
            return;
        currentlyDisplayedSample = sample;
        if(sample != null) {
            splitPane.setRightComponent(new ACAQProjectSampleUI(getWorkbenchUI(), sample));
        }
        else {
            splitPane.setRightComponent(new JPanel());
        }
    }

}
