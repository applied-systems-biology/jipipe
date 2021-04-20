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

package org.hkijena.jipipe.ui.resultanalysis;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataImportOperation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTable;
import org.hkijena.jipipe.extensions.parameters.primitives.DynamicStringEnumParameter;
import org.hkijena.jipipe.extensions.settings.DefaultResultImporterSettings;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.utils.BusyCursor;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;

/**
 * Provides a standard result slot UI that can be also further extended.
 * Override registerActions() for this
 */
public class JIPipeDefaultResultDataSlotRowUI extends JIPipeResultDataSlotRowUI {

    private final List<JIPipeDataImportOperation> importOperations;

    /**
     * Creates a new UI
     *
     * @param workbenchUI The workbench UI
     * @param slot        The data slot
     * @param row         The data slow row
     */
    public JIPipeDefaultResultDataSlotRowUI(JIPipeProjectWorkbench workbenchUI, JIPipeDataSlot slot, JIPipeExportedDataTable.Row row) {
        super(workbenchUI, slot, row);
        String datatypeId = row.getTrueDataType();
        importOperations = JIPipe.getInstance().getDatatypeRegistry().getImportOperationsFor(datatypeId);
        initialize();
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(Box.createHorizontalGlue());

        if (!importOperations.isEmpty()) {
            JIPipeDataImportOperation mainOperation = getMainOperation();
            if (mainOperation == null)
                return;
            JButton mainActionButton = new JButton(mainOperation.getName(), mainOperation.getIcon());
            mainActionButton.setToolTipText(mainOperation.getDescription());
            mainActionButton.addActionListener(e -> runImportOperation(mainOperation));
            add(mainActionButton);

            if (importOperations.size() > 1) {
                JButton menuButton = new JButton("...");
                menuButton.setMaximumSize(new Dimension(1, (int) mainActionButton.getPreferredSize().getHeight()));
                menuButton.setToolTipText("More actions ...");
                JPopupMenu menu = UIUtils.addPopupMenuToComponent(menuButton);
                for (JIPipeDataImportOperation otherSlotAction : importOperations) {
                    if (otherSlotAction == mainOperation)
                        continue;
                    JMenuItem item = new JMenuItem(otherSlotAction.getName(), otherSlotAction.getIcon());
                    item.setToolTipText(otherSlotAction.getDescription());
                    item.addActionListener(e -> runImportOperation(otherSlotAction));
                    menu.add(item);
                }
                add(menuButton);
            }
        }
    }

    private void runImportOperation(JIPipeDataImportOperation operation) {
        try (BusyCursor cursor = new BusyCursor(this)) {
            operation.show(getSlot(), getRow(), getRowStorageFolder(), getAlgorithmCompartmentName(), getAlgorithmName(), getDisplayName(), getWorkbench());
            if (GeneralDataSettings.getInstance().isAutoSaveLastImporter()) {
                String dataTypeId = JIPipe.getDataTypes().getIdOf(getSlot().getAcceptedDataType());
                DynamicStringEnumParameter parameter = DefaultResultImporterSettings.getInstance().getValue(dataTypeId, DynamicStringEnumParameter.class);
                if (parameter != null && !Objects.equals(operation.getName(), parameter.getValue())) {
                    parameter.setValue(operation.getName());
                    DefaultResultImporterSettings.getInstance().setValue(dataTypeId, parameter);
                    JIPipe.getSettings().save();
                }
            }
        }
    }

    private JIPipeDataImportOperation getMainOperation() {
        if (!importOperations.isEmpty()) {
            JIPipeDataImportOperation result = importOperations.get(0);
            String dataTypeId = JIPipe.getDataTypes().getIdOf(getSlot().getAcceptedDataType());
            DynamicStringEnumParameter parameter = DefaultResultImporterSettings.getInstance().getValue(dataTypeId, DynamicStringEnumParameter.class);
            if (parameter != null) {
                String defaultName = parameter.getValue();
                for (JIPipeDataImportOperation operation : importOperations) {
                    if (Objects.equals(operation.getName(), defaultName)) {
                        result = operation;
                        break;
                    }
                }
            }
            return result;
        }
        return null;
    }

    @Override
    public void handleDefaultAction() {
        JIPipeDataImportOperation mainOperation = getMainOperation();
        if (mainOperation != null)
            SwingUtilities.invokeLater(() -> runImportOperation(mainOperation));
    }

}
