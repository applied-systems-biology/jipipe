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
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.ui.registries.JIPipeUIAlgorithmRegistry;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Panel that allows to pick {@link org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration}
 */
public class JIPipeAlgorithmDeclarationPicker extends JPanel {
    boolean reloading = false;
    private Mode mode;
    private EventBus eventBus = new EventBus();
    private SearchTextField searchField;
    private JList<JIPipeAlgorithmDeclaration> declarationList;
    private Set<JIPipeAlgorithmDeclaration> hiddenItems = new HashSet<>();
    private List<JIPipeAlgorithmDeclaration> availableDeclarations;
    private Set<JIPipeAlgorithmDeclaration> selectedDeclarations = new HashSet<>();

    /**
     * @param mode                  the mode
     * @param availableDeclarations list of available trait types
     */
    public JIPipeAlgorithmDeclarationPicker(Mode mode, Set<JIPipeAlgorithmDeclaration> availableDeclarations) {
        this.mode = mode;
        this.availableDeclarations = new ArrayList<>();
        this.availableDeclarations.addAll(availableDeclarations.stream().sorted(Comparator.comparing(JIPipeAlgorithmDeclaration::getName)).collect(Collectors.toList()));
        initialize();
        refreshTraitList();
    }

    public JList<JIPipeAlgorithmDeclaration> getDeclarationList() {
        return declarationList;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        searchField = new SearchTextField();
        searchField.addActionListener(e -> refreshTraitList());
        toolBar.add(searchField);

        add(toolBar, BorderLayout.NORTH);

        declarationList = new JList<>(new DefaultListModel<>());
        declarationList.setCellRenderer(new Renderer());
        if (mode == Mode.NonInteractive) {
            declarationList.setEnabled(false);
        } else if (mode == Mode.Single) {
            declarationList.setSelectionModel(new SingleSelectionModel());
            declarationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        } else {
            declarationList.setSelectionModel(new MultiSelectionModel());
            declarationList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        }
        declarationList.addListSelectionListener(e -> updateSelection());
        add(new JScrollPane(declarationList), BorderLayout.CENTER);
    }

