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

package org.hkijena.jipipe.ui.components;

import com.google.common.eventbus.EventBus;
import com.google.common.primitives.Ints;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Panel that allows to pick {@link JIPipeDataInfo}
 */
public class JIPipeDataTypePicker extends JPanel {

    boolean reloading = false;
    private Mode mode;
    private EventBus eventBus = new EventBus();
    private SearchTextField searchField;
    private JList<JIPipeDataInfo> dataTypeList;
    private Set<JIPipeDataInfo> hiddenDataTypes = new HashSet<>();
    private List<JIPipeDataInfo> availableDataTypes;
    private Set<JIPipeDataInfo> selectedDataTypes = new HashSet<>();

    /**
     * @param mode               the mode
     * @param availableDataTypes list of available trait types
     */
    public JIPipeDataTypePicker(Mode mode, Set<JIPipeDataInfo> availableDataTypes) {
        this.mode = mode;
        this.availableDataTypes = new ArrayList<>();
        this.availableDataTypes.addAll(availableDataTypes.stream().sorted(Comparator.comparing(JIPipeDataInfo::getName)).collect(Collectors.toList()));
        initialize();
        refreshTraitList();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        searchField = new SearchTextField();
        searchField.addActionListener(e -> refreshTraitList());
        toolBar.add(searchField);

        add(toolBar, BorderLayout.NORTH);

        dataTypeList = new JList<>(new DefaultListModel<>());
        dataTypeList.setCellRenderer(new Renderer());
        if (mode == Mode.NonInteractive) {
            dataTypeList.setEnabled(false);
        } else if (mode == Mode.Single) {
            dataTypeList.setSelectionModel(new SingleSelectionModel());
            dataTypeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        } else {
            dataTypeList.setSelectionModel(new MultiSelectionModel());
            dataTypeList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        }
        dataTypeList.addListSelectionListener(e -> updateSelection());
        JScrollPane scrollPane = new JScrollPane(dataTypeList);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void updateSelection() {
        if (reloading)
            return;
        boolean changed = false;
        for (JIPipeDataInfo trait : availableDataTypes) {
            if (hiddenDataTypes.contains(trait))
                continue;
            boolean uiSelected = dataTypeList.getSelectedValuesList().contains(trait);
            boolean modelSelected = selectedDataTypes.contains(trait);
            if (uiSelected != modelSelected) {
                if (modelSelected) {
                    selectedDataTypes.remove(trait);
                    eventBus.post(new DataTypeDeselectedEvent(this, trait));
                    changed = true;
                }
                if (uiSelected) {
                    selectedDataTypes.add(trait);
                    eventBus.post(new DataTypeSelectedEvent(this, trait));
                    changed = true;
                }
            }
        }

        if (changed) {
            eventBus.post(new SelectedDataTypesChangedEvent(this));
        }
    }

    /**
     * Refreshes the list
     */
    public void refreshTraitList() {
        if (reloading)
            return;
        reloading = true;
        DefaultListModel<JIPipeDataInfo> model = (DefaultListModel<JIPipeDataInfo>) dataTypeList.getModel();
        hiddenDataTypes.clear();
        model.clear();
        List<Integer> selectedIndices = new ArrayList<>();

        for (JIPipeDataInfo trait : availableDataTypes) {
            if (!searchField.test(trait.getName() + " " + trait.getDescription())) {
                hiddenDataTypes.add(trait);
                continue;
            }

            model.addElement(trait);
            if (selectedDataTypes.contains(trait)) {
                selectedIndices.add(model.size() - 1);
            }
        }
        dataTypeList.setSelectedIndices(Ints.toArray(selectedIndices));
        reloading = false;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        refreshTraitList();
    }

    public Set<JIPipeDataInfo> getSelectedDataTypes() {
        return Collections.unmodifiableSet(selectedDataTypes);
    }

    public void setSelectedDataTypes(Set<JIPipeDataInfo> traits) {
        this.selectedDataTypes = new HashSet<>(traits);
        eventBus.post(new SelectedDataTypesChangedEvent(this));
        refreshTraitList();
    }

    /**
     * Shows a dialog to pick traits
     *
     * @param parent          parent component
     * @param mode            mode
     * @param availableTraits list of available traits
     * @return picked traits
     */
    public static Set<JIPipeDataInfo> showDialog(Component parent, Mode mode, Set<JIPipeDataInfo> availableTraits) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent));
        JIPipeDataTypePicker picker = new JIPipeDataTypePicker(mode, availableTraits);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(picker, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            picker.setSelectedDataTypes(Collections.emptySet());
            dialog.setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("Pick", UIUtils.getIconFromResources("actions/checkmark.png"));
        confirmButton.addActionListener(e -> dialog.setVisible(false));
        buttonPanel.add(confirmButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setTitle("Pick annotation");
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(new Dimension(500, 600));
        dialog.setLocationRelativeTo(parent);
        UIUtils.addEscapeListener(dialog);
        dialog.setVisible(true);

        return picker.getSelectedDataTypes();
    }

    /**
     * The mode of the picker
     */
    public enum Mode {
        NonInteractive,
        Single,
        Multiple
    }

    /**
     * Generated when a trait is selected
     */
    public static class SelectedDataTypesChangedEvent {
        private JIPipeDataTypePicker dataTypePicker;

        /**
         * @param dataTypePicker event source
         */
        public SelectedDataTypesChangedEvent(JIPipeDataTypePicker dataTypePicker) {
            this.dataTypePicker = dataTypePicker;
        }

        public JIPipeDataTypePicker getDataTypePicker() {
            return dataTypePicker;
        }
    }

    /**
     * Renders an item
     */
    public static class Renderer extends JCheckBox implements ListCellRenderer<JIPipeDataInfo> {

        /**
         * Creates a new renderer
         */
        public Renderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends JIPipeDataInfo> list, JIPipeDataInfo value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value != null) {
                setText(StringUtils.createIconTextHTMLTable(value.getName(), JIPipe.getDataTypes().getIconURLFor(value)));
            } else {
                setText(StringUtils.createIconTextHTMLTable("Select none", ResourceUtils.getPluginResource("icons/actions/stock_calc-cancel.png")));
            }
            setSelected(isSelected);
            if (isSelected) {
                setBackground(UIManager.getColor("List.selectionBackground"));
            } else {
                setBackground(UIManager.getColor("List.background"));
            }
            return this;
        }
    }

    /**
     * Generated when a trait is selected
     */
    public static class DataTypeSelectedEvent {
        private JIPipeDataTypePicker dataTypePicker;
        private JIPipeDataInfo dataInfo;

        /**
         * @param dataTypePicker event source
         * @param dataInfo       picked trait
         */
        public DataTypeSelectedEvent(JIPipeDataTypePicker dataTypePicker, JIPipeDataInfo dataInfo) {
            this.dataTypePicker = dataTypePicker;
            this.dataInfo = dataInfo;
        }

        public JIPipeDataTypePicker getDataTypePicker() {
            return dataTypePicker;
        }

        public JIPipeDataInfo getDataInfo() {
            return dataInfo;
        }
    }

    /**
     * Generated when a trait is deselected
     */
    public static class DataTypeDeselectedEvent {
        private JIPipeDataTypePicker dataTypePicker;
        private JIPipeDataInfo dataInfo;

        /**
         * @param dataTypePicker event source
         * @param dataInfo       deselected trait
         */
        public DataTypeDeselectedEvent(JIPipeDataTypePicker dataTypePicker, JIPipeDataInfo dataInfo) {
            this.dataTypePicker = dataTypePicker;
            this.dataInfo = dataInfo;
        }

        public JIPipeDataTypePicker getDataTypePicker() {
            return dataTypePicker;
        }

        public JIPipeDataInfo getDataInfo() {
            return dataInfo;
        }
    }
}
