/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.acaq5.extensions.imagejdatatypes.compat;

import org.hkijena.acaq5.api.compat.ImageJDatatypeImporter;
import org.hkijena.acaq5.ui.compat.ImageJDatatypeImporterUI;

import javax.swing.*;
import java.awt.BorderLayout;

/**
 * Importer around {@link org.hkijena.acaq5.extensions.imagejdatatypes.compat.ROIDataImageJAdapter}
 */
public class ROIDataImporterUI extends ImageJDatatypeImporterUI {
    /**
     * @param importer the importer
     */
    public ROIDataImporterUI(ImageJDatatypeImporter importer) {
        super(importer);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JLabel label = new JLabel("<Active ROI manager>");
        label.setToolTipText("The ROI Manager that is currently active");
        add(label, BorderLayout.CENTER);
    }
}
