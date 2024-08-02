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
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;

import java.awt.*;

/**
 * Default implementation of an ImageJ data exporter UI
 */
public class DefaultImageJDataExporterUI extends ImageJDataExporterUI {
    /**
     * @param workbench the workbench
     * @param exporter  the exporter
     */
    public DefaultImageJDataExporterUI(JIPipeDesktopWorkbench workbench, ImageJDataExportOperation exporter) {
        super(workbench, exporter);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(new JIPipeDesktopParameterFormPanel(getDesktopWorkbench(), getExporter(), null, JIPipeDesktopParameterFormPanel.NO_GROUP_HEADERS | JIPipeDesktopParameterFormPanel.WITHOUT_COLLAPSE));
    }
}
