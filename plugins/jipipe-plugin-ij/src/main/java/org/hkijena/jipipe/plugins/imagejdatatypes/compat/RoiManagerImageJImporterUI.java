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

package org.hkijena.jipipe.plugins.imagejdatatypes.compat;

import org.hkijena.jipipe.api.compat.ImageJDataImportOperation;
import org.hkijena.jipipe.api.compat.ImageJDataImporterUI;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;

import javax.swing.*;
import java.awt.*;

/**
 * Importer around {@link RoiManagerImageJImporter}
 */
public class RoiManagerImageJImporterUI extends ImageJDataImporterUI {
    /**
     * @param workbench the workbench
     * @param importer  the importer
     */
    public RoiManagerImageJImporterUI(JIPipeDesktopWorkbench workbench, ImageJDataImportOperation importer) {
        super(workbench, importer);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JLabel label = new JLabel("<Active ROI manager>");
        label.setToolTipText("The ROI Manager that is currently active");
        add(label, BorderLayout.CENTER);
    }
}
