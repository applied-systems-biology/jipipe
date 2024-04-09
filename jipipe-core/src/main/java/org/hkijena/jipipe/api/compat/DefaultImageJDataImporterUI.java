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
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterPanel;

import java.awt.*;

/**
 * Default implementation of an ImageJ data importer UI
 */
public class DefaultImageJDataImporterUI extends ImageJDataImporterUI {
    /**
     * @param workbench the workbench
     * @param importer  the importer
     */
    public DefaultImageJDataImporterUI(JIPipeDesktopWorkbench workbench, ImageJDataImportOperation importer) {
        super(workbench, importer);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        ImageJDataImportOperation importer = getImporter();
        add(new JIPipeDesktopParameterPanel(getDesktopWorkbench(), importer, null, JIPipeDesktopParameterPanel.NO_GROUP_HEADERS | JIPipeDesktopParameterPanel.WITHOUT_COLLAPSE));
    }
}
