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

import com.google.common.collect.Sets;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.run.JIPipeProjectRunSet;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.AbstractJIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public class JIPipeDesktopRunSetsListEditor extends JIPipeDesktopProjectWorkbenchPanel {
    private final JList<JIPipeProjectRunSet> jList = new JList<>();

    public JIPipeDesktopRunSetsListEditor(JIPipeDesktopProjectWorkbench workbench) {
        super(workbench);
        initialize();
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

    private void removeSelectedItems() {
        for (JIPipeProjectRunSet runSet : jList.getSelectedValuesList()) {
            getProject().getRunSetsConfiguration().getRunSets().remove(runSet);
        }
        refresh();
    }

    private void addNewItem() {
        JIPipeProjectRunSet runSet = new JIPipeProjectRunSet();
        if(editRunSet(getDesktopWorkbench(), runSet)) {
            getProject().getRunSetsConfiguration().getRunSets().add(runSet);
            refresh();
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
        DefaultListModel<JIPipeProjectRunSet> model = new DefaultListModel<>();
        for (JIPipeProjectRunSet partition : getDesktopProjectWorkbench().getProject().getRunSetsConfiguration().getRunSets()) {
            model.addElement(partition);
        }
        jList.setModel(model);
    }

}
