package org.hkijena.jipipe.extensions.parameters.library.quantities;

import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.DocumentChangeListener;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.Objects;

public class QuantityParameterEditorUI extends JIPipeParameterEditorUI {

    private JXTextField valueEditor = new JXTextField();
    private JComboBox<String> unitEditor;
    private boolean isUpdatingTextBoxes = false;

    /**
     * Creates new instance
     *
     * @param workbench       the workbech
     * @param parameterAccess Parameter
     */
    public QuantityParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("TextField.background"));
        setBorder(BorderFactory.createEtchedBorder());

        valueEditor.setPrompt("Value");
        valueEditor.setBorder(null);
        valueEditor.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                String text = StringUtils.orElse(valueEditor.getText(), "");
                text = text.replace(',', '.').replace(" ", ""); // Allow usage of comma as separator
                if (NumberUtils.isCreatable(text)) {
                    valueEditor.setBorder(null);
                } else {
                    valueEditor.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                }
            }
        });
        add(valueEditor, BorderLayout.CENTER);

        unitEditor = new JComboBox<>();
        unitEditor.setPreferredSize(new Dimension(120, 25));
        unitEditor.setEditor(new Editor());
        unitEditor.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));
        unitEditor.setBackground(UIManager.getColor("TextField.background"));
        unitEditor.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.GRAY),
                BorderFactory.createEmptyBorder(0, 16, 0, 4)));
        for (int i = 0; i < unitEditor.getComponentCount(); i++) {
            Component component = unitEditor.getComponent(i);
            if (component instanceof AbstractButton) {
                UIUtils.setStandardButtonBorder((AbstractButton) component);
                ((AbstractButton) component).setBorder(null);
                ((AbstractButton) component).setOpaque(true);
                component.setBackground(UIManager.getColor("TextField.background"));
                break;
            }
        }
        add(unitEditor, BorderLayout.EAST);

        valueEditor.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                if (!isUpdatingTextBoxes) {
                    String s = StringUtils.nullToEmpty(valueEditor.getText());
                    s = s.replace(',', '.').replace(" ", ""); // Allow usage of comma as separator
                    if (NumberUtils.isCreatable(s)) {
                        Quantity parameter = getParameter(Quantity.class);
                        double value = NumberUtils.createDouble(s);
                        if (value != parameter.getValue()) {
                            parameter.setValue(value);
                            setParameter(parameter, false);
                        }
                    }
                }
            }
        });
        unitEditor.setEditable(true);
        JXTextField unitEditorField = (JXTextField) unitEditor.getEditor().getEditorComponent();
        (unitEditorField).getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                if (!isUpdatingTextBoxes) {
                    String s = StringUtils.nullToEmpty(unitEditorField.getText()).trim();
                    Quantity parameter = getParameter(Quantity.class);
                    if (!Objects.equals(parameter.getUnit(), s)) {
                        parameter.setUnit(s);
                        setParameter(parameter, false);
                    }
                }
            }
        });
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        updateTextFields();
        revalidate();
    }

    private void updateTextFields() {
        try {
            isUpdatingTextBoxes = true;

            String[] predefinedUnits = Quantity.KNOWN_UNITS;
            QuantityParameterSettings settings = getParameterAccess().getAnnotationOfType(QuantityParameterSettings.class);
            if (settings != null) {
                if (settings.predefinedUnits().length > 0) {
                    predefinedUnits = settings.predefinedUnits();
                }
            }
            unitEditor.setModel(new DefaultComboBoxModel<>(predefinedUnits));
            Quantity parameter = getParameter(Quantity.class);
            valueEditor.setText(parameter.getValue() + "");
            unitEditor.setSelectedItem(StringUtils.nullToEmpty(parameter.getUnit()));
        } finally {
            isUpdatingTextBoxes = false;
        }
    }

    private static class Editor extends JXTextField implements ComboBoxEditor {

        public Editor() {
            setPrompt("Unit");
            setBorder(null);
        }

        @Override
        public Component getEditorComponent() {
            return this;
        }

        @Override
        public Object getItem() {
            return getText();
        }

        @Override
        public void setItem(Object anObject) {
            setText(StringUtils.nullToEmpty(anObject));
        }
    }
}
