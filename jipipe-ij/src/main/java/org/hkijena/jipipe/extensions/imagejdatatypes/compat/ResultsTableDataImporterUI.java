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

package org.hkijena.jipipe.extensions.imagejdatatypes.compat;

import org.hkijena.jipipe.api.compat.ImageJDataImportOperation;
import org.hkijena.jipipe.api.compat.ImageJDataImporterUI;

import javax.swing.*;
import java.awt.*;

/**
 * UI for a {@link org.hkijena.jipipe.extensions.imagejdatatypes.compat.ResultsTableDataImageJAdapter}
 */
public class ResultsTableDataImporterUI extends ImageJDataImporterUI {
    /**
     * @param importer the importer
     */
    public ResultsTableDataImporterUI(ImageJDataImportOperation importer) {
        super(importer);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JLabel label = new JLabel("<Active results table>");
        label.setToolTipText("The results table that is currently active");
        add(label, BorderLayout.CENTER);
    }
}
