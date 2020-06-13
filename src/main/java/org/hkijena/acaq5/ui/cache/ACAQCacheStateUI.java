package org.hkijena.acaq5.ui.cache;

import org.hkijena.acaq5.api.ACAQProjectCache;
import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.resultanalysis.ACAQMultipleResultDataSlotTableUI;
import org.hkijena.acaq5.ui.resultanalysis.ACAQResultAlgorithmTree;
import org.hkijena.acaq5.ui.resultanalysis.ACAQResultDataSlotTableUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

/**
 * UI around an {@link ACAQRun} result
 */
public class ACAQCacheStateUI extends ACAQProjectWorkbenchPanel {
    private ACAQProjectCache.State state;
    private JSplitPane splitPane;
    private ACAQCacheStateTree algorithmTree;

    /**
     * @param workbenchUI the workbench
     * @param state         the finished run
     */
    public ACAQCacheStateUI(ACAQProjectWorkbench workbenchUI, ACAQProjectCache.State state) {
        super(workbenchUI);
        this.state = state;
        initialize();
        showAllDataSlots();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        algorithmTree = new ACAQCacheStateTree(getProjectWorkbench(), state);

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
        for (ACAQGraphNode node : getProject().getGraph().getAlgorithmNodes().values()) {
            Map<String, ACAQDataSlot> slotMap = getProject().getCache().extract((ACAQAlgorithm) node, state);
            if (slotMap != null) {
                result.addAll(slotMap.values());
            }
        }
        showDataSlots(result);
    }

    private void showDataSlotsOfAlgorithm(ACAQGraphNode algorithm) {
        Map<String, ACAQDataSlot> slotMap = getProject().getCache().extract((ACAQAlgorithm) algorithm, state);
        showDataSlots(new ArrayList<>(slotMap.values()));
    }

    private void showDataSlotsOfCompartment(ACAQProjectCompartment compartment) {
        List<ACAQDataSlot> result = new ArrayList<>();
        for (ACAQGraphNode algorithm : getProject().getGraph().getAlgorithmNodes().values()) {
            if (algorithm.getCompartment().equals(compartment.getName()) || algorithm.getCompartment().equals(compartment.getProjectCompartmentId())) {
                Map<String, ACAQDataSlot> slotMap = getProject().getCache().extract((ACAQAlgorithm) algorithm, state);
                if (slotMap != null) {
                    result.addAll(slotMap.values());
                }
            }
        }
        showDataSlots(result);
    }

    private void showDataSlots(List<ACAQDataSlot> slots) {
        ACAQCacheMultiDataSlotTableUI ui = new ACAQCacheMultiDataSlotTableUI(getProjectWorkbench(), slots);
        splitPane.setRightComponent(ui);
        revalidate();
    }

    private void showDataSlot(ACAQDataSlot dataSlot) {
        ACAQCacheDataSlotTableUI ui = new ACAQCacheDataSlotTableUI(getProjectWorkbench(), dataSlot);
        splitPane.setRightComponent(ui);
        revalidate();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton clear = new JButton("Clear data", UIUtils.getIconFromResources("clear-brush.png"));
        clear.addActionListener(e -> clearState());
        toolBar.add(clear);

        add(toolBar, BorderLayout.NORTH);
    }

    private void clearState() {
        for (ACAQGraphNode algorithm : getProject().getGraph().getAlgorithmNodes().values()) {
            getProject().getCache().clear((ACAQAlgorithm) algorithm, state);
        }
    }
}
