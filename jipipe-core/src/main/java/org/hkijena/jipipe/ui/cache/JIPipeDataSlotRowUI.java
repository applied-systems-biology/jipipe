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

package org.hkijena.jipipe.ui.cache;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeCacheSlotDataSource;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataDisplayOperation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.extensions.parameters.primitives.DynamicStringEnumParameter;
import org.hkijena.jipipe.extensions.settings.DefaultCacheDisplaySettings;
import org.hkijena.jipipe.extensions.settings.DefaultResultImporterSettings;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.Objects;

/**
 * UI for a row
 */
public class JIPipeDataSlotRowUI extends JIPipeWorkbenchPanel {
    private final JIPipeDataSlot slot;
    private final int row;
    private List<JIPipeDataDisplayOperation> displayOperations;

    /**
     * Creates a new instance
     *
     * @param workbench the workbench
     * @param slot      the slot
     * @param row       the row
     */
    public JIPipeDataSlotRowUI(JIPipeWorkbench workbench, JIPipeDataSlot slot, int row) {
        super(workbench);
        this.slot = slot;
        this.row = row;
        String datatypeId = JIPipe.getInstance().getDatatypeRegistry().getIdOf(slot.getAcceptedDataType());
        displayOperations = JIPipe.getInstance().getDatatypeRegistry().getDisplayOperationsFor(datatypeId);
        this.initialize();
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(Box.createHorizontalGlue());

        JButton copyButton = new JButton("Copy string", UIUtils.getIconFromResources("actions/edit-copy.png"));
        copyButton.setToolTipText("Copies the string representation");
        copyButton.addActionListener(e -> copyString());
        add(copyButton);

        if (!displayOperations.isEmpty()) {
            JIPipeDataDisplayOperation mainOperation = getMainOperation();
            if (mainOperation == null)
                return;
            JButton mainActionButton = new JButton(mainOperation.getName(), mainOperation.getIcon());
            mainActionButton.setToolTipText(mainOperation.getDescription());
            mainActionButton.addActionListener(e -> runDisplayOperation(mainOperation));
            add(mainActionButton);

            if (displayOperations.size() > 1) {
                JButton menuButton = new JButton("...");
                menuButton.setMaximumSize(new Dimension(1, (int) mainActionButton.getPreferredSize().getHeight()));
                menuButton.setToolTipText("More actions ...");
                JPopupMenu menu = UIUtils.addPopupMenuToComponent(menuButton);
                for (JIPipeDataDisplayOperation otherSlotAction : displayOperations) {
                    if (otherSlotAction == mainOperation)
                        continue;
                    JMenuItem item = new JMenuItem(otherSlotAction.getName(), otherSlotAction.getIcon());
                    item.setToolTipText(otherSlotAction.getDescription());
                    item.addActionListener(e -> runDisplayOperation(otherSlotAction));
                    menu.add(item);
                }
                add(menuButton);
            }
        }
    }

    private void runDisplayOperation(JIPipeDataDisplayOperation operation) {
        JIPipeData data = slot.getData(row, JIPipeData.class);
        String displayName = slot.getNode().getName() + "/" + slot.getName() + "/" + row;
        operation.display(data, displayName, getWorkbench(), new JIPipeCacheSlotDataSource(slot, row));
        if (GeneralDataSettings.getInstance().isAutoSaveLastDisplay()) {
            String dataTypeId = JIPipe.getDataTypes().getIdOf(slot.getAcceptedDataType());
            DynamicStringEnumParameter parameter = DefaultCacheDisplaySettings.getInstance().getValue(dataTypeId, DynamicStringEnumParameter.class);
            if (parameter != null && !Objects.equals(operation.getName(), parameter.getValue())) {
                parameter.setValue(operation.getName());
                DefaultResultImporterSettings.getInstance().setValue(dataTypeId, parameter);
                JIPipe.getSettings().save();
            }
        }
    }

    private JIPipeDataDisplayOperation getMainOperation() {
        if (!displayOperations.isEmpty()) {
            JIPipeDataDisplayOperation result = displayOperations.get(0);
            String dataTypeId = JIPipe.getDataTypes().getIdOf(slot.getAcceptedDataType());
            DynamicStringEnumParameter parameter = DefaultCacheDisplaySettings.getInstance().getValue(dataTypeId, DynamicStringEnumParameter.class);
            if (parameter != null) {
                String defaultName = parameter.getValue();
                for (JIPipeDataDisplayOperation operation : displayOperations) {
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

    public void handleDefaultAction() {
        JIPipeDataDisplayOperation mainOperation = getMainOperation();
        if(mainOperation != null) {
            runDisplayOperation(mainOperation);
        }
    }

    private void copyString() {
        String string = "" + slot.getData(row, JIPipeData.class);
        StringSelection selection = new StringSelection(string);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }
}
