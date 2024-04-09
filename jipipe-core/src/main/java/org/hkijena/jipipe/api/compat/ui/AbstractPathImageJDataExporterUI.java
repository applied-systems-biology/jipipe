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

package org.hkijena.jipipe.api.compat.ui;

import org.hkijena.jipipe.api.compat.ImageJDataExportOperation;
import org.hkijena.jipipe.api.compat.ImageJDataExporterUI;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopPathEditorComponent;
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
    public AbstractPathImageJDataExporterUI(JIPipeDesktopWorkbench workbench, ImageJDataExportOperation exporter) {
        super(workbench, exporter);
        initialize();
    }

    public abstract PathType getPathType();

    private void initialize() {
        setLayout(new BorderLayout());
        JIPipeDesktopPathEditorComponent pathEditor = new JIPipeDesktopPathEditorComponent(PathIOMode.Save, getPathType());
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
