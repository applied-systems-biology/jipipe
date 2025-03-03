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

package org.hkijena.jipipe.desktop.app.settings;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProjectRunSetsConfiguration;
import org.hkijena.jipipe.api.run.JIPipeProjectRunSet;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.AbstractJIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.parameters.library.graph.GraphNodeReferenceParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class JIPipeDesktopRunSetsListEditor extends JIPipeDesktopProjectWorkbenchPanel implements JIPipeProjectRunSetsConfiguration.RunSetsModifiedEventListener {
    private final JList<JIPipeProjectRunSet> jList = new JList<>();

    public JIPipeDesktopRunSetsListEditor(JIPipeDesktopProjectWorkbench workbench) {
        super(workbench);
        initialize();
        workbench.getProject().getRunSetsConfiguration().getModifiedEventEmitter().subscribe(this);
        refresh();
    }

    public static boolean editRunSet(JIPipeDesktopWorkbench workbench, JIPipeProjectRunSet value) {
        JIPipeProjectRunSet copy = new JIPipeProjectRunSet(value);
        if (JIPipeDesktopParameterFormPanel.showDialog(workbench,
                copy,
                MarkdownText.fromPluginResource("documentation/project-info-run-sets-editor.md"),
                "Edit run set",
                JIPipeDesktopParameterFormPanel.DEFAULT_DIALOG_FLAGS)) {
            value.setTo(copy);
            return true;
        }
        return false;
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(UIUtils.createButton("", UIUtils.getIconFromResources("actions/caret-up.png"), this::sortItemUp));
        toolBar.add(UIUtils.createButton("", UIUtils.getIconFromResources("actions/caret-down.png"), this::sortItemDown));
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(UIUtils.createButton("Add", UIUtils.getIconFromResources("actions/add.png"), this::addNewItem));
        toolBar.add(UIUtils.createButton("Remove", UIUtils.getIconFromResources("actions/trash-empty.png"), this::removeSelectedItems));
        toolBar.addSeparator();
        toolBar.add(UIUtils.createButton("Edit", UIUtils.getIconFromResources("actions/edit.png"), this::editSelectedItem));

        add(toolBar, BorderLayout.NORTH);

        jList.setCellRenderer(new JIPipeDesktopRunSetListCellRenderer(getDesktopProjectWorkbench().getProject(), getDesktopProjectWorkbench().getProject().getRunSetsConfiguration()));
        jList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    editSelectedItem();
                }
            }
        });
        add(new JScrollPane(jList), BorderLayout.CENTER);
    }

    private void sortItemDown() {
        if(jList.getSelectedValue() != null) {
            getProject().getRunSetsConfiguration().sortUp(jList.getSelectedValue());
        }
    }

    private void sortItemUp() {
        if(jList.getSelectedValue() != null) {
            getProject().getRunSetsConfiguration().sortDown(jList.getSelectedValue());
        }
    }

    private void removeSelectedItems() {
        if (JOptionPane.showConfirmDialog(this, "Do you really want to delete the selected run sets?", "Delete run sets", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
            return;
        }
        for (JIPipeProjectRunSet runSet : jList.getSelectedValuesList()) {
            getProject().getRunSetsConfiguration().remove(runSet);
        }
        refresh();
    }

    private void addNewItem() {
        JIPipeProjectRunSet runSet = new JIPipeProjectRunSet();
        if (editRunSet(getDesktopWorkbench(), runSet)) {
            getProject().getRunSetsConfiguration().add(runSet);
        }
    }

    private void editSelectedItem() {
        JIPipeProjectRunSet value = jList.getSelectedValue();
        if (value != null) {
            editRunSet(getDesktopWorkbench(), value);
            refresh();
        }
    }

    private void refresh() {
        ImmutableList<JIPipeProjectRunSet> selectedValues = ImmutableList.copyOf(jList.getSelectedValuesList());

        DefaultListModel<JIPipeProjectRunSet> model = new DefaultListModel<>();
        for (JIPipeProjectRunSet partition : getDesktopProjectWorkbench().getProject().getRunSetsConfiguration().getRunSets()) {
            model.addElement(partition);
        }
        jList.setModel(model);

        UIUtils.selectItemsInJList(jList, selectedValues);
    }

    @Override
    public void onRunSetsModified(JIPipeProjectRunSetsConfiguration.RunSetsModifiedEvent event) {
        refresh();
    }

    public static void createRunSetsManagementContextMenu(JMenu menu, Set<JIPipeGraphNode> nodes, JIPipeDesktopWorkbench workbench) {
        menu.add(UIUtils.createMenuItem("Assign to new run set ...", "Creates a new run set and afterwards adds the node to it", UIUtils.getIconFromResources("actions/add.png"), () -> {
            String newName = JOptionPane.showInputDialog(workbench.getWindow(), "Enter new run set name", "New run set", JOptionPane.PLAIN_MESSAGE);
            if(!StringUtils.isNullOrEmpty(newName)) {
                JIPipeProjectRunSet runSet = new JIPipeProjectRunSet();
                runSet.setName(newName);
                for (JIPipeGraphNode node : nodes) {
                    if (node instanceof JIPipeAlgorithm || node instanceof JIPipeProjectCompartment) {
                        runSet.getNodes().add(new GraphNodeReferenceParameter(node));
                    }
                }
                workbench.getProject().getRunSetsConfiguration().add(runSet);
            }
        }));
        menu.add(UIUtils.createMenuItem("Edit run sets", "Opens the run set editor panel", UIUtils.getIconFromResources("actions/edit.png"), () -> {
            workbench.getDocumentTabPane().selectSingletonTab(JIPipeDesktopProjectWorkbench.TAB_PROJECT_OVERVIEW);
            Component content = workbench.getDocumentTabPane().getSingletonTabInstances().get(JIPipeDesktopProjectWorkbench.TAB_PROJECT_OVERVIEW).getContent();
            if(content instanceof JIPipeDesktopProjectOverviewUI) {
                ((JIPipeDesktopProjectOverviewUI) content).getDockPanel().activatePanel("RUN_SETS", true);
            }
        }));

        Set<String> targetUUIDs = nodes.stream().map(JIPipeGraphNode::getUUIDInParentGraph).map(UUID::toString).collect(Collectors.toSet());

        Map<JIPipeProjectRunSet, Boolean> states = new HashMap<>();
        for (JIPipeProjectRunSet runSet : workbench.getProject().getRunSetsConfiguration().getRunSets()) {
            if (!Sets.intersection(targetUUIDs, runSet.getNodeUUIDs()).isEmpty()) {
                states.put(runSet, true);
            } else {
                states.put(runSet, false);
            }
        }

        if (!states.isEmpty()) {
            menu.addSeparator();
            states.keySet().stream().sorted(Comparator.comparing(JIPipeProjectRunSet::getDisplayName)).forEach(runSet -> {
                JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(runSet.getDisplayName(), states.get(runSet));
                menuItem.addActionListener(e -> {
                    if (menuItem.isSelected()) {
                        runSet.addNodes(nodes, workbench.getProject().getRunSetsConfiguration());
                    } else {
                        runSet.removeNodes(nodes, workbench.getProject().getRunSetsConfiguration());
                    }
                });
                menu.add(menuItem);
            });
        }
    }
}
