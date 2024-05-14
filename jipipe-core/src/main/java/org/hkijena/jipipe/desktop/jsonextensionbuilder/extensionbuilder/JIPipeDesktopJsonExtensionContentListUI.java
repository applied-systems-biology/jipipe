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

package org.hkijena.jipipe.desktop.jsonextensionbuilder.extensionbuilder;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJsonPlugin;
import org.hkijena.jipipe.JIPipeService;
import org.hkijena.jipipe.api.grouping.JsonNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.desktop.jsonextensionbuilder.JIPipeDesktopJsonExtensionWorkbench;
import org.hkijena.jipipe.desktop.jsonextensionbuilder.JIPipeDesktopJsonExtensionWorkbenchPanel;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Lists the content of an {@link JIPipeJsonPlugin}
 */
public class JIPipeDesktopJsonExtensionContentListUI extends JIPipeDesktopJsonExtensionWorkbenchPanel implements JIPipeService.PluginContentAddedEventListener,
        JIPipeService.PluginContentRemovedEventListener, JIPipeParameterCollection.ParameterChangedEventListener {
    private JList<Object> list;
    private JSplitPane splitPane;
    private Object currentlySelectedValue;

    /**
     * Creates new instance
     *
     * @param workbenchUI the workbench
     */
    public JIPipeDesktopJsonExtensionContentListUI(JIPipeDesktopJsonExtensionWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
        reload();
        getPluginProject().getExtensionContentAddedEventEmitter().subscribeWeak(this);
        getPluginProject().getExtensionContentRemovedEventEmitter().subscribeWeak(this);
    }

    private void reload() {
        Object selectedValue = list.getSelectedValue();
        DefaultListModel<Object> model = (DefaultListModel<Object>) list.getModel();
        model.clear();
        for (JIPipeNodeInfo info : getPluginProject().getNodeInfos().stream().sorted(Comparator.comparing(JIPipeNodeInfo::getName, Comparator.nullsFirst(Comparator.naturalOrder()))).collect(Collectors.toList())) {
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
        list.setCellRenderer(new JIPipeDesktopJsonExtensionContentListCellRenderer());
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.addListSelectionListener(e -> {
            setCurrentlySelectedValue(list.getSelectedValue());
        });
        listPanel.add(list, BorderLayout.CENTER);
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        initializeToolbar(toolBar);
        listPanel.add(toolBar, BorderLayout.NORTH);
        splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, new JPanel(), AutoResizeSplitPane.RATIO_1_TO_3);
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
                getPluginProject().removeAlgorithm((JsonNodeInfo) item);
            }
        }
    }

    private void addAlgorithm() {
        JsonNodeInfo info = new JsonNodeInfo();
        info.setName("");
        info.setGraph(new JIPipeGraph());
        getPluginProject().addAlgorithm(info);
        info.getParameterChangedEventEmitter().subscribeWeak(this);
    }

    /**
     * Triggered when a name was changed
     *
     * @param event Generated event
     */
    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if ("name".equals(event.getKey())) {
            list.repaint();
        }
    }

    /**
     * Triggered when content was removed
     *
     * @param event Generated event
     */
    @Override
    public void onJIPipePluginContentRemoved(JIPipe.ExtensionContentRemovedEvent event) {
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
                    splitPane.setRightComponent(new JIPipeDesktopJsonNodeInfoUI(getExtensionWorkbenchUI(), (JsonNodeInfo) currentlySelectedValue));
                }
            } else {
                splitPane.setRightComponent(new JPanel());
            }
        }
    }

    @Override
    public void onJIPipePluginContentAdded(JIPipeService.ExtensionContentAddedEvent event) {
        reload();
    }
}

