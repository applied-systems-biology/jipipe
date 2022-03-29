package org.hkijena.jipipe.api.compat.ui;

import org.hkijena.jipipe.api.compat.ImageJDataImportOperation;
import org.hkijena.jipipe.api.compat.ImageJDataImporterUI;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.PathEditor;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;

import java.awt.*;
import java.nio.file.Paths;

/**
 * Importer UI that represents the name as path
 */
public abstract class AbstractPathImageJDataImporterUI extends ImageJDataImporterUI {
    /**
     * @param workbench the workbench
     * @param importer  the importer
     */
    public AbstractPathImageJDataImporterUI(JIPipeWorkbench workbench, ImageJDataImportOperation importer) {
        super(workbench, importer);
        initialize();
    }

    public abstract PathType getPathType();

    private void initialize() {
        setLayout(new BorderLayout());
        PathEditor pathEditor = new PathEditor(PathIOMode.Open, getPathType());
        try {
            pathEditor.setPath(Paths.get(getImporter().getName()));
        } catch (Exception e) {
        }
        pathEditor.addActionListener(e -> {
            getImporter().setName(pathEditor.getPath().toString());
        });
        add(pathEditor, BorderLayout.CENTER);
    }
}
