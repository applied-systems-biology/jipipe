package org.hkijena.acaq5.extensions.filesystem.compat;

import ij.ImagePlus;
import ij.WindowManager;
import org.hkijena.acaq5.api.compat.ImageJDatatypeImporter;
import org.hkijena.acaq5.ui.compat.ImageJDatatypeImporterUI;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.ui.components.PathEditor;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
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
