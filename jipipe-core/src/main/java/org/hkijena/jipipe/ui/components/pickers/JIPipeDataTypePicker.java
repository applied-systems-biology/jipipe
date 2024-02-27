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

package org.hkijena.jipipe.ui.components.pickers;

import com.google.common.primitives.Ints;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.ui.components.MultiSelectionModel;
import org.hkijena.jipipe.ui.components.SingleSelectionModel;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Panel that allows to pick {@link JIPipeDataInfo}
 */
public class JIPipeDataTypePicker extends JPanel {

    private final DataTypeDeselectedEventEmitter dataTypeDeselectedEventEmitter = new DataTypeDeselectedEventEmitter();
    private final DataTypeSelectedEventEmitter dataTypeSelectedEventEmitter = new DataTypeSelectedEventEmitter();
    private final SelectedDataTypesChangedEventEmitter selectedDataTypesChangedEventEmitter = new SelectedDataTypesChangedEventEmitter();

    boolean reloading = false;
    private Mode mode;
    private SearchTextField searchField;
    private JList<JIPipeDataInfo> dataTypeList;
    private Set<JIPipeDataInfo> hiddenDataTypes = new HashSet<>();
    private List<JIPipeDataInfo> availableDataTypes;
    private Set<JIPipeDataInfo> selectedDataTypes = new HashSet<>();

    /**
     * @param mode               the mode
     * @param availableDataTypes list of available data types
     */
    public JIPipeDataTypePicker(Mode mode, Set<JIPipeDataInfo> availableDataTypes) {
        this.mode = mode;
        this.availableDataTypes = new ArrayList<>();
        this.availableDataTypes.addAll(availableDataTypes.stream().sorted(Comparator.comparing(JIPipeDataInfo::getName)).collect(Collectors.toList()));
        initialize();
        refreshDataInfoList();
    }

