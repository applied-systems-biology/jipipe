package org.hkijena.jipipe.ui.compat;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compat.ImageJDataImportOperation;
import org.hkijena.jipipe.api.compat.ImageJDataImporter;
import org.hkijena.jipipe.api.compat.ImageJDataImporterUI;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.components.pickers.ImageJDataImporterPicker;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class RunSingleAlgorithmSettingsPanelIOEditorInputUI extends JPanel {
    private final RunSingleAlgorithmSettingsPanelIOEditor editor;
    private final String slotName;
    private final JButton selectImporterButton = new JButton();
    private final JPanel editorPanel = new JPanel(new BorderLayout());

    public RunSingleAlgorithmSettingsPanelIOEditorInputUI(RunSingleAlgorithmSettingsPanelIOEditor editor, String slotName) {
        this.editor = editor;
        this.slotName = slotName;
        initialize();
        reloadUI();
    }

    private void initialize() {
        setBorder(new RoundedLineBorder(Color.darkGray, 1, 3));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Initialize title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0, Color.DARK_GRAY),
                BorderFactory.createEmptyBorder(3,3,3,3)));
        titlePanel.add(new JLabel(slotName,
                JIPipe.getDataTypes().getIconFor(getNode().getInputSlot(slotName).getAcceptedDataType()),
                JLabel.LEFT), BorderLayout.WEST);

        // Initialize importer button
        selectImporterButton.addActionListener(e -> pickImporter());
        JPanel editImporterPanel = new JPanel(new BorderLayout());
        editImporterPanel.add(selectImporterButton, BorderLayout.CENTER);
        JButton pickImporterButton = new JButton(UIUtils.getIconFromResources("actions/edit.png"));
        pickImporterButton.addActionListener(e -> pickImporter());
        editImporterPanel.add(pickImporterButton, BorderLayout.EAST);
        UIUtils.makeFlat25x25(pickImporterButton);
        UIUtils.makeFlatH25(selectImporterButton);
        titlePanel.add(editImporterPanel, BorderLayout.EAST);
        add(titlePanel);

        // Add editor
        editorPanel.setBorder(BorderFactory.createEmptyBorder(8,4,4,4));
        add(editorPanel);
    }

    private void pickImporter() {
        ImageJDataImporterPicker picker = new ImageJDataImporterPicker(editor.getSettingsPanel().getWorkbench().getWindow());
        picker.setAvailableItems(new ArrayList<>(JIPipe.getImageJAdapters().getAvailableImporters(getNode().getInputSlot(slotName).getAcceptedDataType(), true)));
        picker.setSelectedItem(getCurrentOperation().getImporter());
        ImageJDataImporter result = picker.showDialog();
        if(result != null) {
            getCurrentOperation().setImporterId(JIPipe.getImageJAdapters().getIdOf(result));
        }
        reloadUI();
    }

    private JIPipeGraphNode getNode() {
        return editor.getSettingsPanel().getNode();
    }

    private void reloadUI() {
        ImageJDataImportOperation currentOperation = getCurrentOperation();
        selectImporterButton.setText(currentOperation.getImporter().getName());
        selectImporterButton.setIcon(JIPipe.getDataTypes().getIconFor(currentOperation.getImporter().getImportedJIPipeDataType()));
        selectImporterButton.setToolTipText(currentOperation.getImporter().getDescription());

        editorPanel.removeAll();
        ImageJDataImporterUI ui = JIPipe.getImageJAdapters().createUIForImportOperation(editor.getWorkbench(), getCurrentOperation());
        editorPanel.add(ui, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private ImageJDataImportOperation getCurrentOperation() {
        return editor.getSettingsPanel().getRun().getInputSlotImporters().get(slotName);
    }
}
