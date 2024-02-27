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

package org.hkijena.jipipe.api.compat;

import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;

/**
 * User interface around a {@link ImageJDataExportOperation}
 */
public class ImageJDataExporterUI extends JIPipeWorkbenchPanel {
    private final ImageJDataExportOperation exporter;

    /**
     * @param exporter the importer
     */
    public ImageJDataExporterUI(JIPipeWorkbench workbench, ImageJDataExportOperation exporter) {
        super(workbench);
        this.exporter = exporter;
    }

    public ImageJDataExportOperation getExporter() {
        return exporter;
    }
}
