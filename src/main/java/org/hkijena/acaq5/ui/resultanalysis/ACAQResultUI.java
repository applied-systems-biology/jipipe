package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.ACAQProjectUI;
import org.hkijena.acaq5.ui.ACAQProjectUIPanel;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ACAQResultUI extends ACAQProjectUIPanel {
    private ACAQRun run;
    private JSplitPane splitPane;
    private ACAQResultAlgorithmTree algorithmTree;

    public ACAQResultUI(ACAQProjectUI workbenchUI, ACAQRun run) {
        super(workbenchUI);
        this.run = run;
        initialize();
        showAllDataSlots();
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

        algorithmTree.getTree().addTreeSelectionListener(e -> {
            Object lastPathComponent = e.getPath().getLastPathComponent();
            if (lastPathComponent instanceof DefaultMutableTreeNode) {
                Object userObject = ((DefaultMutableTreeNode) lastPathComponent).getUserObject();
                if (userObject instanceof ACAQDataSlot) {
                    showDataSlot((ACAQDataSlot) userObject);
                } else if (userObject instanceof ACAQProjectCompartment) {
                    showDataSlotsOfCompartment((ACAQProjectCompartment) userObject);
                } else if (userObject instanceof ACAQAlgorithm) {
                    showDataSlotsOfAlgorithm((ACAQAlgorithm) userObject);
                } else {
                    showAllDataSlots();
                }
            }
        });

        initializeToolbar();
    }

    private void showAllDataSlots() {
        List<ACAQDataSlot> result = new ArrayList<>();
        for (ACAQAlgorithm algorithm : run.getGraph().getAlgorithmNodes().values()) {
            for (ACAQDataSlot outputSlot : algorithm.getOutputSlots()) {
                if (Files.exists(outputSlot.getStoragePath().resolve("data-table.json"))) {
                    result.add(outputSlot);
                }
            }
        }
        showDataSlots(result);
    }

    private void showDataSlotsOfAlgorithm(ACAQAlgorithm algorithm) {
        List<ACAQDataSlot> result = new ArrayList<>();
        for (ACAQDataSlot outputSlot : algorithm.getOutputSlots()) {
            if (Files.exists(outputSlot.getStoragePath().resolve("data-table.json"))) {
                result.add(outputSlot);
            }
        }
        showDataSlots(result);
    }

    private void showDataSlotsOfCompartment(ACAQProjectCompartment compartment) {
        List<ACAQDataSlot> result = new ArrayList<>();
        for (ACAQAlgorithm algorithm : run.getGraph().getAlgorithmNodes().values()) {
            if (algorithm.getCompartment().equals(compartment.getProjectCompartmentId())) {
                for (ACAQDataSlot outputSlot : algorithm.getOutputSlots()) {
                    if (Files.exists(outputSlot.getStoragePath().resolve("data-table.json"))) {
                        result.add(outputSlot);
                    }
                }
            }
        }
        showDataSlots(result);
    }

    private void showDataSlots(List<ACAQDataSlot> slots) {
        ACAQMultipleResultDataSlotTableUI ui = new ACAQMultipleResultDataSlotTableUI(getWorkbenchUI(), run, slots);
        splitPane.setRightComponent(ui);
        revalidate();
    }

    private void showDataSlot(ACAQDataSlot dataSlot) {
        ACAQResultDataSlotTableUI ui = new ACAQResultDataSlotTableUI(getWorkbenchUI(), run, dataSlot);
        splitPane.setRightComponent(ui);
        revalidate();
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
