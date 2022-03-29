package org.hkijena.jipipe.api.compat;

import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;

import java.awt.*;

/**
 * Default implementation of an ImageJ data exporter UI
 */
public class DefaultImageJDataExporterUI extends ImageJDataExporterUI {
    /**
     * @param workbench the workbench
     * @param exporter  the exporter
     */
    public DefaultImageJDataExporterUI(JIPipeWorkbench workbench, ImageJDataExportOperation exporter) {
        super(workbench, exporter);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(new ParameterPanel(getWorkbench(), getExporter(), null, ParameterPanel.NO_GROUP_HEADERS | ParameterPanel.WITHOUT_COLLAPSE));
    }
}
