package org.hkijena.jipipe.api.compat;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;

import java.awt.*;

/**
 * Default implementation of an ImageJ data importer UI
 */
public class DefaultImageJDataImporterUI extends ImageJDataImporterUI {
    /**
     * @param workbench the workbench
     * @param importer the importer
     */
    public DefaultImageJDataImporterUI(JIPipeWorkbench workbench, ImageJDataImportOperation importer) {
        super(workbench, importer);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        ImageJDataImportOperation importer = getImporter();
        add(new ParameterPanel(getWorkbench(), importer, null, ParameterPanel.NO_GROUP_HEADERS | ParameterPanel.WITHOUT_COLLAPSE));
    }
}
