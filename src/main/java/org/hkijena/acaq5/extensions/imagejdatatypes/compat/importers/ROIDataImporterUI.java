package org.hkijena.acaq5.extensions.imagejdatatypes.compat.importers;

import org.hkijena.acaq5.api.compat.ImageJDatatypeImporter;
import org.hkijena.acaq5.ui.compat.ImageJDatatypeImporterUI;

import javax.swing.*;
import java.awt.*;

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
