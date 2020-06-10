package org.hkijena.acaq5.extensions.filesystem.compat;

import org.hkijena.acaq5.api.compat.ImageJDatatypeImporter;
import org.hkijena.acaq5.ui.compat.ImageJDatatypeImporterUI;
import org.hkijena.acaq5.ui.components.PathEditor;

import java.awt.*;

/**
 * Imports {@link org.hkijena.acaq5.extensions.filesystem.dataypes.PathData}
 */
public class PathDataImporterUI extends ImageJDatatypeImporterUI {

    private PathEditor pathEditor;

    /**
     * @param importer the importer
     */
    public PathDataImporterUI(ImageJDatatypeImporter importer) {
        super(importer);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        pathEditor = new PathEditor(PathEditor.IOMode.Open, PathEditor.PathMode.FilesAndDirectories);
        pathEditor.addActionListener(e -> getImporter().setParameters("" + pathEditor.getPath()));
        add(pathEditor, BorderLayout.CENTER);
    }
}
