package org.hkijena.jipipe.ui.compat;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compat.ImageJDataExportOperation;
import org.hkijena.jipipe.api.compat.ImageJDataExporter;
import org.hkijena.jipipe.api.compat.ImageJDataExporterUI;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.components.pickers.ImageJDataExporterPicker;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class RunSingleAlgorithmSettingsPanelIOEditorOutputUI extends JPanel {
    private final RunSingleAlgorithmSettingsPanelIOEditor editor;
    private final String slotName;
    private final JButton selectExporterButton = new JButton();
    private final JPanel editorPanel = new JPanel(new BorderLayout());

    public RunSingleAlgorithmSettingsPanelIOEditorOutputUI(RunSingleAlgorithmSettingsPanelIOEditor editor, String slotName) {
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
        titlePanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY),
                BorderFactory.createEmptyBorder(3, 3, 3, 3)));
        titlePanel.add(new JLabel(slotName,
                JIPipe.getDataTypes().getIconFor(getNode().getOutputSlot(slotName).getAcceptedDataType()),
                JLabel.LEFT), BorderLayout.WEST);

        // Initialize importer button
        selectExporterButton.addActionListener(e -> pickExporter());
        JPanel editExporterPanel = new JPanel(new BorderLayout());
        editExporterPanel.add(selectExporterButton, BorderLayout.CENTER);
        JButton pickExporterButton = new JButton(UIUtils.getIconFromResources("actions/edit.png"));
        pickExporterButton.addActionListener(e -> pickExporter());
        editExporterPanel.add(pickExporterButton, BorderLayout.EAST);
        UIUtils.makeFlat25x25(pickExporterButton);
        UIUtils.makeFlatH25(selectExporterButton);
        titlePanel.add(editExporterPanel, BorderLayout.EAST);
        add(titlePanel);

        // Add editor
        editorPanel.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));
        add(editorPanel);
    }

    private void pickExporter() {
        ImageJDataExporterPicker picker = new ImageJDataExporterPicker(editor.getSettingsPanel().getWorkbench().getWindow());
        picker.setAvailableItems(new ArrayList<>(JIPipe.getImageJAdapters().getAvailableExporters(getNode().getOutputSlot(slotName).getAcceptedDataType(), true)));
        picker.setSelectedItem(getCurrentOperation().getExporter());
        ImageJDataExporter result = picker.showDialog();
        if (result != null) {
            getCurrentOperation().setExporterId(JIPipe.getImageJAdapters().getIdOf(result));
        }
        reloadUI();
    }

    private JIPipeGraphNode getNode() {
        return editor.getSettingsPanel().getNode();
    }

    private void reloadUI() {
        ImageJDataExportOperation currentOperation = getCurrentOperation();
        selectExporterButton.setText(currentOperation.getExporter().getName());
        selectExporterButton.setIcon(JIPipe.getDataTypes().getIconFor(currentOperation.getExporter().getExportedJIPipeDataType()));
        selectExporterButton.setToolTipText(currentOperation.getExporter().getDescription());

        editorPanel.removeAll();
        ImageJDataExporterUI ui = JIPipe.getImageJAdapters().createUIForExportOperation(editor.getWorkbench(), getCurrentOperation());
        editorPanel.add(ui, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private ImageJDataExportOperation getCurrentOperation() {
        return editor.getSettingsPanel().getRun().getOutputSlotExporters().get(slotName);
    }
}
