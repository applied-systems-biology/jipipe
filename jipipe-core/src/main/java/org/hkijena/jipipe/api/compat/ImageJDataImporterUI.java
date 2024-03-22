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

import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;

/**
 * User interface around a {@link ImageJDataImportOperation}
 */
public class ImageJDataImporterUI extends JIPipeDesktopWorkbenchPanel {
    private final ImageJDataImportOperation importer;

    /**
     * @param desktopWorkbench the workbench
     * @param importer         the importer
     */
    public ImageJDataImporterUI(JIPipeDesktopWorkbench desktopWorkbench, ImageJDataImportOperation importer) {
        super(desktopWorkbench);
        this.importer = importer;
    }

    public ImageJDataImportOperation getImporter() {
        return importer;
    }
}
