/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.filesystem.compat;

import org.hkijena.jipipe.api.compat.ImageJDataImportOperation;
import org.hkijena.jipipe.api.compat.ImageJDataImporterUI;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopPathEditorComponent;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;

import java.awt.*;

/**
 * Imports {@link org.hkijena.jipipe.plugins.filesystem.dataypes.PathData}
 */
public class PathDataImporterUI extends ImageJDataImporterUI {

    private JIPipeDesktopPathEditorComponent pathEditor;

    /**
     * @param importer the importer
     */
    public PathDataImporterUI(JIPipeDesktopWorkbench workbench, ImageJDataImportOperation importer) {
        super(workbench, importer);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        pathEditor = new JIPipeDesktopPathEditorComponent(getDesktopWorkbench(), PathIOMode.Open, PathType.FilesAndDirectories);
        pathEditor.addActionListener(e -> getImporter().setName("" + pathEditor.getPath()));
        add(pathEditor, BorderLayout.CENTER);
    }
}