    /**
     * Shows a dialog to data types
     *
     * @param parent             parent component
     * @param mode               mode
     * @param availableDataInfos list of available traits
     * @return picked data types
     */
    public static Set<JIPipeDataInfo> showDialog(Component parent, Mode mode, Set<JIPipeDataInfo> availableDataInfos) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent));
        JIPipeDataTypePicker picker = new JIPipeDataTypePicker(mode, availableDataInfos);

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

    private void initialize() {
        setLayout(new BorderLayout());
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        searchField = new SearchTextField();
        searchField.addActionListener(e -> refreshDataInfoList());
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

    public DataTypeDeselectedEventEmitter getDataTypeDeselectedEventEmitter() {
        return dataTypeDeselectedEventEmitter;
    }

    public DataTypeSelectedEventEmitter getDataTypeSelectedEventEmitter() {
        return dataTypeSelectedEventEmitter;
    }

    public SelectedDataTypesChangedEventEmitter getSelectedDataTypesChangedEventEmitter() {
        return selectedDataTypesChangedEventEmitter;
    }

    private void updateSelection() {
        if (reloading)
            return;
        boolean changed = false;
        if (mode == Mode.Single)
            selectedDataTypes.clear();
        for (JIPipeDataInfo dataInfo : availableDataTypes) {
            if (hiddenDataTypes.contains(dataInfo))
                continue;
            boolean uiSelected = dataTypeList.getSelectedValuesList().contains(dataInfo);
            boolean modelSelected = selectedDataTypes.contains(dataInfo);
            if (uiSelected != modelSelected) {
                if (modelSelected) {
                    selectedDataTypes.remove(dataInfo);
                    dataTypeDeselectedEventEmitter.emit(new DataTypeDeselectedEvent(this, dataInfo));
                    changed = true;
                }
                if (uiSelected) {
                    selectedDataTypes.add(dataInfo);
                    dataTypeSelectedEventEmitter.emit(new DataTypeSelectedEvent(this, dataInfo));
                    changed = true;
                }
            }
        }

        if (changed) {
            selectedDataTypesChangedEventEmitter.emit(new SelectedDataTypesChangedEvent(this));
        }
    }

    /**
     * Refreshes the list
     */
    public void refreshDataInfoList() {
        if (reloading)
            return;
        reloading = true;
        DefaultListModel<JIPipeDataInfo> model = (DefaultListModel<JIPipeDataInfo>) dataTypeList.getModel();
        hiddenDataTypes.clear();
        model.clear();
        List<Integer> selectedIndices = new ArrayList<>();

        for (JIPipeDataInfo dataInfo : availableDataTypes) {
            if (!searchField.test(dataInfo.getName() + " " + dataInfo.getDescription())) {
                hiddenDataTypes.add(dataInfo);
                continue;
            }

            model.addElement(dataInfo);
            if (selectedDataTypes.contains(dataInfo)) {
                selectedIndices.add(model.size() - 1);
            }
        }
        dataTypeList.setSelectedIndices(Ints.toArray(selectedIndices));
        reloading = false;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        refreshDataInfoList();
    }

    public Set<JIPipeDataInfo> getSelectedDataTypes() {
        return Collections.unmodifiableSet(selectedDataTypes);
    }

    public void setSelectedDataTypes(Set<JIPipeDataInfo> dataInfos) {
        this.selectedDataTypes = new HashSet<>(dataInfos);
        selectedDataTypesChangedEventEmitter.emit(new SelectedDataTypesChangedEvent(this));
        refreshDataInfoList();
    }

    /**
     * The mode of the picker
     */
    public enum Mode {
        NonInteractive,
        Single,
        Multiple
    }

    public interface SelectedDataTypesChangedEventListener {
        void onDataTypePickerSelectedDataTypesChanged(SelectedDataTypesChangedEvent event);
    }

    public interface DataTypeSelectedEventListener {
        void onDataTypePickerDataTypeSelected(DataTypeSelectedEvent event);
    }

    public interface DataTypeDeselectedEventListener {
        void onDataTypePickerDataTypeDeselected(DataTypeDeselectedEvent event);
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
     * Generated when a data type is selected
     */
    public static class SelectedDataTypesChangedEvent extends AbstractJIPipeEvent {
        private JIPipeDataTypePicker dataTypePicker;

        /**
         * @param dataTypePicker event source
         */
        public SelectedDataTypesChangedEvent(JIPipeDataTypePicker dataTypePicker) {
            super(dataTypePicker);
            this.dataTypePicker = dataTypePicker;
        }

        public JIPipeDataTypePicker getDataTypePicker() {
            return dataTypePicker;
        }
    }

    public static class SelectedDataTypesChangedEventEmitter extends JIPipeEventEmitter<SelectedDataTypesChangedEvent, SelectedDataTypesChangedEventListener> {

        @Override
        protected void call(SelectedDataTypesChangedEventListener selectedDataTypesChangedEventListener, SelectedDataTypesChangedEvent event) {
            selectedDataTypesChangedEventListener.onDataTypePickerSelectedDataTypesChanged(event);
        }
    }

    /**
     * Generated when a data type is selected
     */
    public static class DataTypeSelectedEvent extends AbstractJIPipeEvent {
        private JIPipeDataTypePicker dataTypePicker;
        private JIPipeDataInfo dataInfo;

        /**
         * @param dataTypePicker event source
         * @param dataInfo       picked data type
         */
        public DataTypeSelectedEvent(JIPipeDataTypePicker dataTypePicker, JIPipeDataInfo dataInfo) {
            super(dataTypePicker);
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

    public static class DataTypeSelectedEventEmitter extends JIPipeEventEmitter<DataTypeSelectedEvent, DataTypeSelectedEventListener> {

        @Override
        protected void call(DataTypeSelectedEventListener dataTypeSelectedEventListener, DataTypeSelectedEvent event) {
            dataTypeSelectedEventListener.onDataTypePickerDataTypeSelected(event);
        }
    }

    /**
     * Generated when a data type is deselected
     */
    public static class DataTypeDeselectedEvent extends AbstractJIPipeEvent {
        private JIPipeDataTypePicker dataTypePicker;
        private JIPipeDataInfo dataInfo;

        /**
         * @param dataTypePicker event source
         * @param dataInfo       deselected data type
         */
        public DataTypeDeselectedEvent(JIPipeDataTypePicker dataTypePicker, JIPipeDataInfo dataInfo) {
            super(dataTypePicker);
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

    public static class DataTypeDeselectedEventEmitter extends JIPipeEventEmitter<DataTypeDeselectedEvent, DataTypeDeselectedEventListener> {

        @Override
        protected void call(DataTypeDeselectedEventListener dataTypeDeselectedEventListener, DataTypeDeselectedEvent event) {
            dataTypeDeselectedEventListener.onDataTypePickerDataTypeDeselected(event);
        }
    }
}
