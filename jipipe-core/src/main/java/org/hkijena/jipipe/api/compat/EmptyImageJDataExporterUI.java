package org.hkijena.jipipe.api.compat;

import org.hkijena.jipipe.ui.JIPipeWorkbench;

import javax.swing.*;
import java.awt.*;

/**
 * Default implementation of an ImageJ data exporter UI
 */
public class EmptyImageJDataExporterUI extends ImageJDataExporterUI {
    /**
     * @param workbench the workbench
     * @param exporter  the importer
     */
    public EmptyImageJDataExporterUI(JIPipeWorkbench workbench, ImageJDataExportOperation exporter) {
        super(workbench, exporter);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(new JLabel("Nothing will be exported"));
    }
}
