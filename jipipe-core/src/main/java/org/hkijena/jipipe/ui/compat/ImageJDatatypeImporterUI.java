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

package org.hkijena.jipipe.ui.compat;

import org.hkijena.jipipe.api.compat.ImageJDatatypeImporter;

import javax.swing.*;

/**
 * User interface around an {@link org.hkijena.jipipe.api.compat.ImageJDatatypeAdapter} that
 */
public class ImageJDatatypeImporterUI extends JPanel {
    private ImageJDatatypeImporter importer;

    /**
     * @param importer the importer
     */
    public ImageJDatatypeImporterUI(ImageJDatatypeImporter importer) {
        this.importer = importer;
    }

    public ImageJDatatypeImporter getImporter() {
        return importer;
    }
}
