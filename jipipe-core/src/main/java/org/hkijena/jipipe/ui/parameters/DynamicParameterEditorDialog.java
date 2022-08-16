package org.hkijena.jipipe.ui.parameters;

import com.google.common.eventbus.Subscribe;
import com.vladsch.flexmark.util.collection.OrderedSet;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeParameterTypeInfoRef;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchAccess;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.components.pickers.JIPipeParameterTypeInfoPicker;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class DynamicParameterEditorDialog extends JDialog implements JIPipeWorkbenchAccess {

    private final JIPipeWorkbench workbench;
    private final JIPipeDynamicParameterCollection dynamicParameterCollection;
    private final JList<ParameterEntry> parameterEntryJList = new JList<>();
    private final List<ParameterEntry> parameterEntryList = new ArrayList<>();
    private final JPanel contentPanel = new JPanel(new BorderLayout());

    private final MarkdownDocument documentation;

    public DynamicParameterEditorDialog(Component parent, JIPipeWorkbench workbench, JIPipeDynamicParameterCollection dynamicParameterCollection) {
        super(SwingUtilities.getWindowAncestor(parent));
        this.workbench = workbench;
        this.dynamicParameterCollection = dynamicParameterCollection;
        this.documentation = MarkdownDocument.fromPluginResource("documentation/edit-dynamic-parameters.md", Collections.emptyMap());
        initialize();
        reinitializeEntryList();
        onListSelectionChanged();
    }

    private void reinitializeEntryList() {
        for (Map.Entry<String, JIPipeParameterAccess> entry : dynamicParameterCollection.getParameters().entrySet()) {
            ParameterEntry parameterEntry = new ParameterEntry(dynamicParameterCollection.getAllowedTypes());
            parameterEntry.setName(entry.getValue().getName());
            parameterEntry.setKey(entry.getKey());
            parameterEntry.setDescription(new HTMLText(entry.getValue().getDescription()));
            parameterEntry.setType(new JIPipeParameterTypeInfoRef(entry.getValue().getFieldClass()));
            parameterEntry.getEventBus().register(this);
            parameterEntryList.add(parameterEntry);
        }
        updateJList();
    }

    private void initialize() {
        setModal(true);
        setTitle("Edit custom parameters");
        setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        getContentPane().setLayout(new BorderLayout());

        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(AutoResizeSplitPane.LEFT_RIGHT, AutoResizeSplitPane.RATIO_1_TO_3);
        getContentPane().add(splitPane, BorderLayout.CENTER);

        initializeListPanel(splitPane);
        splitPane.setRightComponent(contentPanel);
        initializeButtonPanel();

        pack();
        setSize(new Dimension(1024, 768));
    }

    private void updateJList() {
        ParameterEntry selectedValue = parameterEntryJList.getSelectedValue();
        DefaultListModel<ParameterEntry> model = new DefaultListModel<>();
        for (ParameterEntry parameterEntry : parameterEntryList) {
            model.addElement(parameterEntry);
        }
        parameterEntryJList.setModel(model);
        if(selectedValue != null && parameterEntryList.contains(selectedValue)) {
            parameterEntryJList.setSelectedValue(selectedValue, true);
        }
    }

    private boolean supportsAddingFieldClass(Class<?> klass) {
        if(dynamicParameterCollection.getAllowedTypes() == null || dynamicParameterCollection.getAllowedTypes().isEmpty()) {
            return true;
        }
        else {
            return dynamicParameterCollection.getAllowedTypes().contains(klass);
        }
    }

    private void initializeListPanel(AutoResizeSplitPane splitPane) {
        JPanel listPanel = new JPanel(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(Box.createHorizontalGlue());

        initializeAddButton(toolBar);

        JButton removeButton = new JButton("Remove", UIUtils.getIconFromResources("actions/delete.png"));
        removeButton.addActionListener(e -> removeSelectedItems());
        toolBar.add(removeButton);

        listPanel.add(toolBar, BorderLayout.NORTH);
        parameterEntryJList.setCellRenderer(new ParameterEntryListCellRenderer());
        parameterEntryJList.addListSelectionListener(e -> {
            onListSelectionChanged();
        });
        listPanel.add(new JScrollPane(parameterEntryJList), BorderLayout.CENTER);

        splitPane.setLeftComponent(listPanel);
    }

    private void onListSelectionChanged() {
        ParameterEntry entry = parameterEntryJList.getSelectedValue();
        contentPanel.removeAll();
        if(entry == null) {
            contentPanel.add(new MarkdownReader(false, documentation), BorderLayout.CENTER);
        }
        else {
            ParameterPanel parameterPanel = new ParameterPanel(getWorkbench(), entry, documentation, FormPanel.WITH_SCROLLING | FormPanel.WITH_DOCUMENTATION | FormPanel.DOCUMENTATION_BELOW);
            contentPanel.add(parameterPanel, BorderLayout.CENTER);
        }
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void initializeAddButton(JToolBar toolBar) {
        Set<Class<?>> quickAccessParameterTypes = new LinkedHashSet<>();
        if(!dynamicParameterCollection.supportsAllParameterTypes()) {
            if(dynamicParameterCollection.getAllowedTypes().size() < 15) {
                quickAccessParameterTypes.addAll(dynamicParameterCollection.getAllowedTypes());
            }
        }
        // Add common types
        if(supportsAddingFieldClass(Boolean.class)) {
            quickAccessParameterTypes.add(Boolean.class);
        }
        if(supportsAddingFieldClass(Byte.class)) {
            quickAccessParameterTypes.add(Byte.class);
        }
        if(supportsAddingFieldClass(Short.class)) {
            quickAccessParameterTypes.add(Short.class);
        }
        if(supportsAddingFieldClass(Integer.class)) {
            quickAccessParameterTypes.add(Integer.class);
        }
        if(supportsAddingFieldClass(Long.class)) {
            quickAccessParameterTypes.add(Long.class);
        }
        if(supportsAddingFieldClass(Float.class)) {
            quickAccessParameterTypes.add(Float.class);
        }
        if(supportsAddingFieldClass(Double.class)) {
            quickAccessParameterTypes.add(Double.class);
        }

        if(quickAccessParameterTypes.isEmpty()) {
            JButton addButton = new JButton("Add", UIUtils.getIconFromResources("actions/add.png"));
            addButton.addActionListener(e -> addParameterByPicker());
            toolBar.add(addButton);
        }
        else {
            JButton addButton = new JButton("Add", UIUtils.getIconFromResources("actions/add.png"));
            JPopupMenu popupMenu = UIUtils.addPopupMenuToComponent(addButton);
            popupMenu.add(UIUtils.createMenuItem("Search ...", null, UIUtils.getIconFromResources("actions/search.png"), this::addParameterByPicker));
            popupMenu.addSeparator();
            for (Class<?> parameterType : quickAccessParameterTypes) {
                JIPipeParameterTypeInfo info = JIPipe.getParameterTypes().getInfoByFieldClass(parameterType);
                popupMenu.add(UIUtils.createMenuItem(info.getName(), info.getDescription(), UIUtils.getIconFromResources("actions/add.png"), () -> addParameterByFieldClass(parameterType)));
            }
            toolBar.add(addButton);
        }
    }

    private void removeSelectedItems() {
        List<ParameterEntry> selectedValuesList = parameterEntryJList.getSelectedValuesList();
        if(!selectedValuesList.isEmpty()) {
            parameterEntryList.removeAll(selectedValuesList);
        }
        updateJList();
    }

    private void addParameterByFieldClass(Class<?> fieldClass) {
        Set<String> existing = parameterEntryList.stream().map(ParameterEntry::getKey).collect(Collectors.toSet());
        String key = StringUtils.makeUniqueString("parameter", "-", existing);
        ParameterEntry entry = new ParameterEntry(dynamicParameterCollection.getAllowedTypes());
        entry.setType(new JIPipeParameterTypeInfoRef(fieldClass));
        entry.setKey(key);
        entry.getEventBus().register(this);
        parameterEntryList.add(entry);
        updateJList();
        parameterEntryJList.setSelectedValue(entry, true);
    }

    @Subscribe
    public void onParameterEntryUpdated(JIPipeParameterCollection.ParameterChangedEvent event) {
        if(event.getSource() instanceof ParameterEntry) {
            parameterEntryJList.repaint();
        }
    }

    private void addParameterByPicker() {
        JIPipeParameterTypeInfoPicker picker = new JIPipeParameterTypeInfoPicker(this, dynamicParameterCollection.getAllowedTypes());
        JIPipeParameterTypeInfo info = picker.showDialog();
        if(info != null) {
            addParameterByFieldClass(info.getFieldClass());
        }
    }

    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            this.setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("OK", UIUtils.getIconFromResources("actions/dialog-ok.png"));
        confirmButton.addActionListener(e -> {
            if(checkSettings()) {
                copyEntriesToParameterCollection();
                this.setVisible(false);
            }
        });
        buttonPanel.add(confirmButton);

        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    private boolean checkSettings() {
        Set<String> existing = parameterEntryList.stream().map(ParameterEntry::getKey).collect(Collectors.toSet());
        for (String s : existing) {
            if(StringUtils.isNullOrEmpty(StringUtils.nullToEmpty(s).trim())) {
                JOptionPane.showMessageDialog(this,
                        "The unique parameter keys cannot be empty!",
                        "Empty keys found",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if(existing.size() != parameterEntryList.size()) {
            JOptionPane.showMessageDialog(this,
                    "There are " + (parameterEntryList.size() - existing.size()) + " entries that share the same key. Please ensure that each parameter has its unique key!",
                    "Non-unique keys found",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void copyEntriesToParameterCollection() {
        try {
            Map<String, Object> oldValues = new HashMap<>();
            for (Map.Entry<String, JIPipeParameterAccess> entry : dynamicParameterCollection.getParameters().entrySet()) {
                oldValues.put(entry.getKey(), entry.getValue().get(Object.class));
            }
            dynamicParameterCollection.beginModificationBlock();
            dynamicParameterCollection.clear();
            for (ParameterEntry entry : parameterEntryList) {
                String key = StringUtils.makeUniqueString(StringUtils.orElse(entry.getKey(), "parameter"), "-", dynamicParameterCollection.getParameters().keySet());
                JIPipeMutableParameterAccess access = entry.toParameterAccess();
                access.setKey(key);
                Object oldValue = oldValues.getOrDefault(key, null);
                if (oldValue != null && access.getFieldClass().isAssignableFrom(oldValue.getClass())) {
                    access.set(oldValue);
                }
                dynamicParameterCollection.addParameter(access);
            }
        }
        finally {
            dynamicParameterCollection.endModificationBlock();
        }
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    public static class ParameterEntry extends AbstractJIPipeParameterCollection {

        private final Set<Class<?>> allowedParameterTypes;

        private String key = "";
        private String name = "";
        private JIPipeParameterTypeInfoRef type = new JIPipeParameterTypeInfoRef();
        private HTMLText description = new HTMLText();

        public ParameterEntry(Set<Class<?>> allowedParameterTypes) {
            this.allowedParameterTypes = allowedParameterTypes;
        }

        @JIPipeDocumentation(name = "Unique key", description = "The unique key of the parameter")
        @JIPipeParameter(value = "key", important = true, uiOrder = -100)
        @StringParameterSettings(monospace = true)
        public String getKey() {
            return key;
        }

        @JIPipeParameter("key")
        public void setKey(String key) {
            this.key = key;
        }

        @JIPipeDocumentation(name = "Name", description = "The name of the parameter")
        @JIPipeParameter(value = "name", uiOrder = -90)
        public String getName() {
            return name;
        }

        @JIPipeParameter("name")
        public void setName(String name) {
            this.name = name;
        }

        @JIPipeDocumentation(name = "Type", description = "The type of the parameter")
        @JIPipeParameter(value = "type", important = true, uiOrder = -200)
        public JIPipeParameterTypeInfoRef getType() {
            return type;
        }

        @JIPipeParameter("type")
        public void setType(JIPipeParameterTypeInfoRef type) {
            this.type = type;
            type.setUiAllowedParameterTypes(allowedParameterTypes);
        }

        @JIPipeDocumentation(name = "Description", description = "A custom description")
        @JIPipeParameter(value = "description", uiOrder = -80)
        public HTMLText getDescription() {
            return description;
        }

        @JIPipeParameter("description")
        public void setDescription(HTMLText description) {
            this.description = description;
        }

        public JIPipeMutableParameterAccess toParameterAccess() {
            JIPipeMutableParameterAccess parameterAccess = new JIPipeMutableParameterAccess();
            parameterAccess.setKey(key);
            parameterAccess.setName(StringUtils.orElse(name, key));
            parameterAccess.setDescription(description.getBody());
            parameterAccess.setFieldClass(type.getInfo().getFieldClass());
            return parameterAccess;
        }
    }

    public static class ParameterEntryListCellRenderer extends JPanel implements ListCellRenderer<ParameterEntry> {

        private JLabel typeLabel;
        private JLabel idLabel;
        private JLabel nameLabel;

        /**
         * Creates a new renderer
         */
        public ParameterEntryListCellRenderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            initialize();
        }

        private void initialize() {
            setLayout(new GridBagLayout());
            JLabel parameterIcon = new JLabel(UIUtils.getIconFromResources("data-types/parameters.png"));
            nameLabel = new JLabel();
            typeLabel = new JLabel();
            idLabel = new JLabel();
            typeLabel.setForeground(Color.GRAY);
            idLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            typeLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 11));

            add(parameterIcon, new GridBagConstraints() {
                {
                    gridx = 0;
                    gridy = 0;
                    insets = new Insets(0, 4, 0, 4);
                }
            });
            add(nameLabel, new GridBagConstraints() {
                {
                    gridx = 1;
                    gridy = 0;
                    fill = HORIZONTAL;
                    weightx = 1;
                }
            });
            add(idLabel, new GridBagConstraints() {
                {
                    gridx = 1;
                    gridy = 2;
                    fill = HORIZONTAL;
                    weightx = 1;
                }
            });
            add(typeLabel, new GridBagConstraints() {
                {
                    gridx = 1;
                    gridy = 3;
                    fill = HORIZONTAL;
                    weightx = 1;
                }
            });
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ParameterEntry> list, ParameterEntry value, int index, boolean isSelected, boolean cellHasFocus) {
            setFont(list.getFont());
            nameLabel.setText(StringUtils.orElse(value.getName(), value.getKey()));
            idLabel.setText(StringUtils.orElse(value.getKey(), "<No key set>"));
            typeLabel.setText(value.getType().getInfo() != null ? value.getType().getInfo().getName() : "<Not set>");

            if (isSelected) {
                setBackground(UIManager.getColor("List.selectionBackground"));
            } else {
                setBackground(UIManager.getColor("List.background"));
            }
            return this;
        }
    }
}
