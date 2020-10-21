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
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.registries.JIPipeNodeRegistry;
import org.hkijena.jipipe.utils.CustomScrollPane;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Panel that allows to pick {@link JIPipeNodeInfo}
 */
public class JIPipeNodeInfoPicker extends JPanel {
    boolean reloading = false;
    private Mode mode;
    private EventBus eventBus = new EventBus();
    private SearchTextField searchField;
    private JList<JIPipeNodeInfo> nodeInfoJList;
    private Set<JIPipeNodeInfo> hiddenItems = new HashSet<>();
    private List<JIPipeNodeInfo> availableInfos;
    private Set<JIPipeNodeInfo> selectedInfos = new HashSet<>();

    /**
     * @param mode           the mode
     * @param availableInfos list of available trait types
     */
    public JIPipeNodeInfoPicker(Mode mode, Set<JIPipeNodeInfo> availableInfos) {
        this.mode = mode;
        this.availableInfos = new ArrayList<>();
        this.availableInfos.addAll(availableInfos.stream().sorted(Comparator.comparing(JIPipeNodeInfo::getName)).collect(Collectors.toList()));
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

        nodeInfoJList = new JList<>(new DefaultListModel<>());
        nodeInfoJList.setCellRenderer(new Renderer());
        if (mode == Mode.NonInteractive) {
            nodeInfoJList.setEnabled(false);
        } else if (mode == Mode.Single) {
            nodeInfoJList.setSelectionModel(new SingleSelectionModel());
            nodeInfoJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        } else {
            nodeInfoJList.setSelectionModel(new MultiSelectionModel());
            nodeInfoJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        }
        nodeInfoJList.addListSelectionListener(e -> updateSelection());
        JScrollPane scrollPane = new CustomScrollPane(nodeInfoJList);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void updateSelection() {
        if (reloading)
            return;
        boolean changed = false;
        for (JIPipeNodeInfo trait : availableInfos) {
            if (hiddenItems.contains(trait))
                continue;
            boolean uiSelected = nodeInfoJList.getSelectedValuesList().contains(trait);
            boolean modelSelected = selectedInfos.contains(trait);
            if (uiSelected != modelSelected) {
                if (modelSelected) {
                    selectedInfos.remove(trait);
                    eventBus.post(new AlgorithmDeselectedEvent(this, trait));
                    changed = true;
                }
                if (uiSelected) {
                    selectedInfos.add(trait);
                    eventBus.post(new NodeSelectedEvent(this, trait));
                    changed = true;
                }
            }
        }

        if (changed) {
            eventBus.post(new SelectedTraitsChangedEvent(this));
        }
    }

    /**
     * Refreshes the list
     */
    public void refreshTraitList() {
        reloading = true;
        DefaultListModel<JIPipeNodeInfo> model = (DefaultListModel<JIPipeNodeInfo>) nodeInfoJList.getModel();
        hiddenItems.clear();
        model.clear();
        String[] searchStrings = getSearchStrings();
        List<Integer> selectedIndices = new ArrayList<>();

        for (JIPipeNodeInfo trait : availableInfos) {
            if (!searchStringsMatches(trait, searchStrings)) {
                hiddenItems.add(trait);
                continue;
            }

            model.addElement(trait);
            if (selectedInfos.contains(trait)) {
                selectedIndices.add(model.size() - 1);
            }
        }
        nodeInfoJList.setSelectedIndices(Ints.toArray(selectedIndices));
        reloading = false;
    }

    private boolean searchStringsMatches(JIPipeNodeInfo trait, String[] strings) {
        if (trait == null)
            return true;
        if (strings == null)
            return true;
        String traitName = trait.getName() + " " + trait.getDescription();
        for (String str : strings) {
            if (traitName.toLowerCase().contains(str.toLowerCase()))
                return true;
        }
        return false;
    }

    private String[] getSearchStrings() {
        String[] searchStrings = null;
        if (searchField.getText() != null) {
            String str = searchField.getText().trim();
            if (!str.isEmpty()) {
                searchStrings = str.split(" ");
            }
        }
        return searchStrings;
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

    public Set<JIPipeNodeInfo> getSelectedInfos() {
        return Collections.unmodifiableSet(selectedInfos);
    }

    public void setSelectedInfos(Set<JIPipeNodeInfo> traits) {
        this.selectedInfos = new HashSet<>(traits);
        eventBus.post(new SelectedTraitsChangedEvent(this));
        refreshTraitList();
    }

    /**
     * Shows a dialog to pick traits
     *
     * @param parent              parent component
     * @param mode                mode
     * @param availableAlgorithms list of available traits
     * @return picked traits
     */
    public static Set<JIPipeNodeInfo> showDialog(Component parent, Mode mode, Set<JIPipeNodeInfo> availableAlgorithms) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent));
        JIPipeNodeInfoPicker picker = new JIPipeNodeInfoPicker(mode, availableAlgorithms);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(picker, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            picker.setSelectedInfos(Collections.emptySet());
            dialog.setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("Pick", UIUtils.getIconFromResources("actions/checkmark.png"));
        confirmButton.addActionListener(e -> dialog.setVisible(false));
        buttonPanel.add(confirmButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        picker.nodeInfoJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if (picker.nodeInfoJList.getSelectedValue() != null) {
                        dialog.setVisible(false);
                    }
                }
            }
        });

        dialog.setContentPane(panel);
        dialog.setTitle("Pick algorithm");
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(new Dimension(500, 600));
        dialog.setLocationRelativeTo(parent);
        UIUtils.addEscapeListener(dialog);
        dialog.setVisible(true);

        return picker.getSelectedInfos();
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
    public static class SelectedTraitsChangedEvent {
        private JIPipeNodeInfoPicker picker;

        /**
         * @param picker event source
         */
        public SelectedTraitsChangedEvent(JIPipeNodeInfoPicker picker) {
            this.picker = picker;
        }

        public JIPipeNodeInfoPicker getPicker() {
            return picker;
        }
    }

    /**
     * Renders the entries
     */
    public static class Renderer extends JCheckBox implements ListCellRenderer<JIPipeNodeInfo> {

        /**
         * Creates a new renderer
         */
        public Renderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends JIPipeNodeInfo> list, JIPipeNodeInfo value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value != null) {
                setText(StringUtils.createIconTextHTMLTable(value.getName(), JIPipe.getNodes().getIconURLFor(value)));
            } else {
                setText(StringUtils.createIconTextHTMLTable("Select none", ResourceUtils.getPluginResource("icons/actions/stock_calc-cancel.png")));
            }
            setSelected(isSelected);
            if (isSelected) {
                setBackground(new Color(184, 207, 229));
            } else {
                setBackground(new Color(255, 255, 255));
            }
            return this;
        }
    }

    /**
     * Generated when a trait is selected
     */
    public static class NodeSelectedEvent {
        private JIPipeNodeInfoPicker picker;
        private JIPipeNodeInfo info;

        /**
         * @param picker event source
         * @param info   picked trait
         */
        public NodeSelectedEvent(JIPipeNodeInfoPicker picker, JIPipeNodeInfo info) {
            this.picker = picker;
            this.info = info;
        }

        public JIPipeNodeInfoPicker getPicker() {
            return picker;
        }

        public JIPipeNodeInfo getInfo() {
            return info;
        }
    }

    /**
     * Generated when a trait is deselected
     */
    public static class AlgorithmDeselectedEvent {
        private JIPipeNodeInfoPicker picker;
        private JIPipeNodeInfo info;

        /**
         * @param picker event source
         * @param info   deselected trait
         */
        public AlgorithmDeselectedEvent(JIPipeNodeInfoPicker picker, JIPipeNodeInfo info) {
            this.picker = picker;
            this.info = info;
        }

        public JIPipeNodeInfoPicker getPicker() {
            return picker;
        }

        public JIPipeNodeInfo getInfo() {
            return info;
        }
    }

}
