package org.hkijena.acaq5.ui.compat;

import org.hkijena.acaq5.api.compat.ImageJDatatypeImporter;

import javax.swing.*;

/**
 * User interface around an {@link org.hkijena.acaq5.api.compat.ImageJDatatypeAdapter} that
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
