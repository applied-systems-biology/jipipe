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

package org.hkijena.jipipe.ui.resultanalysis;

import org.hkijena.jipipe.api.JIPipeRun;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * UI around an {@link JIPipeRun} result
 */
public class JIPipeResultUI extends JIPipeProjectWorkbenchPanel {
    private JIPipeRun run;
    private JSplitPane splitPane;
    private JIPipeResultAlgorithmTree algorithmTree;

    /**
     * @param workbenchUI the workbench
     * @param run         the finished run
     */
    public JIPipeResultUI(JIPipeProjectWorkbench workbenchUI, JIPipeRun run) {
        super(workbenchUI);
        this.run = run;
        initialize();
        showDataSlots(listAllDataSlots());
    }

    private void initialize() {
        setLayout(new BorderLayout());
        algorithmTree = new JIPipeResultAlgorithmTree(getProjectWorkbench(), run);

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

        algorithmTree.getTree().addTreeSelectionListener(e -> updateSelection());

        initializeToolbar();
    }

    private void updateSelection() {
        Set<JIPipeDataSlot> result = new LinkedHashSet<>();
        if (algorithmTree.getTree().getSelectionPaths() != null) {
            for (TreePath path : algorithmTree.getTree().getSelectionPaths()) {
                Object lastPathComponent = path.getLastPathComponent();
                if (lastPathComponent instanceof DefaultMutableTreeNode) {
                    Object userObject = ((DefaultMutableTreeNode) lastPathComponent).getUserObject();
                    if (userObject instanceof JIPipeDataSlot) {
                        result.add((JIPipeDataSlot) userObject);
                    } else if (userObject instanceof JIPipeProjectCompartment) {
                        result.addAll(listDataSlotsOfCompartment((JIPipeProjectCompartment) userObject));
                    } else if (userObject instanceof JIPipeGraphNode) {
                        result.addAll(listDataSlotsOfAlgorithm((JIPipeGraphNode) userObject));
                    } else {
                        result.addAll(listAllDataSlots());
                    }
                }
            }
        }
        if (result.size() == 1) {
            showDataSlot(result.iterator().next());
        } else {
            showDataSlots(new ArrayList<>(result));
        }
    }

    private List<JIPipeDataSlot> listAllDataSlots() {
        List<JIPipeDataSlot> result = new ArrayList<>();
        for (JIPipeGraphNode algorithm : run.getGraph().getGraphNodes()) {
            for (JIPipeDataSlot outputSlot : algorithm.getOutputSlots()) {
                if (Files.exists(outputSlot.getStoragePath().resolve("data-table.json"))) {
                    result.add(outputSlot);
                }
            }
        }
        return result;
    }

    private List<JIPipeDataSlot> listDataSlotsOfAlgorithm(JIPipeGraphNode algorithm) {
        List<JIPipeDataSlot> result = new ArrayList<>();
        for (JIPipeDataSlot outputSlot : algorithm.getOutputSlots()) {
            if (Files.exists(outputSlot.getStoragePath().resolve("data-table.json"))) {
                result.add(outputSlot);
            }
        }
        return result;
    }

    private List<JIPipeDataSlot> listDataSlotsOfCompartment(JIPipeProjectCompartment compartment) {
        List<JIPipeDataSlot> result = new ArrayList<>();
        for (JIPipeGraphNode algorithm : run.getGraph().getGraphNodes()) {
            if (algorithm.getCompartment().equals(compartment.getProjectCompartmentId())) {
                for (JIPipeDataSlot outputSlot : algorithm.getOutputSlots()) {
                    if (Files.exists(outputSlot.getStoragePath().resolve("data-table.json"))) {
                        result.add(outputSlot);
                    }
                }
            }
        }
        return result;
    }

    private void showDataSlots(List<JIPipeDataSlot> slots) {
        JIPipeMergedResultDataSlotTableUI ui = new JIPipeMergedResultDataSlotTableUI(getProjectWorkbench(), run, slots);
        splitPane.setRightComponent(ui);
        revalidate();
    }

    private void showDataSlot(JIPipeDataSlot dataSlot) {
        JIPipeResultDataSlotTableUI ui = new JIPipeResultDataSlotTableUI(getProjectWorkbench(), run, dataSlot);
        splitPane.setRightComponent(ui);
        revalidate();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton openFolderButton = new JButton("Open output folder", UIUtils.getIconFromResources("actions/document-open-folder.png"));
        openFolderButton.addActionListener(e -> openOutputFolder());
        toolBar.add(openFolderButton);

        JButton openLogButton = new JButton("Open log", UIUtils.getIconFromResources("actions/show_log.png"));
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
    public JIPipeRun getRun() {
        return run;
    }
}
