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

package org.hkijena.pipelinej.extensions.filesystem.compat;

import org.hkijena.pipelinej.api.compat.ImageJDatatypeImporter;
import org.hkijena.pipelinej.ui.compat.ImageJDatatypeImporterUI;
import org.hkijena.pipelinej.ui.components.PathEditor;

import java.awt.*;

/**
 * Imports {@link org.hkijena.pipelinej.extensions.filesystem.dataypes.PathData}
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
