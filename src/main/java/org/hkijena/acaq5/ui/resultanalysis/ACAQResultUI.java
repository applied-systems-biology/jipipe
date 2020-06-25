/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
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

/**
 * UI around an {@link ACAQRun} result
 */
public class ACAQResultUI extends ACAQProjectWorkbenchPanel {
    private ACAQRun run;
    private JSplitPane splitPane;
    private ACAQResultAlgorithmTree algorithmTree;

    /**
     * @param workbenchUI the workbench
     * @param run         the finished run
     */
    public ACAQResultUI(ACAQProjectWorkbench workbenchUI, ACAQRun run) {
        super(workbenchUI);
        this.run = run;
        initialize();
        showAllDataSlots();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        algorithmTree = new ACAQResultAlgorithmTree(getProjectWorkbench(), run);

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
                } else if (userObject instanceof ACAQGraphNode) {
                    showDataSlotsOfAlgorithm((ACAQGraphNode) userObject);
                } else {
                    showAllDataSlots();
                }
            }
        });

        initializeToolbar();
    }

    private void showAllDataSlots() {
        List<ACAQDataSlot> result = new ArrayList<>();
        for (ACAQGraphNode algorithm : run.getGraph().getAlgorithmNodes().values()) {
            for (ACAQDataSlot outputSlot : algorithm.getOutputSlots()) {
                if (Files.exists(outputSlot.getStoragePath().resolve("data-table.json"))) {
                    result.add(outputSlot);
                }
            }
        }
        showDataSlots(result);
    }

    private void showDataSlotsOfAlgorithm(ACAQGraphNode algorithm) {
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
        for (ACAQGraphNode algorithm : run.getGraph().getAlgorithmNodes().values()) {
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
        ACAQMultipleResultDataSlotTableUI ui = new ACAQMultipleResultDataSlotTableUI(getProjectWorkbench(), run, slots);
        splitPane.setRightComponent(ui);
        revalidate();
    }

    private void showDataSlot(ACAQDataSlot dataSlot) {
        ACAQResultDataSlotTableUI ui = new ACAQResultDataSlotTableUI(getProjectWorkbench(), run, dataSlot);
        splitPane.setRightComponent(ui);
        revalidate();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton openFolderButton = new JButton("Open output folder", UIUtils.getIconFromResources("open.png"));
        openFolderButton.addActionListener(e -> openOutputFolder());
        toolBar.add(openFolderButton);

        JButton openLogButton = new JButton("Open log", UIUtils.getIconFromResources("log.png"));
        openLogButton.addActionListener(e -> openLog());
        toolBar.add(openLogButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void openLog() {
        try {
            Desktop.getDesktop().open(run.getConfiguration().getOutputPath().resolve("log.txt").toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void openOutputFolder() {
        try {
            Desktop.getDesktop().open(run.getConfiguration().getOutputPath().toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return The run
     */
    public ACAQRun getRun() {
        return run;
    }
}
