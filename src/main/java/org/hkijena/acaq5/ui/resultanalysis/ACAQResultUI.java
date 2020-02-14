package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.api.ACAQRunSample;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.io.IOException;

public class ACAQResultUI extends JPanel {
    private ACAQRun run;
    private JSplitPane splitPane;

    public ACAQResultUI(ACAQRun run) {
        this.run = run;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        ACAQResultSampleManagerUI sampleManagerUI = new ACAQResultSampleManagerUI(this);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sampleManagerUI, new JPanel());
        add(splitPane, BorderLayout.CENTER);

        sampleManagerUI.getSampleTree().addTreeSelectionListener(e -> {
            Object pathComponent = e.getPath().getLastPathComponent();
            if(pathComponent != null) {
                DefaultMutableTreeNode nd = (DefaultMutableTreeNode) pathComponent;
                if(nd.getUserObject() instanceof ACAQRunSample) {
                    setCurrentDisplayed((ACAQRunSample)nd.getUserObject());
                }
            }
        });

        initializeToolbar();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        JButton openFolderButton = new JButton("Open output folder", UIUtils.getIconFromResources("open.png"));
        openFolderButton.addActionListener(e -> openOutputFolder());
        toolBar.add(openFolderButton);
        add(toolBar, BorderLayout.NORTH);
    }

    private void openOutputFolder() {
        try {
            Desktop.getDesktop().open(run.getOutputPath().toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setCurrentDisplayed(ACAQRunSample sample) {
        splitPane.setRightComponent(new ACAQRunSampleUI(sample));
    }

    public ACAQRun getRun() {
        return run;
    }
}
