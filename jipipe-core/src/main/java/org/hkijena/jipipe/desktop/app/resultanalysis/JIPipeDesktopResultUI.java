/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.app.resultanalysis;

import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeGraphRun;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

/**
 * UI around an {@link JIPipeGraphRun} result
 */
public class JIPipeDesktopResultUI extends JIPipeDesktopProjectWorkbenchPanel {
    private final JIPipeProject project;
    private final Path storagePath;
    private JSplitPane splitPane;
    private JIPipeDesktopResultAlgorithmTree algorithmTree;

    public JIPipeDesktopResultUI(JIPipeDesktopProjectWorkbench workbenchUI, JIPipeProject project, Path storagePath) {
        super(workbenchUI);
        this.project = project;
        this.storagePath = storagePath;

        // Needed here to get back the storage paths
        JIPipeGraphRun.restoreStoragePaths(project, storagePath);

        // Continue with init
        initialize();
        showDataSlots(listAllDataSlots());
    }

    private void initialize() {
        setLayout(new BorderLayout());
        algorithmTree = new JIPipeDesktopResultAlgorithmTree(getDesktopProjectWorkbench(), project, storagePath);

        splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT, algorithmTree,
                new JPanel(), AutoResizeSplitPane.RATIO_1_TO_3);
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
        for (JIPipeGraphNode algorithm : project.getGraph().getGraphNodes()) {
            for (JIPipeDataSlot outputSlot : algorithm.getOutputSlots()) {
                if (Files.exists(outputSlot.getSlotStoragePath().resolve("data-table.json"))) {
                    result.add(outputSlot);
                }
            }
        }
        return result;
    }

    private List<JIPipeDataSlot> listDataSlotsOfAlgorithm(JIPipeGraphNode algorithm) {
        List<JIPipeDataSlot> result = new ArrayList<>();
        for (JIPipeDataSlot outputSlot : algorithm.getOutputSlots()) {
            if (Files.exists(outputSlot.getSlotStoragePath().resolve("data-table.json"))) {
                result.add(outputSlot);
            }
        }
        return result;
    }

    private List<JIPipeDataSlot> listDataSlotsOfCompartment(JIPipeProjectCompartment compartment) {
        UUID projectCompartmentUUID = compartment.getProjectCompartmentUUID();
        List<JIPipeDataSlot> result = new ArrayList<>();
        for (JIPipeGraphNode algorithm : project.getGraph().getGraphNodes()) {
            if (Objects.equals(algorithm.getCompartmentUUIDInParentGraph(), projectCompartmentUUID)) {
                for (JIPipeDataSlot outputSlot : algorithm.getOutputSlots()) {
                    if (Files.exists(outputSlot.getSlotStoragePath().resolve("data-table.json"))) {
                        result.add(outputSlot);
                    }
                }
            }
        }
        return result;
    }

    private void showDataSlots(List<JIPipeDataSlot> slots) {
        JIPipeDesktopMergedResultDataSlotTableUI ui = new JIPipeDesktopMergedResultDataSlotTableUI(getDesktopProjectWorkbench(), project, storagePath, slots);
        splitPane.setRightComponent(ui);
        revalidate();
    }

    private void showDataSlot(JIPipeDataSlot dataSlot) {
        JIPipeDesktopResultDataSlotTableUI ui = new JIPipeDesktopResultDataSlotTableUI(getDesktopProjectWorkbench(), project, storagePath, dataSlot);
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
            Desktop.getDesktop().open(storagePath.resolve("log.txt").toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void openOutputFolder() {
        try {
            Desktop.getDesktop().open(storagePath.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
