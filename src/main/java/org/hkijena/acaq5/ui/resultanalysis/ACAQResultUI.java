package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;

public class ACAQResultUI extends ACAQUIPanel {
    private ACAQRun run;
    private JSplitPane splitPane;
    private ACAQResultAlgorithmTree algorithmTree;

    public ACAQResultUI(ACAQWorkbenchUI workbenchUI, ACAQRun run) {
        super(workbenchUI);
        this.run = run;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        algorithmTree = new ACAQResultAlgorithmTree(getWorkbenchUI(), run);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, algorithmTree,
        new JPanel());
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.33);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.33);
            }
        });
        add(splitPane, BorderLayout.CENTER);

//        ACAQResultSampleManagerUI sampleManagerUI = new ACAQResultSampleManagerUI(this);
//
//        MarkdownReader documentation = new MarkdownReader(false);
//        documentation.loadFromResource("documentation/result-analysis.md");

//        add(splitPane, BorderLayout.CENTER);
//
//        sampleManagerUI.getSampleTree().addTreeSelectionListener(e -> {
//            Object pathComponent = e.getPath().getLastPathComponent();
//            if(pathComponent != null) {
//                DefaultMutableTreeNode nd = (DefaultMutableTreeNode) pathComponent;
//                if(nd.getUserObject() instanceof ACAQRunSample) {
//                    setCurrentDisplayed((ACAQRunSample)nd.getUserObject());
//                }
//            }
//        });

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
            Desktop.getDesktop().open(run.getConfiguration().getOutputPath().toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ACAQRun getRun() {
        return run;
    }
}
