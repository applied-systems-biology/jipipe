package org.hkijena.jipipe.ui.parameters;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeParameterTypeInfoRef;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchAccess;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class DynamicParameterEditorDialog extends JDialog implements JIPipeWorkbenchAccess {

    private final JIPipeWorkbench workbench;
    private final JIPipeDynamicParameterCollection dynamicParameterCollection;
    private final JList<ParameterEntry> parameterAccessJList = new JList<>();

    public DynamicParameterEditorDialog(Component parent, JIPipeWorkbench workbench, JIPipeDynamicParameterCollection dynamicParameterCollection) {
        super(SwingUtilities.getWindowAncestor(parent));
        this.workbench = workbench;
        this.dynamicParameterCollection = dynamicParameterCollection;
        initialize();
    }

    private void initialize() {
        setModal(true);
        setTitle("Edit custom parameters");
        setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        getContentPane().setLayout(new BorderLayout());

        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(AutoResizeSplitPane.LEFT_RIGHT, AutoResizeSplitPane.RATIO_1_TO_3);
        getContentPane().add(splitPane, BorderLayout.CENTER);

        initializeListPanel(splitPane);
        initializeButtonPanel();

        pack();
        setSize(new Dimension(1024, 768));
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

        List<Class<?>> quickAccessParameterTypes = new ArrayList<>();
        if(dynamicParameterCollection.getAllowedTypes() == null || dynamicParameterCollection.getAllowedTypes().isEmpty() || dynamicParameterCollection.getAllowedTypes().size() == JIPipe.getParameterTypes().getRegisteredParameters().size()) {
            // Supports all field types -> Only offer a search
        }
        else {

        }

        listPanel.add(toolBar, BorderLayout.NORTH);
        listPanel.add(new JScrollPane(parameterAccessJList), BorderLayout.CENTER);

        splitPane.setLeftComponent(listPanel);
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
            writeTempToMain();
            this.setVisible(false);
        });
        buttonPanel.add(confirmButton);

        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    private void writeTempToMain() {
        try {
            Map<String, Object> oldValues = new HashMap<>();
            for (Map.Entry<String, JIPipeParameterAccess> entry : dynamicParameterCollection.getParameters().entrySet()) {
                oldValues.put(entry.getKey(), entry.getValue().get(Object.class));
            }
            dynamicParameterCollection.beginModificationBlock();
            dynamicParameterCollection.clear();
            for (int i = 0; i < parameterAccessJList.getModel().getSize(); i++) {
                ParameterEntry entry = parameterAccessJList.getModel().getElementAt(i);
                String key = StringUtils.makeUniqueString(StringUtils.orElse(entry.getKey(), "parameter"), "-", dynamicParameterCollection.getParameters().keySet());
                JIPipeMutableParameterAccess access = entry.toParameterAccess();
                access.setKey(key);
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
        @JIPipeParameter("name")
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
        @JIPipeParameter("description")
        public HTMLText getDescription() {
            return description;
        }

        @JIPipeParameter("description")
        public void setDescription(HTMLText description) {
            this.description = description;
        }

        public JIPipeMutableParameterAccess toParameterAccess() {
            return null;
        }
    }
}
