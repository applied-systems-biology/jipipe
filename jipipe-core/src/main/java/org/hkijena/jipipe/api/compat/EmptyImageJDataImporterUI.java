package org.hkijena.jipipe.api.compat;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import javax.swing.*;
import java.awt.*;

/**
 * Default implementation of an ImageJ data importer UI
 */
public class EmptyImageJDataImporterUI extends ImageJDataImporterUI {
    /**
     * @param workbench the workbench
     * @param importer the importer
     */
    public EmptyImageJDataImporterUI(JIPipeWorkbench workbench, ImageJDataImportOperation importer) {
        super(workbench, importer);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(new JLabel("Nothing will be imported"));
    }
}
