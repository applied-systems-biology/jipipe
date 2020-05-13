package org.hkijena.acaq5.extensions.tables.parameters;

import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataDeclaration;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.extensions.parameters.references.ACAQDataDeclarationRef;
import org.hkijena.acaq5.extensions.tables.ColumnContentType;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.ui.components.ACAQDataDeclarationRefListCellRenderer;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A parameter editor UI that works for all enumerations
 */
public class TableColumnGeneratorParameterEditorUI extends ACAQParameterEditorUI {

    private boolean isProcessing = false;
    private JComboBox<ACAQDataDeclarationRef> comboBox;
    private JToggleButton numericColumnToggle;
    private JToggleButton textColumnToggle;

    /**
     * @param context         SciJava context
     * @param parameterAccess the parameter
     */
    public TableColumnGeneratorParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
        super(context, parameterAccess);
        initialize();
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        isProcessing = true;
        TableColumnGeneratorParameter parameter = getParameterAccess().get(TableColumnGeneratorParameter.class);
        if (parameter == null) {
            parameter = new TableColumnGeneratorParameter();
        }
        comboBox.setSelectedItem(getParameterAccess().get(Object.class));
        if (parameter.getGeneratedType() == ColumnContentType.NumericColumn) {
            numericColumnToggle.setSelected(true);
        } else {
            textColumnToggle.setSelected(true);
        }
        isProcessing = false;
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        comboBox = new JComboBox<>(getAvailableGenerators());
        comboBox.setRenderer(new ACAQDataDeclarationRefListCellRenderer());
        comboBox.addActionListener(e -> writeValueToParameter());
        add(comboBox);

        ButtonGroup buttonGroup = new ButtonGroup();
        numericColumnToggle = addToggle(buttonGroup, UIUtils.getIconFromResources("number.png"), "Numeric column");
        textColumnToggle = addToggle(buttonGroup, UIUtils.getIconFromResources("text.png"), "Text column");
    }

    private void writeValueToParameter() {
        if (isProcessing)
            return;
        isProcessing = true;
        TableColumnGeneratorParameter parameter = getParameterAccess().get(TableColumnGeneratorParameter.class);
        if (parameter == null) {
            parameter = new TableColumnGeneratorParameter();
        }
        parameter.setGeneratorType((ACAQDataDeclarationRef) comboBox.getSelectedItem());
        if (numericColumnToggle.isSelected())
            parameter.setGeneratedType(ColumnContentType.NumericColumn);
        else
            parameter.setGeneratedType(ColumnContentType.StringColumn);
        getParameterAccess().set(parameter);
        isProcessing = false;
    }

    private ACAQDataDeclarationRef[] getAvailableGenerators() {
        List<Object> result = new ArrayList<>();
        for (Class<? extends ACAQData> klass : ACAQDatatypeRegistry.getInstance().getRegisteredDataTypes().values()) {
            if (TableColumn.isGeneratingTableColumn(klass)) {
                ACAQDataDeclarationRef ref = new ACAQDataDeclarationRef(ACAQDataDeclaration.getInstance(klass));
                result.add(ref);
            }
        }

        return result.toArray(new ACAQDataDeclarationRef[0]);
    }

    private JToggleButton addToggle(ButtonGroup group, Icon icon, String description) {
        JToggleButton toggleButton = new JToggleButton(icon);
        UIUtils.makeFlat25x25(toggleButton);
        toggleButton.addActionListener(e -> writeValueToParameter());
        toggleButton.setToolTipText(description);
        group.add(toggleButton);
        add(toggleButton);
        return toggleButton;
    }
}
