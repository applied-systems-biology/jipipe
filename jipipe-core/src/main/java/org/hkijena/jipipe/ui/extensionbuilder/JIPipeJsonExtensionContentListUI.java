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

package org.hkijena.jipipe.ui.extensionbuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.grouping.JsonNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.ui.JIPipeJsonExtensionWorkbench;
import org.hkijena.jipipe.ui.JIPipeJsonExtensionWorkbenchPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Lists the content of an {@link org.hkijena.jipipe.JIPipeJsonExtension}
 */
public class JIPipeJsonExtensionContentListUI extends JIPipeJsonExtensionWorkbenchPanel {
    private JList<Object> list;
    private JSplitPane splitPane;
    private Object currentlySelectedValue;

    /**
     * Creates new instance
     *
     * @param workbenchUI the workbench
     */
    public JIPipeJsonExtensionContentListUI(JIPipeJsonExtensionWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
        reload();
        getProject().getEventBus().register(this);
    }

    private void reload() {
        Object selectedValue = list.getSelectedValue();
        DefaultListModel<Object> model = (DefaultListModel<Object>) list.getModel();
        model.clear();
        for (JIPipeNodeInfo info : getProject().getNodeInfos().stream().sorted(Comparator.comparing(JIPipeNodeInfo::getName, Comparator.nullsFirst(Comparator.naturalOrder()))).collect(Collectors.toList())) {
            model.addElement(info);
        }
        if (model.contains(selectedValue)) {
            list.setSelectedValue(selectedValue, true);
        } else if (!model.isEmpty()) {
            list.setSelectedIndex(0);
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JPanel listPanel = new JPanel(new BorderLayout());
        list = new JList<>(new DefaultListModel<>());
        list.setCellRenderer(new JsonExtensionContentListCellRenderer());
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.addListSelectionListener(e -> {
            setCurrentlySelectedValue(list.getSelectedValue());
        });
        listPanel.add(list, BorderLayout.CENTER);
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        initializeToolbar(toolBar);
        listPanel.add(toolBar, BorderLayout.NORTH);
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, new JPanel());
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
    }

    private void initializeToolbar(JToolBar toolBar) {
        JButton addAlgorithmButton = new JButton("Add algorithm", UIUtils.getIconFromResources("actions/list-add.png"));
        addAlgorithmButton.addActionListener(e -> addAlgorithm());
        toolBar.add(addAlgorithmButton);
        toolBar.add(Box.createHorizontalGlue());
        JButton removeButton = new JButton(UIUtils.getIconFromResources("actions/delete.png"));
        removeButton.addActionListener(e -> removeSelection());
        toolBar.add(removeButton);
    }

    private void removeSelection() {
        for (Object item : ImmutableList.copyOf(list.getSelectedValuesList())) {
            if (item instanceof JsonNodeInfo) {
                getProject().removeAlgorithm((JsonNodeInfo) item);
            }
        }
    }

    private void addAlgorithm() {
        JsonNodeInfo info = new JsonNodeInfo();
        info.setName("");
        info.setGraph(new JIPipeGraph());
        getProject().addAlgorithm(info);
        info.getEventBus().register(this);
    }

    /**
     * Triggered when a name was changed
     *
     * @param event Generated event
     */
    @Subscribe
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if ("name".equals(event.getKey())) {
            list.repaint();
        }
    }

    /**
     * Triggered when content was added
     *
     * @param event Generated event
     */
    @Subscribe
    public void onContentAddedEvent(JIPipe.ExtensionContentAddedEvent event) {
        reload();
    }

    /**
     * Triggered when content was removed
     *
     * @param event Generated event
     */
    @Subscribe
    public void onContentRemovedEvent(JIPipe.ExtensionContentRemovedEvent event) {
        reload();
    }

    /**
     * @return The currently selected value
     */
    public Object getCurrentlySelectedValue() {
        return currentlySelectedValue;
    }

    /**
     * Sets the selected value      *      * @param currentlySelectedValue The value
     */
    public void setCurrentlySelectedValue(Object currentlySelectedValue) {
        if (currentlySelectedValue != this.currentlySelectedValue) {
            this.currentlySelectedValue = currentlySelectedValue;
            if (currentlySelectedValue != null) {
                if (currentlySelectedValue instanceof JsonNodeInfo) {
                    splitPane.setRightComponent(new JsonNodeInfoUI(getExtensionWorkbenchUI(), (JsonNodeInfo) currentlySelectedValue));
                }
            } else {
                splitPane.setRightComponent(new JPanel());
            }
        }
    }
}

