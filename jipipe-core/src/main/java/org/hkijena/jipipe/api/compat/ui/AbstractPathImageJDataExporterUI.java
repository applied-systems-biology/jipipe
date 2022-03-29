package org.hkijena.jipipe.api.compat.ui;

import org.hkijena.jipipe.api.compat.ImageJDataExportOperation;
import org.hkijena.jipipe.api.compat.ImageJDataExporterUI;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.PathEditor;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Paths;

/**
 * Exporter UI that changes the activation state and the name (based on a path)
 */
public abstract class AbstractPathImageJDataExporterUI extends ImageJDataExporterUI {
    /**
     * @param workbench the workbench
     * @param exporter  the importer
     */
    public AbstractPathImageJDataExporterUI(JIPipeWorkbench workbench, ImageJDataExportOperation exporter) {
        super(workbench, exporter);
        initialize();
    }

    public abstract PathType getPathType();

    private void initialize() {
        setLayout(new BorderLayout());
        PathEditor pathEditor = new PathEditor(PathIOMode.Save, getPathType());
        try {
            pathEditor.setPath(Paths.get(getExporter().getName()));
        } catch (Exception e) {
        }
        pathEditor.addActionListener(e -> {
            getExporter().setName(pathEditor.getPath().toString());
        });
        add(pathEditor, BorderLayout.CENTER);

        JCheckBox activateBox = new JCheckBox("Activate", getExporter().isActivate());
        activateBox.addActionListener(e -> {
            getExporter().setActivate(activateBox.isSelected());
        });
        add(activateBox, BorderLayout.SOUTH);
    }
}