    private void updateSelection() {
        if (reloading)
            return;
        boolean changed = false;
        for (JIPipeAlgorithmDeclaration trait : availableDeclarations) {
            if (hiddenItems.contains(trait))
                continue;
            boolean uiSelected = declarationList.getSelectedValuesList().contains(trait);
            boolean modelSelected = selectedDeclarations.contains(trait);
            if (uiSelected != modelSelected) {
                if (modelSelected) {
                    selectedDeclarations.remove(trait);
                    eventBus.post(new AlgorithmDeselectedEvent(this, trait));
                    changed = true;
                }
                if (uiSelected) {
                    selectedDeclarations.add(trait);
                    eventBus.post(new AlgorithmSelectedEvent(this, trait));
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
        DefaultListModel<JIPipeAlgorithmDeclaration> model = (DefaultListModel<JIPipeAlgorithmDeclaration>) declarationList.getModel();
        hiddenItems.clear();
        model.clear();
        String[] searchStrings = getSearchStrings();
        List<Integer> selectedIndices = new ArrayList<>();

        for (JIPipeAlgorithmDeclaration trait : availableDeclarations) {
            if (!searchStringsMatches(trait, searchStrings)) {
                hiddenItems.add(trait);
                continue;
            }

            model.addElement(trait);
            if (selectedDeclarations.contains(trait)) {
                selectedIndices.add(model.size() - 1);
            }
        }
        declarationList.setSelectedIndices(Ints.toArray(selectedIndices));
        reloading = false;
    }

    private boolean searchStringsMatches(JIPipeAlgorithmDeclaration trait, String[] strings) {
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

    public Set<JIPipeAlgorithmDeclaration> getSelectedDeclarations() {
        return Collections.unmodifiableSet(selectedDeclarations);
    }

    public void setSelectedDeclarations(Set<JIPipeAlgorithmDeclaration> traits) {
        this.selectedDeclarations = new HashSet<>(traits);
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
    public static Set<JIPipeAlgorithmDeclaration> showDialog(Component parent, Mode mode, Set<JIPipeAlgorithmDeclaration> availableAlgorithms) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent));
        JIPipeAlgorithmDeclarationPicker picker = new JIPipeAlgorithmDeclarationPicker(mode, availableAlgorithms);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(picker, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("remove.png"));
        cancelButton.addActionListener(e -> {
            picker.setSelectedDeclarations(Collections.emptySet());
            dialog.setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("Pick", UIUtils.getIconFromResources("checkmark.png"));
        confirmButton.addActionListener(e -> dialog.setVisible(false));
        buttonPanel.add(confirmButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        picker.declarationList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if (picker.declarationList.getSelectedValue() != null) {
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

        return picker.getSelectedDeclarations();
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
        private JIPipeAlgorithmDeclarationPicker picker;

        /**
         * @param picker event source
         */
        public SelectedTraitsChangedEvent(JIPipeAlgorithmDeclarationPicker picker) {
            this.picker = picker;
        }

        public JIPipeAlgorithmDeclarationPicker getPicker() {
            return picker;
        }
    }

    /**
     * Renders the entries
     */
    public static class Renderer extends JCheckBox implements ListCellRenderer<JIPipeAlgorithmDeclaration> {

        /**
         * Creates a new renderer
         */
        public Renderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends JIPipeAlgorithmDeclaration> list, JIPipeAlgorithmDeclaration value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value != null) {
                setText(StringUtils.createIconTextHTMLTable(value.getName(), JIPipeUIAlgorithmRegistry.getInstance().getIconURLFor(value)));
            } else {
                setText(StringUtils.createIconTextHTMLTable("Select none", ResourceUtils.getPluginResource("icons/remove.png")));
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
    public static class AlgorithmSelectedEvent {
        private JIPipeAlgorithmDeclarationPicker picker;
        private JIPipeAlgorithmDeclaration declaration;

        /**
         * @param picker      event source
         * @param declaration picked trait
         */
        public AlgorithmSelectedEvent(JIPipeAlgorithmDeclarationPicker picker, JIPipeAlgorithmDeclaration declaration) {
            this.picker = picker;
            this.declaration = declaration;
        }

        public JIPipeAlgorithmDeclarationPicker getPicker() {
            return picker;
        }

        public JIPipeAlgorithmDeclaration getDeclaration() {
            return declaration;
        }
    }

    /**
     * Generated when a trait is deselected
     */
    public static class AlgorithmDeselectedEvent {
        private JIPipeAlgorithmDeclarationPicker picker;
        private JIPipeAlgorithmDeclaration declaration;

        /**
         * @param picker      event source
         * @param declaration deselected trait
         */
        public AlgorithmDeselectedEvent(JIPipeAlgorithmDeclarationPicker picker, JIPipeAlgorithmDeclaration declaration) {
            this.picker = picker;
            this.declaration = declaration;
        }

        public JIPipeAlgorithmDeclarationPicker getPicker() {
            return picker;
        }

        public JIPipeAlgorithmDeclaration getDeclaration() {
            return declaration;
        }
    }

    /**
     * Selection model for single selections
     */
    public static class SingleSelectionModel extends DefaultListSelectionModel {
        @Override
        public void setSelectionInterval(int startIndex, int endIndex) {
            if (startIndex == endIndex) {
                if (multipleItemsAreCurrentlySelected()) {
                    clearSelection();
                }
                if (isSelectedIndex(startIndex)) {
                    clearSelection();
                } else {
                    super.setSelectionInterval(startIndex, endIndex);
                }
            }
            // User selected multiple items
            else {
                super.setSelectionInterval(startIndex, endIndex);
            }
        }

        private boolean multipleItemsAreCurrentlySelected() {
            return getMinSelectionIndex() != getMaxSelectionIndex();
        }
    }

    /**
     * Selection model for multiple selections
     */
    public static class MultiSelectionModel extends DefaultListSelectionModel {
        @Override
        public void setSelectionInterval(int startIndex, int endIndex) {
            if (startIndex == endIndex) {
                if (isSelectedIndex(startIndex)) {
                    removeSelectionInterval(startIndex, endIndex);
                } else {
                    super.addSelectionInterval(startIndex, endIndex);
                }
            } else {
                super.addSelectionInterval(startIndex, endIndex);
            }
        }

        private boolean multipleItemsAreCurrentlySelected() {
            return getMinSelectionIndex() != getMaxSelectionIndex();
        }
    }
}
